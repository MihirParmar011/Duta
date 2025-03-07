plugins {
    alias(libs.plugins.android.application)

    // Add the Google services Gradle plugin
    id("com.google.gms.google-services")
}

android {
    namespace = "com.pm.appdev.duta"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.pm.appdev.duta"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    viewBinding {
        var enabled = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    implementation(libs.car.ui.lib)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    implementation(libs.appcompat.v110)
    implementation(libs.constraintlayout.v113)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)
    implementation(libs.firebase.storage)
    implementation(libs.legacy.support.v4)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.storage.v2030)

    testImplementation(libs.junit.v412)
    androidTestImplementation(libs.junit.v111)
    androidTestImplementation(libs.espresso.core.v320)

    implementation(libs.material.v110)

    // Volley
    implementation(libs.volley)

    // Glide
    implementation(libs.glide)

    // Circular ImageView
    implementation(libs.circularimageview)

    // ML Kit Smart Reply
    implementation(libs.smart.reply)
    implementation(libs.activity.v180)
    implementation(libs.fragment)

    implementation(platform(libs.firebase.bom.v3270))
    implementation(libs.google.firebase.storage)
    implementation(libs.google.firebase.analytics)
    implementation ("com.android.car.ui:car-ui-lib:2.5.0")
}