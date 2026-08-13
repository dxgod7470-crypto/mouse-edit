package com.sazanx.mouseconfigurator

import android.app.Application
import rikka.shizuku.ShizukuProvider

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Ensure Shizuku provider hooks initialize globally at app launch
        try {
            ShizukuProvider.enable()
        } catch (_: Throwable) {
            // Ignored if Shizuku isn't active yet
        }
    }
}

