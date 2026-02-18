package me.nekosu.aqnya

import android.app.Application
import android.os.Process
import android.util.Log
import kotlin.system.exitProcess
import me.nekosu.aqnya.R
import me.nekosu.aqnya.ui.util.CrashHandler
import me.nekosu.aqnya.util.SigCheck

class NkApplication : Application() {
    override fun onCreate() {
        CrashHandler.init(this)
        super.onCreate()
        if (!SigCheck.validate(this)) {
            Log.w("NkApplication", getString(R.string.sig_check_failed))
            Process.killProcess(Process.myPid())
            exitProcess(1)
        }
    }
}
