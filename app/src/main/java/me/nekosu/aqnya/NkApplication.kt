package me.nekosu.aqnya

import android.app.Application
import android.os.Process
import android.util.Log
import me.nekosu.aqnya.R
import me.nekosu.aqnya.util.CrashHandler
import kotlin.system.exitProcess
import android.content.Context

class NkApplication : Application() {
override fun attachBaseContext(base: Context) {
        System.loadLibrary("ncore_init")
        super.attachBaseContext(base)
    }

    override fun onCreate() {
        CrashHandler.init(this)
        super.onCreate()
    }
}
