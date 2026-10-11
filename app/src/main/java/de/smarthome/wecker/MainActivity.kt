package de.smarthome.wecker

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    companion object {
        private const val RC_TON = 7
    }

    private lateinit var tvStatus: TextView
    private lateinit var tvRechte: TextView
    private lateinit var etHost: EditText
    private lateinit var etPort: EditText
    private lateinit var etTopic: EditText
    private lateinit var etUser: EditText
    private lateinit var etPass: EditText
    private lateinit var tvTon: TextView
    private lateinit var swVibration: Switch
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        tvRechte = findViewById(R.id.tvRechte)
        etHost = findViewById(R.id.etHost)
        etPort = findViewById(R.id.etPort)
        etTopic = findViewById(R.id.etTopic)
        etUser = findViewById(R.id.etUser)
        etPass = findViewById(R.id.etPass)
        tvTon = findViewById(R.id.tvTon)
        swVibration = findViewById(R.id.swVibration)

        val p = Prefs(this)
        etHost.setText(p.host)
        etPort.setText(p.port.toString())
        etTopic.setText(p.topic)
        etUser.setText(p.user)
        etPass.setText(p.pass)

        findViewById<Button>(R.id.btnSpeichern).setOnClickListener {
            p.host = etHost.text.toString()
            p.port = etPort.text.toString().toIntOrNull() ?: 1883
            p.topic = etTopic.text.toString().ifBlank { "smarthome/wecker/handy" }
            p.user = etUser.text.toString()
            p.pass = etPass.text.toString()
            p.status = "Abruf läuft …"
            WeckerPlaner.jetztAbrufen(this, "manuell")
            Toast.makeText(this, "Gespeichert, rufe ab …", Toast.LENGTH_SHORT).show()
            aktualisieren()
            handler.postDelayed({ aktualisieren() }, 5000)
            handler.postDelayed({ aktualisieren() }, 12000)
        }

        findViewById<Button>(R.id.btnTest).setOnClickListener {
            WeckerPlaner.testalarm(this)
            Toast.makeText(this, "Testalarm klingelt in 1 Minute", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnRechte).setOnClickListener { rechteEinrichten() }

        findViewById<Button>(R.id.btnTon).setOnClickListener {
            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Weckton wählen")
                .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Benachrichtigung.ton(this))
            startActivityForResult(intent, RC_TON)
        }

        swVibration.isChecked = p.vibration
        swVibration.setOnCheckedChangeListener { _, an ->
            if (Prefs(this).vibration != an) {
                Prefs(this).vibration = an
                Benachrichtigung.einstellungenGeaendert(this)
            }
        }

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != RC_TON || resultCode != RESULT_OK || data == null) return
        @Suppress("DEPRECATION")
        val uri: Uri? = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        val neu = uri?.toString() ?: ""
        val p = Prefs(this)
        if (neu != p.tonUri) {
            p.tonUri = neu
            Benachrichtigung.einstellungenGeaendert(this)
        }
        aktualisieren()
    }

    override fun onResume() {
        super.onResume()
        aktualisieren()
    }

    private fun aktualisieren() {
        val p = Prefs(this)
        val fmt = SimpleDateFormat("dd.MM. HH:mm", Locale.GERMANY)
        tvStatus.text = buildString {
            append(p.weckInfo).append("\n\n")
            append("Letzter erfolgreicher Abruf: ")
            append(if (p.letzterAbruf > 0) fmt.format(Date(p.letzterAbruf)) else "noch nie").append("\n")
            if (p.status.isNotBlank()) append("Status: ").append(p.status)
        }

        tvTon.text = "Weckton: " + Benachrichtigung.tonName(this)

        val fehlend = fehlendeRechte()
        tvRechte.text = if (fehlend.isEmpty()) {
            "✅ Alle Berechtigungen vorhanden"
        } else {
            "⚠️ Es fehlt: " + fehlend.joinToString(", ") + "\nOhne diese kann der Wecker nicht zuverlässig klingeln."
        }
    }

    private fun fehlendeRechte(): List<String> {
        val liste = mutableListOf<String>()
        val nm = getSystemService(NotificationManager::class.java)
        if (!nm.areNotificationsEnabled()) liste.add("Benachrichtigungen")
        if (Build.VERSION.SDK_INT >= 31 && !getSystemService(AlarmManager::class.java).canScheduleExactAlarms()) {
            liste.add("Wecker & Erinnerungen")
        }
        if (Build.VERSION.SDK_INT >= 34 && !nm.canUseFullScreenIntent()) {
            liste.add("Vollbild-Benachrichtigungen")
        }
        return liste
    }

    private fun rechteEinrichten() {
        val nm = getSystemService(NotificationManager::class.java)
        val paket = Uri.parse("package:$packageName")
        val intent = when {
            !nm.areNotificationsEnabled() ->
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            Build.VERSION.SDK_INT >= 31 && !getSystemService(AlarmManager::class.java).canScheduleExactAlarms() ->
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, paket)
            Build.VERSION.SDK_INT >= 34 && !nm.canUseFullScreenIntent() ->
                Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, paket)
            else -> {
                Toast.makeText(this, "Alles eingerichtet", Toast.LENGTH_SHORT).show()
                return
            }
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, paket))
        }
    }
}
