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

    // Enable viewBinding
    viewBinding {
        enable = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // AndroidX and Material Design
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    implementation(libs.swiperefreshlayout)
    implementation(libs.swiperefreshlayout)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Import the Firebase BoM
    implementation(platform(libs.firebase.bom))

    // Declare Firebase dependencies without versions
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.messaging)
    implementation(libs.google.firebase.analytics)

    // Volley
    implementation(libs.volley)

    // Glide
    implementation(libs.glide)

    // Circular ImageView
    implementation(libs.circularimageview)

    // ML Kit Smart Reply
    implementation(libs.smart.reply)

    // AndroidX Activity and Fragment
    implementation(libs.activity.v180)
    implementation(libs.fragment)

    // Car UI Library
    implementation(libs.car.ui.lib)

    annotationProcessor (libs.compiler)
    implementation (libs.core.ktx) // For FileProvider
}