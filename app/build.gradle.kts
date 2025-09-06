plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.miyo.doctorsaludapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.miyo.doctorsaludapp"
        minSdk = 26
        targetSdk = 34
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }
    viewBinding {
        enable = true
    }
    dataBinding {
        enable = true
    }
    buildToolsVersion = "35.0.0"
    ndkVersion = "29.0.13113456 rc1"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
//
    implementation(libs.facebook.android.sdk.vlatestrelease)
    // Fragment
    implementation(libs.androidx.fragment.ktx)
    // Activity
    implementation (libs.androidx.activity.ktx)
    // ViewModel
    implementation (libs.androidx.lifecycle.viewmodel.ktx)
    // LiveData
    implementation (libs.androidx.lifecycle.livedata.ktx)
    implementation (libs.androidx.lifecycle.runtime.ktx)
    //Corrutinas
    //
    implementation (libs.kotlinx.coroutines.android)
    implementation (libs.kotlinx.coroutines.core)
    implementation (libs.kotlinx.coroutines.play.services)
    //DaggerHilt
    implementation(libs.hilt.android)
    implementation(libs.androidx.annotation)
    kapt(libs.hilt.android.compiler)
    //Timber
    implementation (libs.timber)
    //Lottie
    implementation (libs.lottie)
    //firebase
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    // Google Sign-In
    implementation(libs.play.services.auth)
    // Facebook Sign-In
    // Facebook SDK
    implementation(libs.facebook.android.sdk)
    //Room-Runtime
    implementation(libs.androidx.room.runtime)
    //Room-Compiler
    //rovv
    kapt(libs.androidx.room.compiler)
    //viewpager2
    implementation (libs.androidx.viewpager2)
    //tensorflow
    implementation(libs.tensorflow.lite)
    implementation(libs.tensorflow.lite.support)
    //github / glide
    implementation(libs.glide)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
apply(plugin = "dagger.hilt.android.plugin")