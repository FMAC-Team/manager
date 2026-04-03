package me.nekosu.aqnya.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import me.nekosu.aqnya.KeyUtils
import me.nekosu.aqnya.R
import me.nekosu.aqnya.util.AppPermission
import me.nekosu.aqnya.util.BottomNavItem
import me.nekosu.aqnya.util.CheckUpdate
import me.nekosu.aqnya.util.DebugPreferences
import me.nekosu.aqnya.util.MiuiPermissionUtils
import me.nekosu.aqnya.util.rememberPermissionState

@Composable
fun BottomNavigationBar(
    navController: NavController,
    items: List<BottomNavItem>,
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .windowInsetsPadding(WindowInsets.navigationBars),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 8.dp,
            shadowElevation = 16.dp,
        ) {
            Row(
                modifier =
                    Modifier
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                        .height(48.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEach { item ->
                    val selected = currentRoute == item.route

                    val containerColor by animateColorAsState(
                        targetValue =
                            if (selected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                Color.Transparent
                            },
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "navItemBg",
                    )

                    val itemWidth by animateDpAsState(
                        targetValue = if (selected) 88.dp else 48.dp,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "navItemWidth",
                    )

                    Surface(
                        onClick = {
                            navController.navigate(item.route) {
                                launchSingleTop = true
                                restoreState = true
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                            }
                        },
                        shape = RoundedCornerShape(50),
                        color = containerColor,
                        modifier =
                            Modifier
                                .height(48.dp)
                                .width(itemWidth),
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(horizontal = 12.dp),
                            ) {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.title,
                                    tint =
                                        if (selected) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                    modifier = Modifier.size(22.dp),
                                )
                                AnimatedVisibility(
                                    visible = selected,
                                    enter = fadeIn(tween(200)),
                                    exit = fadeOut(tween(150)),
                                ) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        maxLines = 1,
                                        softWrap = false,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val context = LocalContext.current
    var showKeyDialog by remember { mutableStateOf(false) }
    val showRules by DebugPreferences.showRulesFlow(context).collectAsState(initial = false)
    val navItems = remember(showRules) { BottomNavItem.items(showRules) }

    val miuiAppsPermState = rememberPermissionState(AppPermission.MIUI_GET_INSTALLED_APPS)

    LaunchedEffect(Unit) {
        if (!KeyUtils.checkKeyExists(context)) {
            showKeyDialog = true
        }
        if (MiuiPermissionUtils.isSupportedOnThisDevice(context) &&
            !MiuiPermissionUtils.isGranted(context)
        ) {
            miuiAppsPermState.launchRequest()
        }
    }

    LaunchedEffect(showRules) {
        if (!showRules &&
            navController.currentBackStackEntry?.destination?.route == BottomNavItem.FmacRules.route
        ) {
            navController.popBackStack()
        }
    }

    Scaffold { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            NavHost(
                navController = navController,
                startDestination = BottomNavItem.Home.route,
                modifier = Modifier.fillMaxSize(),
            ) {
                val commonTween = tween<Float>(300)

                composable(
                    route = BottomNavItem.Home.route,
                    enterTransition = { fadeIn(commonTween) },
                    exitTransition = { fadeOut(commonTween) },
                    popEnterTransition = { fadeIn(commonTween) },
                    popExitTransition = { fadeOut(commonTween) },
                ) { HomeScreen() }

                composable(
                    route = BottomNavItem.History.route,
                    enterTransition = { fadeIn(commonTween) },
                    exitTransition = { fadeOut(commonTween) },
                    popEnterTransition = { fadeIn(commonTween) },
                    popExitTransition = { fadeOut(commonTween) },
                ) { HistoryScreen() }

                composable(
                    route = BottomNavItem.FmacRules.route,
                    enterTransition = { fadeIn(commonTween) },
                    exitTransition = { fadeOut(commonTween) },
                    popEnterTransition = { fadeIn(commonTween) },
                    popExitTransition = { fadeOut(commonTween) },
                ) { RulesScreen() }

                composable(
                    route = BottomNavItem.Settings.route,
                    enterTransition = { fadeIn(commonTween) },
                    exitTransition = { fadeOut(commonTween) },
                    popEnterTransition = { fadeIn(commonTween) },
                    popExitTransition = { fadeOut(commonTween) },
                ) { SettingsScreen(navController) }

                composable(
                    route = "about",
                    enterTransition = { fadeIn(commonTween) },
                    exitTransition = { fadeOut(commonTween) },
                ) { AboutScreen(navController) }

                composable(
                    route = "open_source",
                    enterTransition = { fadeIn(commonTween) },
                    exitTransition = { fadeOut(commonTween) },
                ) { OpenSourceScreen(navController) }

                composable(
                    route = "debug_settings",
                    enterTransition = { fadeIn(commonTween) },
                    exitTransition = { fadeOut(commonTween) },
                ) { DebugSettingsScreen(navController) }
            }

            KeyInputDialog(
                show = showKeyDialog,
                onDismiss = { showKeyDialog = false },
            )

            CheckUpdate(owner = "aqnya", repo = "nekosu")

            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                BottomNavigationBar(navController, navItems)
            }
        }
    }
}

@Composable
fun KeyInputDialog(
    show: Boolean,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    var errorType by remember { mutableIntStateOf(0) }
    val scrollState = rememberScrollState()

    AnimatedVisibility(
        visible = show,
        enter = fadeIn(tween(250)) + scaleIn(initialScale = 0.8f, animationSpec = tween(250)),
        exit = fadeOut(tween(200)) + scaleOut(targetScale = 0.8f, animationSpec = tween(200)),
    ) {
        AlertDialog(
            onDismissRequest = { onDismiss() },
            title = {
                Text(
                    text = stringResource(R.string.dialog_key_set),
                    style = TextStyle(fontSize = 16.sp),
                )
            },
            text = {
                Column(
                    modifier =
                        Modifier
                            .verticalScroll(scrollState)
                            .fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.dialog_key_please_input))
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = {
                            inputText = it
                            errorType = 0
                        },
                        label = { Text("ECC Key (PEM/Base64)", fontSize = 14.sp) },
                        placeholder = {
                            Text("-----BEGIN EC PRIVATE KEY-----...", fontSize = 14.sp)
                        },
                        singleLine = false,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp, max = 240.dp),
                        isError = errorType != 0,
                        supportingText = {
                            when (errorType) {
                                1 -> {
                                    Text(
                                        stringResource(R.string.dialog_key_input_no_empty),
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                }

                                2 -> {
                                    Text(
                                        stringResource(R.string.dialog_key_input_invalid),
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        },
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmedKey = inputText.trim()
                        errorType =
                            when {
                                trimmedKey.isBlank() -> 1
                                !KeyUtils.isValidECCKey(trimmedKey) -> 2
                                else -> 0
                            }
                        if (errorType == 0) {
                            KeyUtils.saveKey(context, trimmedKey)
                            onDismiss()
                        }
                    },
                ) { Text(stringResource(R.string.dialog_key_save)) }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.dialog_key_later))
                }
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    MainScreen()
}
