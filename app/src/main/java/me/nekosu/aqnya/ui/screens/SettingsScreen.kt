package me.nekosu.aqnya.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ViewQuilt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import me.nekosu.aqnya.R
import me.nekosu.aqnya.util.DebugPreferences
import me.nekosu.aqnya.util.LogUtils
import me.nekosu.aqnya.util.NavBarStyle

enum class ThemeMode(
    @param:StringRes val titleRes: Int,
    val value: Int,
) {
    SYSTEM(R.string.theme_system, 0),
    LIGHT(R.string.theme_light, 1),
    DARK(R.string.theme_dark, 2),
    ;

    companion object {
        fun fromValue(value: Int) = entries.find { it.value == value } ?: SYSTEM
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val mContext = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val showRules by DebugPreferences.showRulesFlow(mContext).collectAsState(initial = false)

    val themeValue by DebugPreferences.themeModeFlow(mContext).collectAsState(initial = 0)
    val currentThemeMode = ThemeMode.fromValue(themeValue)
    var themeMenuExpanded by remember { mutableStateOf(false) }

    val navBarStyleValue by DebugPreferences.navBarStyleFlow(mContext).collectAsState(initial = 2)
    val currentNavBarStyle = NavBarStyle.fromValue(navBarStyleValue)
    var navBarStyleMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                scrollBehavior = scrollBehavior,
            )
        },
        contentWindowInsets =
            WindowInsets.safeDrawing.only(
                WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
            ),
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            ListItem(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate("about") },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                    )
                },
                headlineContent = {
                    Text(
                        text = stringResource(R.string.about),
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
            )

            ListItem(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { LogUtils.exportLogs(mContext) },
                leadingContent = {
                    Icon(Icons.Outlined.BugReport, contentDescription = null)
                },
                headlineContent = {
                    Text(
                        text = stringResource(R.string.export_log),
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                supportingContent = {
                    Text(stringResource(R.string.export_log_describe))
                },
            )

            ListItem(
                modifier =
                    Modifier.fillMaxWidth().clickable { navController.navigate("debug_settings") },
                leadingContent = { Icon(Icons.Outlined.Science, contentDescription = null) },
                headlineContent = {
                    Text(
                        text = "Debug Settings",
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                supportingContent = { Text("开发者调试选项") },
                trailingContent = { Icon(Icons.Outlined.ChevronRight, contentDescription = null) },
            )

            ListItem(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { themeMenuExpanded = true },
                leadingContent = { Icon(Icons.Outlined.Palette, contentDescription = null) },
                headlineContent = {
                    Text(
                        stringResource(R.string.theme_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                supportingContent = { Text(stringResource(currentThemeMode.titleRes)) },
                trailingContent = {
                    Box {
                        DropdownMenu(
                            expanded = themeMenuExpanded,
                            onDismissRequest = { themeMenuExpanded = false },
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            ThemeMode.entries.forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(mode.titleRes)) },
                                    onClick = {
                                        themeMenuExpanded = false
                                        scope.launch {
                                            DebugPreferences.setThemeMode(mContext, mode.value)
                                        }
                                    },
                                    trailingIcon = {
                                        if (currentThemeMode == mode) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp),
                                            )
                                        }
                                    },
                                )
                            }
                        }
                    }
                },
            )

            ListItem(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { navBarStyleMenuExpanded = true },
                leadingContent = { Icon(Icons.AutoMirrored.Outlined.ViewQuilt, contentDescription = null) },
                headlineContent = {
                    Text(
                        stringResource(R.string.navbar_style_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                supportingContent = { Text(stringResource(currentNavBarStyle.titleRes)) },
                trailingContent = {
                    Box {
                        DropdownMenu(
                            expanded = navBarStyleMenuExpanded,
                            onDismissRequest = { navBarStyleMenuExpanded = false },
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            NavBarStyle.entries.forEach { style ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(style.titleRes)) },
                                    onClick = {
                                        navBarStyleMenuExpanded = false
                                        scope.launch {
                                            DebugPreferences.setNavBarStyle(mContext, style.value)
                                        }
                                    },
                                    trailingIcon = {
                                        if (currentNavBarStyle == style) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp),
                                            )
                                        }
                                    },
                                )
                            }
                        }
                    }
                },
            )

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    SettingsScreen(navController = rememberNavController())
}
