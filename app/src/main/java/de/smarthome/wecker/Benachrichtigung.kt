package de.smarthome.wecker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Benachrichtigung {
    private const val KANAL_PREFIX = "wecker_"
    const val KANAL_INFO = "hinweise_v1"
    private const val ID_KLINGELN = 1
    private const val ID_WARNUNG = 2

    const val ACTION_STOPP = "de.smarthome.wecker.STOPP"
    const val ACTION_SCHLUMMERN = "de.smarthome.wecker.SCHLUMMERN"

    fun kanalWecker(ctx: Context): String = KANAL_PREFIX + Prefs(ctx).kanalVersion

    fun kanaeleAnlegen(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        val p = Prefs(ctx)
        val aktuell = kanalWecker(ctx)

        // Alte Wecker-Kanäle (andere Ton-/Vibrationseinstellung) entfernen
        for (k in nm.notificationChannels) {
            if (k.id.startsWith(KANAL_PREFIX) && k.id != aktuell) nm.deleteNotificationChannel(k.id)
        }

        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val wecker = NotificationChannel(aktuell, "Wecker", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Klingeln des Weckers"
            setSound(ton(ctx), attrs)
            if (p.vibration) {
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 800, 600, 800, 600)
            } else {
                enableVibration(false)
                vibrationPattern = longArrayOf(0)
            }
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setBypassDnd(true)
        }
        val info = NotificationChannel(KANAL_INFO, "Hinweise", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Warnungen, z. B. wenn keine Weckzeit abgerufen werden konnte"
        }
        nm.createNotificationChannels(listOf(wecker, info))
    }

    /** Ton oder Vibration geändert: neuen Kanal mit neuer ID anlegen. */
    fun einstellungenGeaendert(ctx: Context) {
        val p = Prefs(ctx)
        p.kanalVersion = p.kanalVersion + 1
        kanaeleAnlegen(ctx)
    }

    fun ton(ctx: Context): Uri {
        val gewaehlt = Prefs(ctx).tonUri
        if (gewaehlt.isNotBlank()) return Uri.parse(gewaehlt)
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
    }

    fun tonName(ctx: Context): String =
        try {
            RingtoneManager.getRingtone(ctx, ton(ctx))?.getTitle(ctx) ?: "Standard-Weckton"
        } catch (e: Exception) {
            "Standard-Weckton"
        }

    /** Klingeln: Vollbild + Alarmton in Dauerschleife (FLAG_INSISTENT), max. 10 Minuten. */
    fun klingeln(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        val vollbild = PendingIntent.getActivity(
            ctx, 10,
            Intent(ctx, KlingelActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val icon = Icon.createWithResource(ctx, android.R.drawable.ic_lock_idle_alarm)
        val uhrzeit = SimpleDateFormat("HH:mm", Locale.GERMANY).format(Date())

        val n = Notification.Builder(ctx, kanalWecker(ctx))
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Wecker")
            .setContentText("Es ist $uhrzeit Uhr")
            .setCategory(Notification.CATEGORY_ALARM)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setFullScreenIntent(vollbild, true)
            .setContentIntent(vollbild)
            .setOngoing(true)
            .setAutoCancel(false)
            .setTimeoutAfter(10 * 60_000L)
            .addAction(Notification.Action.Builder(icon, "Schlummern", aktion(ctx, ACTION_SCHLUMMERN)).build())
            .addAction(Notification.Action.Builder(icon, "Stopp", aktion(ctx, ACTION_STOPP)).build())
            .setDeleteIntent(aktion(ctx, ACTION_STOPP))
            .build()
        n.flags = n.flags or Notification.FLAG_INSISTENT
        nm.notify(ID_KLINGELN, n)
    }

    fun klingelnBeenden(ctx: Context) {
        ctx.getSystemService(NotificationManager::class.java).cancel(ID_KLINGELN)
    }

    fun warnung(ctx: Context, text: String) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        val oeffnen = PendingIntent.getActivity(
            ctx, 11, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = Notification.Builder(ctx, KANAL_INFO)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("SHNG Wecker")
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(text))
            .setContentIntent(oeffnen)
            .setAutoCancel(true)
            .build()
        nm.notify(ID_WARNUNG, n)
    }

    fun warnungEntfernen(ctx: Context) {
        ctx.getSystemService(NotificationManager::class.java).cancel(ID_WARNUNG)
    }

    private fun aktion(ctx: Context, action: String): PendingIntent =
        PendingIntent.getBroadcast(
            ctx, action.hashCode(),
            Intent(ctx, AktionReceiver::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}
