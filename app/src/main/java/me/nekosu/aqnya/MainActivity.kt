package me.nekosu.aqnya

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import me.nekosu.aqnya.ui.screens.MainScreen
import me.nekosu.aqnya.ui.theme.NekosuTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NekosuTheme {
                MainScreen()
            }
        }
    }
}
