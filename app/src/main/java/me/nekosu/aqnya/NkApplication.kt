package me.nekosu.aqnya

import android.app.Application
import android.os.Process
import android.util.Log
import kotlin.system.exitProcess
import me.nekosu.aqnya.R
import me.nekosu.aqnya.util.CrashHandler

class NkApplication : Application() {
    override fun onCreate() {
        CrashHandler.init(this)
        super.onCreate()
    }
}
