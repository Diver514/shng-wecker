package de.smarthome.wecker

import android.app.Application

class WeckerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Benachrichtigung.kanaeleAnlegen(this)
        WeckerPlaner.wiederherstellen(this)
        WeckerPlaner.abendPlanen(this)
        WeckerPlaner.stuendlichPlanen(this)
        WeckerPlaner.wlanBeobachten(this)
    }
}
