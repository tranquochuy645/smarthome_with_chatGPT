plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.alexucana"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.alexucana"
        minSdk = 25
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }

    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {

    implementation("com.github.EspressifApp:lib-esptouch-android:1.1.1") // ESP32 wifi hot config
    implementation("com.github.EspressifApp:lib-esptouch-v2-android:2.2.1")


    implementation(platform("com.google.firebase:firebase-bom:32.6.0")) // Firebase dynamic import
    implementation("com.google.firebase:firebase-database") // Realtime db sdk

    implementation("com.squareup.okhttp3:okhttp:4.11.0") // Http

    implementation("com.squareup.okhttp3:okhttp-sse:4.11.0") // SSE


    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}