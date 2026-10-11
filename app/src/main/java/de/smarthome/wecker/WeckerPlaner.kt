package de.smarthome.wecker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object WeckerPlaner {
    private const val TAG = "SHNGWecker"
    private const val RC_WECKER = 100
    private const val RC_SCHLUMMER = 101
    private const val RC_ABEND = 102
    private const val RC_NETZ = 103
    private const val RC_ANZEIGE = 104

    const val SCHLUMMER_MIN = 10L

    // ------------------------------------------------------------------
    // MQTT-Nachricht auswerten
    // ------------------------------------------------------------------

    fun verarbeite(ctx: Context, json: String) {
        val o = JSONObject(json)
        val p = Prefs(ctx)

        val aktiv = o.optInt("aktiv", 0) == 1
        val datum = LocalDate.parse(o.getString("datum"))
        val zeit = LocalTime.of(o.getInt("stunde"), o.getInt("minute"))
        val millis = LocalDateTime.of(datum, zeit)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        if (aktiv && millis > System.currentTimeMillis()) {
            stellen(ctx, millis, RC_WECKER)
            p.weckMillis = millis
            p.weckInfo = "Wecker: " + format(millis)
        } else {
            abbrechen(ctx, RC_WECKER)
            p.weckMillis = 0L
            p.weckInfo = if (aktiv) "kein anstehender Wecker" else "Wecker aus (frei am $datum)"
        }
    }

    // ------------------------------------------------------------------
    // Wecker stellen / löschen
    // ------------------------------------------------------------------

    fun stellen(ctx: Context, millis: Long, requestCode: Int = RC_WECKER) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        val anzeige = PendingIntent.getActivity(
            ctx, RC_ANZEIGE, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            am.setAlarmClock(AlarmManager.AlarmClockInfo(millis, anzeige), klingelIntent(ctx, requestCode))
        } catch (e: SecurityException) {
            Prefs(ctx).status = "Fehler: keine Berechtigung für exakte Wecker"
            Log.e(TAG, "setAlarmClock verweigert", e)
        }
    }

    fun abbrechen(ctx: Context, requestCode: Int = RC_WECKER) {
        ctx.getSystemService(AlarmManager::class.java).cancel(klingelIntent(ctx, requestCode))
    }

    fun schlummern(ctx: Context) {
        stellen(ctx, System.currentTimeMillis() + SCHLUMMER_MIN * 60_000L, RC_SCHLUMMER)
    }

    fun testalarm(ctx: Context) {
        stellen(ctx, System.currentTimeMillis() + 60_000L, RC_SCHLUMMER)
    }

    /** Nach Neustart/Update: gespeicherten Wecker erneut stellen. */
    fun wiederherstellen(ctx: Context) {
        val p = Prefs(ctx)
        if (p.weckMillis > System.currentTimeMillis()) {
            stellen(ctx, p.weckMillis, RC_WECKER)
        }
    }

    private fun klingelIntent(ctx: Context, requestCode: Int): PendingIntent =
        PendingIntent.getBroadcast(
            ctx, requestCode, Intent(ctx, AlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    // ------------------------------------------------------------------
    // Abruf-Zeitpunkte
    // ------------------------------------------------------------------

    /** Täglicher Abruf um 22:00 Uhr (ohne Dauerverbindung). */
    fun abendPlanen(ctx: Context) {
        var t = LocalDate.now().atTime(22, 0).atZone(ZoneId.systemDefault())
        if (!t.isAfter(ZonedDateTime.now())) t = t.plusDays(1)

        val pi = PendingIntent.getBroadcast(
            ctx, RC_ABEND, Intent(ctx, AbendReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        ctx.getSystemService(AlarmManager::class.java)
            .setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t.toInstant().toEpochMilli(), pi)
    }

    /** Stündlicher Abruf, aber nur wenn ein WLAN (unbegrenztes Netz) verbunden ist. */
    fun stuendlichPlanen(ctx: Context) {
        val req = PeriodicWorkRequestBuilder<AbrufWorker>(1, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.UNMETERED).build()
            )
            .setInputData(workDataOf("grund" to "stündlich"))
            .build()
        WorkManager.getInstance(ctx)
            .enqueueUniquePeriodicWork("stuendlich", ExistingPeriodicWorkPolicy.KEEP, req)
    }

    /** System meldet sich, sobald ein WLAN verbunden wird – auch wenn die App nicht läuft. */
    fun wlanBeobachten(ctx: Context) {
        val cm = ctx.getSystemService(ConnectivityManager::class.java)
        val req = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        val flags = if (Build.VERSION.SDK_INT >= 31) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pi = PendingIntent.getBroadcast(ctx, RC_NETZ, Intent(ctx, NetzReceiver::class.java), flags)
        try {
            cm.registerNetworkCallback(req, pi)
        } catch (e: Exception) {
            Log.e(TAG, "WLAN-Beobachtung nicht möglich", e)
        }
    }

    fun jetztAbrufen(ctx: Context, grund: String, verzoegerungSek: Long = 0) {
        val req = OneTimeWorkRequestBuilder<AbrufWorker>()
            .setInputData(workDataOf("grund" to grund))
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            )
            .setInitialDelay(verzoegerungSek, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(ctx)
            .enqueueUniqueWork("abruf_$grund", ExistingWorkPolicy.REPLACE, req)
    }

    fun format(millis: Long): String =
        SimpleDateFormat("EEEE, dd.MM. 'um' HH:mm 'Uhr'", Locale.GERMANY).format(Date(millis))
}
