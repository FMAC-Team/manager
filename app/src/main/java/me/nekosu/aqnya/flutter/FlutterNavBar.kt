package me.nekosu.aqnya.ui.navbar

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.flutter.embedding.android.FlutterSurfaceView
import io.flutter.embedding.android.FlutterView
import io.flutter.embedding.engine.FlutterEngineCache
import io.flutter.plugin.common.MethodChannel

const val ENGINE_ID = "nav_engine"
const val CHANNEL   = "nekosu.aqnya/navbar"

@Composable
fun FlutterNavBar(
    modifier: Modifier = Modifier,
    selectedIndex: Int = 0,
    onTabSelected: (Int) -> Unit = {},
) {
    val engine = remember {
        FlutterEngineCache.getInstance().get(ENGINE_ID)
            ?: error("FlutterEngine not ready, check NkApplication.onCreate()")
    }

    val channel = remember {
        MethodChannel(engine.dartExecutor.binaryMessenger, CHANNEL)
    }

    LaunchedEffect(selectedIndex) {
        channel.invokeMethod("setIndex", selectedIndex)
    }

    DisposableEffect(channel) {
        channel.setMethodCallHandler { call, _ ->
            if (call.method == "onTabSelected") {
                onTabSelected(call.arguments as Int)
            }
        }
        onDispose { channel.setMethodCallHandler(null) }
    }

    AndroidView(
        modifier = modifier.height(112.dp),
        factory = { ctx ->
            FlutterView(ctx, FlutterSurfaceView(ctx)).also { view ->
                view.attachToFlutterEngine(engine)
            }
        },
        onRelease = { view ->
            (view as? FlutterView)?.detachFromFlutterEngine()
        }
    )
}