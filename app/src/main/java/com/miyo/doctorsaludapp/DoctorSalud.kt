package com.miyo.doctorsaludapp

import android.app.Application
import android.util.Log
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsLogger
import com.google.firebase.Firebase
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

class DoctorSalud : Application() {

    override fun onCreate() {
        super.onCreate()
        initFacebook()
        initFirebaseAppCheck()
    }

    private fun initFacebook() {
        FacebookSdk.sdkInitialize(applicationContext)
        AppEventsLogger.activateApp(this)
    }

    private fun initFirebaseAppCheck() {
        val appCheck: FirebaseAppCheck = Firebase.appCheck
        if (BuildConfig.DEBUG) {
            appCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
            Log.i("AppCheck", "DebugAppCheckProvider instalado")
        } else {
            appCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
            Log.i("AppCheck", "PlayIntegrityAppCheckProvider instalado")
        }
    }
}
