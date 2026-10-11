package de.smarthome.wecker

import android.content.Context

/** Einstellungen und Zustand der App (SharedPreferences). */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("wecker", Context.MODE_PRIVATE)

    var host: String
        get() = sp.getString("host", "") ?: ""
        set(v) = sp.edit().putString("host", v.trim()).apply()

    var port: Int
        get() = sp.getInt("port", 1883)
        set(v) = sp.edit().putInt("port", v).apply()

    var topic: String
        get() = sp.getString("topic", "smarthome/wecker/handy") ?: "smarthome/wecker/handy"
        set(v) = sp.edit().putString("topic", v.trim()).apply()

    var user: String
        get() = sp.getString("user", "") ?: ""
        set(v) = sp.edit().putString("user", v).apply()

    var pass: String
        get() = sp.getString("pass", "") ?: ""
        set(v) = sp.edit().putString("pass", v).apply()

    /** Gestellte Weckzeit (Epoch-Millis), 0 = kein Wecker. */
    var weckMillis: Long
        get() = sp.getLong("weckMillis", 0L)
        set(v) = sp.edit().putLong("weckMillis", v).apply()

    var weckInfo: String
        get() = sp.getString("weckInfo", "noch keine Daten") ?: ""
        set(v) = sp.edit().putString("weckInfo", v).apply()

    var letzterAbruf: Long
        get() = sp.getLong("letzterAbruf", 0L)
        set(v) = sp.edit().putLong("letzterAbruf", v).apply()

    var status: String
        get() = sp.getString("status", "") ?: ""
        set(v) = sp.edit().putString("status", v).apply()

    var letzterWlanTrigger: Long
        get() = sp.getLong("letzterWlanTrigger", 0L)
        set(v) = sp.edit().putLong("letzterWlanTrigger", v).apply()

    /** Gewählter Weckton (Content-URI), leer = Standard-Weckton des Systems. */
    var tonUri: String
        get() = sp.getString("tonUri", "") ?: ""
        set(v) = sp.edit().putString("tonUri", v).apply()

    var vibration: Boolean
        get() = sp.getBoolean("vibration", false)
        set(v) = sp.edit().putBoolean("vibration", v).apply()

    /** Kanal-Version: Android erlaubt keine Änderung von Ton/Vibration an bestehenden Kanälen. */
    var kanalVersion: Int
        get() = sp.getInt("kanalVersion", 2)
        set(v) = sp.edit().putInt("kanalVersion", v).apply()
}
