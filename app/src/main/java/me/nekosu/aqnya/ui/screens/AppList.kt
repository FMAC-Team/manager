package me.nekosu.aqnya.ui.screens

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.annotation.StringRes
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.nekosu.aqnya.R
import me.nekosu.aqnya.ncore
import me.nekosu.aqnya.util.RootDbHelper

enum class LinuxCap(
    val value: Int,
    val label: String,
    val description: String,
) {
    CAP_CHOWN(0, "CHOWN", "任意更改文件 UID/GID"),
    CAP_DAC_OVERRIDE(1, "DAC_OVERRIDE", "绕过文件读写执行权限检查"),
    CAP_DAC_READ_SEARCH(2, "DAC_READ_SEARCH", "绕过文件读取/目录搜索权限检查"),
    CAP_FOWNER(3, "FOWNER", "绕过需要文件所有者检查的操作"),
    CAP_SETUID(7, "SETUID", "任意更改进程 UID"),
    CAP_SETGID(6, "SETGID", "任意更改进程 GID"),
    CAP_SETPCAP(8, "SETPCAP", "修改进程 capability 集合"),
    CAP_NET_ADMIN(12, "NET_ADMIN", "网络配置（接口/路由/防火墙）"),
    CAP_NET_RAW(13, "NET_RAW", "使用原始/数据报套接字"),
    CAP_SYS_ADMIN(21, "SYS_ADMIN", "大量系统管理操作（挂载等）"),
    CAP_SYS_CHROOT(18, "SYS_CHROOT", "调用 chroot()"),
    CAP_SYS_PTRACE(19, "SYS_PTRACE", "ptrace 任意进程"),
    CAP_SYS_MODULE(16, "SYS_MODULE", "加载/卸载内核模块"),
    CAP_SYS_RAWIO(17, "SYS_RAWIO", "访问 /dev/mem、ioperm 等"),
    CAP_KILL(5, "KILL", "向任意进程发送信号"),
    CAP_AUDIT_WRITE(29, "AUDIT_WRITE", "写入内核审计日志"),
}

val DEFAULT_CAPS: Set<LinuxCap> =
    setOf(
        LinuxCap.CAP_CHOWN,
        LinuxCap.CAP_DAC_OVERRIDE,
        LinuxCap.CAP_DAC_READ_SEARCH,
        LinuxCap.CAP_FOWNER,
        LinuxCap.CAP_SETUID,
        LinuxCap.CAP_SETGID,
        LinuxCap.CAP_SYS_ADMIN,
    )

data class AppInfo(
    val name: String,
    val packageName: String,
    val uid: Int,
    val isSystem: Boolean,
    val isLaunchable: Boolean,
)

data class AppConfig(
    val allowed: Boolean,
    val caps: Set<LinuxCap> = DEFAULT_CAPS,
)

enum class FilterMode(
    @param:StringRes val labelRes: Int,
) {
    ALL(R.string.all_app),
    LAUNCHABLE(R.string.can_launch_app),
    SYSTEM(R.string.system_app),
    USER(R.string.user_app),
}

class AppViewModel(
    private val context: Context,
) : ViewModel() {
    private val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val dbHelper = RootDbHelper(context)

    var allApps by mutableStateOf<List<AppInfo>>(emptyList())
        private set

    var isLoaded by mutableStateOf(false)
        private set

    var appConfigs by mutableStateOf<Map<String, AppConfig>>(emptyMap())
        private set

    init {
        viewModelScope.launch(Dispatchers.IO) {
            loadAppConfigs()
        }
    }

    override fun onCleared() {
        super.onCleared()
        dbHelper.close()
    }

private suspend fun loadAppConfigs() {
    withContext(Dispatchers.IO) {
        try {
            val allowed = dbHelper.getAllowedPackages()
            val configs = mutableMapOf<String, AppConfig>()

            for (pkg in allowed) {
                val capsJson = prefs.getString("caps_$pkg", null)
                val caps =
                    if (capsJson != null) {
                        try {
                            val type = object : TypeToken<Set<String>>() {}.type
                            val capLabels = gson.fromJson<Set<String>>(capsJson, type)
                            LinuxCap.entries.filter { it.label in capLabels }.toSet()
                        } catch (_: Exception) {
                            DEFAULT_CAPS
                        }
                    } else {
                        DEFAULT_CAPS
                    }
                configs[pkg] = AppConfig(allowed = true, caps = caps)
            }

            appConfigs = configs

            val nc = ncore()
            val pm = context.packageManager
            for ((pkg, cfg) in configs) {
                try {
                    val uid = pm.getApplicationInfo(pkg, 0).uid
                    if (nc.hasuid(uid) == 0) nc.adduid(uid)
                    val capsBits = cfg.caps.fold(0L) { acc, cap -> acc or (1L shl cap.value) }
                    nc.setCap(uid, capsBits)
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

    suspend fun loadApps(forceRefresh: Boolean = false) {
        withContext(Dispatchers.IO) {
            if (!forceRefresh) {
                val cached = prefs.getString("apps_cache", null)
                if (cached != null) {
                    val type = object : TypeToken<List<AppInfo>>() {}.type
                    allApps = gson.fromJson(cached, type)
                    isLoaded = true
                    if (allApps.isNotEmpty()) {
                        loadAppConfigs()
                        return@withContext
                    }
                }
            }
            val pm = context.packageManager
            allApps =
                pm
                    .getInstalledPackages(PackageManager.GET_META_DATA)
                    .mapNotNull { pkg ->
                        pkg.applicationInfo?.let { ai ->
                            AppInfo(
                                name = ai.loadLabel(pm).toString(),
                                packageName = pkg.packageName,
                                uid = ai.uid,
                                isSystem = (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                                isLaunchable = pm.getLaunchIntentForPackage(pkg.packageName) != null,
                            )
                        }
                    }.sortedBy { it.name.lowercase() }
            isLoaded = true
            prefs.edit().putString("apps_cache", gson.toJson(allApps)).apply()
            loadAppConfigs()
        }
    }

fun setAppConfig(
    app: AppInfo,
    config: AppConfig,
) {
    appConfigs =
        if (config.allowed) {
            appConfigs + (app.packageName to config)
        } else {
            appConfigs - app.packageName
        }

    viewModelScope.launch(Dispatchers.IO) {
        dbHelper.setAllowed(app.packageName, config.allowed)

        val nc = ncore()
        if (config.allowed) {
            val capsJson = gson.toJson(config.caps.map { it.label }.toSet())
            prefs.edit().putString("caps_${app.packageName}", capsJson).apply()

            nc.adduid(app.uid)
            val capsBits = config.caps.fold(0L) { acc, cap -> acc or (1L shl cap.value) }
            nc.setCap(app.uid, capsBits)
        } else {
            prefs.edit().remove("caps_${app.packageName}").apply()
            nc.delCap(app.uid)
            nc.deluid(app.uid)
        }
    }
}

    fun refreshAppConfig(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            loadAppConfigs()
        }
    }
}

class AppViewModelFactory(
    private val context: Context,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = AppViewModel(context) as T
}

@Composable
fun getAdapterShape(
    index: Int,
    totalCount: Int,
    cornerRadius: Dp = 20.dp,
): Shape =
    when {
        totalCount <= 1 -> RoundedCornerShape(cornerRadius)
        index == 0 -> RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius)
        index == totalCount - 1 -> RoundedCornerShape(bottomStart = cornerRadius, bottomEnd = cornerRadius)
        else -> RoundedCornerShape(0.dp)
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    navController: NavController,
    extraBottomPadding: Dp = 96.dp,
) {
    val context = LocalContext.current.applicationContext
    val viewModel: AppViewModel = viewModel(factory = AppViewModelFactory(context))
    val listState = rememberLazyListState()
    var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var filterMode by remember { mutableStateOf(FilterMode.USER) }
    var searchQuery by remember { mutableStateOf("") }
    var menuExpanded by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val refreshState = rememberPullToRefreshState()

    LaunchedEffect(searchQuery) { listState.scrollToItem(0) }
    LaunchedEffect(Unit) { viewModel.loadApps() }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    LaunchedEffect(navBackStackEntry) {
        if (navBackStackEntry?.destination?.route?.startsWith("app_detail/") == false) {
            viewModel.refreshAppConfig("")
        }
    }

    val sortSnapshotAllowed = remember { mutableStateOf<Map<String, AppConfig>>(emptyMap()) }
    LaunchedEffect(viewModel.allApps, filterMode, searchQuery, viewModel.appConfigs) {
        sortSnapshotAllowed.value = viewModel.appConfigs
        apps =
            viewModel.allApps
                .filter { app ->
                    val passFilter =
                        when (filterMode) {
                            FilterMode.ALL -> true
                            FilterMode.LAUNCHABLE -> app.isLaunchable
                            FilterMode.SYSTEM -> app.isSystem
                            FilterMode.USER -> !app.isSystem
                        }
                    val q = searchQuery.trim().lowercase()
                    val passSearch =
                        q.isEmpty() ||
                            app.name.lowercase().contains(q) ||
                            app.packageName.lowercase().contains(q)
                    passFilter && passSearch
                }.sortedWith(
                    compareByDescending<AppInfo> { sortSnapshotAllowed.value.containsKey(it.packageName) }
                        .thenBy { it.name.lowercase() },
                )
    }

    Scaffold(
        topBar = {
            AnimatedContent(
                targetState = isSearching,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "topBarSwitch",
            ) { searching ->
                if (searching) {
                    TopAppBar(
                        navigationIcon = {
                            IconButton(onClick = {
                                isSearching = false
                                searchQuery = ""
                            }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
                        },
                        title = {
                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = {
                                    Text(
                                        "搜索应用…",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors =
                                    TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                    ),
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = "清空")
                                        }
                                    }
                                },
                            )
                        },
                    )
                } else {
                    TopAppBar(
                        title = {
                            Text(
                                text = stringResource(filterMode.labelRes),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        },
                        actions = {
                            IconButton(onClick = { isSearching = true }) {
                                Icon(Icons.Default.Search, null)
                            }
                            Box {
                                IconButton(onClick = { menuExpanded = true }) {
                                    Icon(Icons.Default.FilterList, null)
                                }
                                DropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = { menuExpanded = false },
                                    shape = RoundedCornerShape(20.dp),
                                ) {
                                    FilterMode.entries.forEach { mode ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    stringResource(mode.labelRes),
                                                    fontWeight = if (mode == filterMode) FontWeight.SemiBold else FontWeight.Normal,
                                                )
                                            },
                                            leadingIcon =
                                                if (mode == filterMode) {
                                                    { Icon(Icons.Default.CheckCircle, null, Modifier.size(18.dp)) }
                                                } else {
                                                    null
                                                },
                                            onClick = {
                                                filterMode = mode
                                                menuExpanded = false
                                            },
                                        )
                                    }
                                }
                            }
                        },
                    )
                }
            }
        },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                if (!isRefreshing) {
                    isRefreshing = true
                    scope.launch {
                        viewModel.loadApps(forceRefresh = true)
                        isRefreshing = false
                    }
                }
            },
            state = refreshState,
            indicator = {
                Indicator(
                    modifier = Modifier.align(Alignment.TopCenter),
                    isRefreshing = isRefreshing,
                    state = refreshState,
                    color = MaterialTheme.colorScheme.primary,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    threshold = 40.dp,
                )
            },
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            when {
                !viewModel.isLoaded -> {
                    LoadingState()
                }

                apps.isEmpty() -> {
                    EmptyState(isSearching, searchQuery)
                }

                else -> {
                    val allowedList = apps.filter { viewModel.appConfigs.containsKey(it.packageName) }
                    val otherList = apps.filter { !viewModel.appConfigs.containsKey(it.packageName) }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        contentPadding = PaddingValues(top = 12.dp, bottom = extraBottomPadding),
                    ) {
                        if (allowedList.isNotEmpty()) {
                            item { SectionLabel("已授权  ·  ${allowedList.size}") }
                            itemsIndexed(allowedList, key = { _, it -> "allowed_${it.packageName}" }) { index, app ->
                                AppInfoItem(
                                    app = app,
                                    config = viewModel.appConfigs[app.packageName],
                                    onClick = {
                                        navController.navigate("app_detail/${app.packageName}")
                                    },
                                    shape = getAdapterShape(index, allowedList.size),
                                    modifier = Modifier.animateItem(),
                                )
                            }
                            item { Spacer(Modifier.height(12.dp)) }
                        }

                        if (otherList.isNotEmpty()) {
                            itemsIndexed(otherList, key = { _, it -> it.packageName }) { index, app ->
                                AppInfoItem(
                                    app = app,
                                    config = null,
                                    onClick = {
                                        navController.navigate("app_detail/${app.packageName}")
                                    },
                                    shape = getAdapterShape(index, otherList.size),
                                    modifier = Modifier.animateItem(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 2.dp),
    )
}

@Composable
fun LoadingState() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CircularProgressIndicator(strokeWidth = 2.5.dp, modifier = Modifier.size(36.dp))
        Text(
            "加载应用列表…",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        )
    }
}

@Composable
fun EmptyState(
    isSearching: Boolean,
    query: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = if (isSearching) Icons.Default.Search else Icons.Default.Android,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
        )
        Text(
            text = if (isSearching && query.isNotBlank()) "未找到「$query」" else "暂无应用",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        )
    }
}

@Composable
fun AppInfoItem(
    app: AppInfo,
    config: AppConfig?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
) {
    val isAllowed = config?.allowed == true
    var expanded by remember { mutableStateOf(false) }
    var isOverflown by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (isAllowed) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f)
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)
                    },
            ),
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
            onClick()
        },
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .animateContentSize(spring(Spring.DampingRatioNoBouncy, Spring.StiffnessHigh))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(46.dp), contentAlignment = Alignment.Center) {
                AppIcon(
                    packageName = app.packageName,
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)),
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                        .let { mod ->
                            if (isOverflown || expanded) {
                                mod.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { expanded = !expanded }
                            } else {
                                mod
                            }
                        },
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = app.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (app.isSystem) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                        ) {
                            Text(
                                text = "系统",
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                }

                AnimatedContent(
                    targetState = expanded,
                    transitionSpec = {
                        (fadeIn(tween(180)) + expandVertically(tween(220)))
                            .togetherWith(fadeOut(tween(180)) + shrinkVertically(tween(220)))
                    },
                    label = "pkgReveal",
                ) { isExpanded ->
                    Text(
                        text = if (isExpanded) "${app.packageName}\nUID: ${app.uid}" else "${app.packageName}  ·  ${app.uid}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 14.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                        maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                        overflow = TextOverflow.Ellipsis,
                        onTextLayout = { if (!isExpanded) isOverflown = it.hasVisualOverflow },
                    )
                }

                if (isAllowed && config != null) {
                    Text(
                        text =
                            if (config.caps.isEmpty()) {
                                "无 capabilities"
                            } else {
                                config.caps.take(4).joinToString(" · ") { it.label } +
                                    if (config.caps.size > 4) " +${config.caps.size - 4}" else ""
                            },
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
fun AppIcon(
    packageName: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val iconBitmap by produceState<ImageBitmap?>(null, packageName) {
        value =
            withContext(Dispatchers.IO) {
                try {
                    context.packageManager
                        .getApplicationIcon(packageName)
                        .toBitmap()
                        .asImageBitmap()
                } catch (_: Exception) {
                    null
                }
            }
    }
    if (iconBitmap != null) {
        Image(bitmap = iconBitmap!!, contentDescription = null, modifier = modifier)
    } else {
        Icon(
            Icons.Default.Android,
            contentDescription = null,
            modifier = modifier,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewHistoryScreen() {
    val navController = androidx.navigation.compose.rememberNavController()
    MaterialTheme {
        HistoryScreen(navController = navController)
    }
}
