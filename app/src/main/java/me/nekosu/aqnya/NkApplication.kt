package me.nekosu.aqnya

import android.app.Application
import android.content.Context
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.FlutterEngineCache
import io.flutter.embedding.engine.dart.DartExecutor
import me.nekosu.aqnya.util.CrashHandler

class NkApplication : Application() {
    val flutterEngine by lazy {
        FlutterEngine(this).apply {
            dartExecutor.executeDartEntrypoint(
                DartExecutor.DartEntrypoint.createDefault(),
            )
            FlutterEngineCache.getInstance().put("nav_engine", this)
        }
    }

    override fun attachBaseContext(base: Context) {
        System.loadLibrary("ncore_init")
        super.attachBaseContext(base)
    }

    override fun onCreate() {
        super.onCreate()
        CrashHandler.init(this)
        flutterEngine
    }
}
