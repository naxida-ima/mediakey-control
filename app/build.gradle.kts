plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.nxd.mediakeycontrol"
    compileSdk = 33

    defaultConfig {
        applicationId = "com.nxd.mediakeycontrol"
        minSdk = 26
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        create("release") {
            storeFile = rootProject.file("keystore/mediakey.keystore")
            storePassword = (project.findProperty("RELEASE_STORE_PASSWORD") as String?) ?: ""
            keyAlias = (project.findProperty("RELEASE_KEY_ALIAS") as String?) ?: ""
            keyPassword = (project.findProperty("RELEASE_KEY_PASSWORD") as String?) ?: ""
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
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
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.22")
}
