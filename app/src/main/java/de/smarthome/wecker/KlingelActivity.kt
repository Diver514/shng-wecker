package de.smarthome.wecker

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Vollbild-Weckanzeige, auch über dem Sperrbildschirm. */
class KlingelActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_klingel)

        findViewById<TextView>(R.id.tvZeit).text =
            SimpleDateFormat("HH:mm", Locale.GERMANY).format(Date())

        findViewById<Button>(R.id.btnStopp).setOnClickListener {
            Benachrichtigung.klingelnBeenden(this)
            finish()
        }
        findViewById<Button>(R.id.btnSchlummern).setOnClickListener {
            WeckerPlaner.schlummern(this)
            Benachrichtigung.klingelnBeenden(this)
            finish()
        }
    }
}
