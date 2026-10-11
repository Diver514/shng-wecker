package de.smarthome.wecker

import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Kurzer MQTT-Abruf ohne Dauerverbindung:
 * verbinden -> retained Nachricht lesen -> trennen.
 */
object MqttAbruf {

    fun holen(p: Prefs, timeoutSek: Long = 8): String {
        require(p.host.isNotBlank()) { "Kein MQTT-Server eingetragen" }

        val clientId = "shng-wecker-" + UUID.randomUUID().toString().take(8)
        val client = MqttClient("tcp://${p.host}:${p.port}", clientId, MemoryPersistence())
        val optionen = MqttConnectOptions().apply {
            isCleanSession = true
            connectionTimeout = 5
            keepAliveInterval = 30
            isAutomaticReconnect = false
            if (p.user.isNotBlank()) {
                userName = p.user
                password = p.pass.toCharArray()
            }
        }

        val latch = CountDownLatch(1)
        var nachricht: String? = null

        try {
            client.connect(optionen)
            client.subscribe(p.topic, 1) { _, msg ->
                nachricht = String(msg.payload, Charsets.UTF_8)
                latch.countDown()
            }
            if (!latch.await(timeoutSek, TimeUnit.SECONDS)) {
                throw IllegalStateException("Keine Nachricht auf ${p.topic}")
            }
            return nachricht ?: throw IllegalStateException("Leere Nachricht")
        } finally {
            try {
                if (client.isConnected) client.disconnect(1000)
            } catch (ignoriert: Exception) {
            }
            try {
                client.close()
            } catch (ignoriert: Exception) {
            }
        }
    }
}
