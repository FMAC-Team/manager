package me.nekosu.aqnya.ui.navbar

import android.graphics.Color
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.flutter.embedding.android.FlutterTextureView
import io.flutter.embedding.android.FlutterView
import io.flutter.embedding.engine.FlutterEngineCache
import io.flutter.plugin.common.MethodChannel

const val ENGINE_ID = "nav_engine"
const val CHANNEL = "nekosu.aqnya/navbar"

@Composable
fun FlutterNavBar(
    modifier: Modifier = Modifier,
    selectedIndex: Int = 0,
    navBarVisible: Boolean = true,
    onTabSelected: (Int) -> Unit = {},
) {
    val engine =
        remember {
            FlutterEngineCache.getInstance().get(ENGINE_ID)
                ?: error("FlutterEngine not ready, check NkApplication.onCreate()")
        }

    val channel =
        remember {
            MethodChannel(engine.dartExecutor.binaryMessenger, CHANNEL)
        }

    val scheme = MaterialTheme.colorScheme
    LaunchedEffect(selectedIndex, scheme.surfaceContainer, navBarVisible) {
        channel.invokeMethod("setIndex", selectedIndex)
        channel.invokeMethod(
            "setColors",
            mapOf(
                "surfaceContainer" to scheme.surfaceContainer.toArgb(),
                "secondaryContainer" to scheme.secondaryContainer.toArgb(),
                "onSecondaryContainer" to scheme.onSecondaryContainer.toArgb(),
                "onSurfaceVariant" to scheme.onSurfaceVariant.toArgb(),
                "surfaceTint" to scheme.surfaceTint.toArgb(),
            ),
        )
        channel.invokeMethod("setNavBarVisible", navBarVisible)
    }

    DisposableEffect(channel) {
        val currentScheme = scheme
        channel.setMethodCallHandler { call, _ ->
            when (call.method) {
                "onTabSelected" -> {
                    onTabSelected(call.arguments as Int)
                }

                "requestColors" -> {
                    channel.invokeMethod(
                        "setColors",
                        mapOf(
                            "surfaceContainer" to currentScheme.surfaceContainer.toArgb(),
                            "secondaryContainer" to currentScheme.secondaryContainer.toArgb(),
                            "onSecondaryContainer" to currentScheme.onSecondaryContainer.toArgb(),
                            "onSurfaceVariant" to currentScheme.onSurfaceVariant.toArgb(),
                            "surfaceTint" to currentScheme.surfaceTint.toArgb(),
                        ),
                    )
                }
            }
        }
        onDispose { channel.setMethodCallHandler(null) }
    }

    val barHeight by animateDpAsState(
        targetValue = if (navBarVisible) 112.dp else 0.dp,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "navBarHeight",
    )

    AndroidView(
        modifier = modifier.height(barHeight),
        factory = { ctx ->
            val textureView =
                FlutterTextureView(ctx).apply {
                    isOpaque = false
                }

            FlutterView(ctx, textureView).also { view ->
                view.setBackgroundColor(Color.TRANSPARENT)
                view.attachToFlutterEngine(engine)
            }
        },
        onRelease = { view ->
            view.detachFromFlutterEngine()
        },
    )
}
