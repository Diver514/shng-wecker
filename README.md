# SHNG Wecker

Android-Wecker, der seine Weckzeit von SmartHomeNG per MQTT holt
(Topic `smarthome/wecker/handy`, retained, von der Logik `wecker_urlaub.py`).

Keine Dauerverbindung: Die App verbindet sich nur kurz
- jeden Abend um 22:00 Uhr,
- sobald ein WLAN verbunden wird (höchstens alle 15 Minuten),
- stündlich, solange WLAN besteht,
- manuell über "Speichern und jetzt abrufen".

## APK bauen (ohne Android Studio)
1. Privates GitHub-Repository anlegen und den Inhalt dieses Ordners hochladen
   (inkl. `.github/workflows/build.yml` und `app/wecker.keystore`).
2. Unter "Actions" läuft der Build automatisch (oder "Run workflow").
3. Nach ca. 5 Minuten im Lauf unter "Artifacts" `SHNG-Wecker-APK` herunterladen,
   entpacken und `app-release.apk` auf dem Handy installieren.

Der Schlüssel in `app/wecker.keystore` sorgt dafür, dass neue Versionen
ohne Deinstallation installiert werden können. Repository privat halten.
