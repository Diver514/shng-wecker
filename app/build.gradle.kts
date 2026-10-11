plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "de.smarthome.wecker"
    compileSdk = 34

    defaultConfig {
        applicationId = "de.smarthome.wecker"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    // Fester Schlüssel, damit Updates ohne Deinstallation möglich sind
    signingConfigs {
        create("fest") {
            storeFile = file("wecker.keystore")
            storePassword = "shngwecker"
            keyAlias = "wecker"
            keyPassword = "shngwecker"
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("fest")
        }
        getByName("debug") {
            signingConfig = signingConfigs.getByName("fest")
        }
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")
}
