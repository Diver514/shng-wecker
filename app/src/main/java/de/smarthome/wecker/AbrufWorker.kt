package de.smarthome.wecker

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class AbrufWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    override fun doWork(): Result {
        val ctx = applicationContext
        val p = Prefs(ctx)
        val grund = inputData.getString("grund") ?: "unbekannt"

        try {
            val json = MqttAbruf.holen(p)
            WeckerPlaner.verarbeite(ctx, json)
            p.letzterAbruf = System.currentTimeMillis()
            p.status = "OK ($grund)"
            Benachrichtigung.warnungEntfernen(ctx)
        } catch (e: Exception) {
            p.status = "Nicht erreichbar ($grund): ${e.message ?: e.javaClass.simpleName}"
            // Abends warnen, wenn seit über 24 h keine Daten kamen (z. B. nicht zu Hause)
            val alt = System.currentTimeMillis() - p.letzterAbruf > 24 * 3_600_000L
            if (grund == "abend" && alt) {
                Benachrichtigung.warnung(
                    ctx,
                    "Seit über 24 Stunden keine Weckzeit vom SmartHome erhalten. Bitte Wecker für morgen prüfen."
                )
            }
        }
        // Kein Retry: der nächste Abruf (WLAN/stündlich/22 Uhr) kommt ohnehin
        return Result.success()
    }
}
