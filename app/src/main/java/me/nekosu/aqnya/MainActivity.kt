package me.nekosu.aqnya

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.flutter.embedding.engine.FlutterEngineCache
import me.nekosu.aqnya.ui.screens.MainScreen
import me.nekosu.aqnya.ui.theme.NekosuTheme

class MainActivity : ComponentActivity() {
    private val engine get() = FlutterEngineCache.getInstance().get("nav_engine")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NekosuTheme {
                MainScreen()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        engine?.lifecycleChannel?.appIsResumed()
    }

    override fun onPause() {
        super.onPause()
        engine?.lifecycleChannel?.appIsInactive()
    }

    override fun onStop() {
        super.onStop()
        engine?.lifecycleChannel?.appIsPaused()
    }

    override fun onDestroy() {
        super.onDestroy()
        engine?.lifecycleChannel?.appIsDetached()
    }
}
