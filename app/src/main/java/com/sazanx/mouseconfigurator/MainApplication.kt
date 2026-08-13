package com.sazanx.mouseconfigurator

import android.app.Application
import android.util.Log

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Catch early unhandled runtime crashes globally
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("MouseConfigurator", "Uncaught exception on thread ${thread.name}", throwable)
        }
    }
}
