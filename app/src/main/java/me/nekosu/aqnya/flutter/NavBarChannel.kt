package me.nekosu.aqnya.ui.navbar

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.toArgb
import io.flutter.plugin.common.MethodChannel

fun ColorScheme.toNavBarColorMap(): Map<String, Int> = mapOf(
    "surfaceContainer" to surfaceContainer.toArgb(),
    "secondaryContainer" to secondaryContainer.toArgb(),
    "onSecondaryContainer" to onSecondaryContainer.toArgb(),
    "onSurfaceVariant" to onSurfaceVariant.toArgb(),
    "surfaceTint" to surfaceTint.toArgb(),
)

fun MethodChannel.sendColors(scheme: ColorScheme) =
    invokeMethod("setColors", scheme.toNavBarColorMap())

fun MethodChannel.sendIndex(index: Int) =
    invokeMethod("setIndex", index)

fun MethodChannel.sendNavBarVisible(visible: Boolean) =
    invokeMethod("setNavBarVisible", visible)

fun MethodChannel.setNavBarCallHandler(
    scheme: ColorScheme,
    onTabSelected: (Int) -> Unit,
) {
    setMethodCallHandler { call, _ ->
        when (call.method) {
            "onTabSelected" -> onTabSelected(call.arguments as Int)
            "requestColors" -> sendColors(scheme)
        }
    }
}
