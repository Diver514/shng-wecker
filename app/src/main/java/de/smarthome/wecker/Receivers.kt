package de.smarthome.wecker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Weckzeit erreicht -> klingeln. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        Benachrichtigung.klingeln(ctx)
    }
}

/** Knöpfe "Stopp" / "Schlummern" in der Benachrichtigung. */
class AktionReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        Benachrichtigung.klingelnBeenden(ctx)
        if (intent.action == Benachrichtigung.ACTION_SCHLUMMERN) {
            WeckerPlaner.schlummern(ctx)
        }
    }
}

/** 22:00 Uhr -> abrufen und nächsten Abend planen. */
class AbendReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        WeckerPlaner.abendPlanen(ctx)
        WeckerPlaner.jetztAbrufen(ctx, "abend")
    }
}

/** WLAN verbunden -> kurz abrufen (höchstens alle 15 Minuten). */
class NetzReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val p = Prefs(ctx)
        val jetzt = System.currentTimeMillis()
        if (jetzt - p.letzterWlanTrigger < 15 * 60_000L) return
        p.letzterWlanTrigger = jetzt
        WeckerPlaner.jetztAbrufen(ctx, "wlan", 15)
    }
}

/** Nach Neustart/Update startet der App-Prozess; WeckerApp plant dann alles neu. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        // Arbeit erledigt WeckerApp.onCreate()
    }
}
