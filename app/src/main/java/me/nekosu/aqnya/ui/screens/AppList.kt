package me.nekosu.aqnya.ui.screens

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.LruCache
import androidx.annotation.StringRes
import androidx.compose.animation.*
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
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
    CAP_FSETID(4, "FSETID", "文件修改后保留 setuid/setgid 位"),
    CAP_KILL(5, "KILL", "向任意进程发送信号"),
    CAP_SETGID(6, "SETGID", "任意更改进程 GID"),
    CAP_SETUID(7, "SETUID", "任意更改进程 UID"),
    CAP_SETPCAP(8, "SETPCAP", "修改进程 capability 集合"),
    CAP_LINUX_IMMUTABLE(9, "LINUX_IMMUTABLE", "修改文件的不可变 (i) 和只追加 (a) 属性"),
    CAP_NET_BIND_SERVICE(10, "NET_BIND_SERVICE", "绑定 1024 以下特权端口"),
    CAP_NET_BROADCAST(11, "NET_BROADCAST", "网络广播和多播访问"),
    CAP_NET_ADMIN(12, "NET_ADMIN", "网络配置（接口/路由/防火墙）"),
    CAP_NET_RAW(13, "NET_RAW", "使用原始/数据报套接字 (ping/tcpdump)"),
    CAP_IPC_LOCK(14, "IPC_LOCK", "锁定共享内存段"),
    CAP_IPC_OWNER(15, "IPC_OWNER", "绕过 IPC 所有者检查"),
    CAP_SYS_MODULE(16, "SYS_MODULE", "加载/卸载内核模块"),
    CAP_SYS_RAWIO(17, "SYS_RAWIO", "访问 /dev/mem、ioperm 等底层 IO"),
    CAP_SYS_CHROOT(18, "SYS_CHROOT", "调用 chroot()"),
    CAP_SYS_PTRACE(19, "SYS_PTRACE", "ptrace/调试任意进程"),
    CAP_SYS_PACCT(20, "SYS_PACCT", "配置进程记账"),
    CAP_SYS_ADMIN(21, "SYS_ADMIN", "万能管理权限（挂载/命名空间/设置主机名等）"),
    CAP_SYS_BOOT(22, "SYS_BOOT", "调用 reboot() 和加载新内核"),
    CAP_SYS_NICE(23, "SYS_NICE", "提升进程优先级/设置调度策略"),
    CAP_SYS_RESOURCE(24, "SYS_RESOURCE", "配置系统资源限制（磁盘配额/保留空间）"),
    CAP_SYS_TIME(25, "SYS_TIME", "修改系统时钟/实时时钟"),
    CAP_SYS_TTY_CONFIG(26, "SYS_TTY_CONFIG", "配置 TTY 设备"),
    CAP_MKNOD(27, "MKNOD", "使用 mknod() 创建特殊文件"),
    CAP_LEASE(28, "LEASE", "对文件建立租借锁"),
    CAP_AUDIT_WRITE(29, "AUDIT_WRITE", "写入内核审计日志"),
    CAP_AUDIT_CONTROL(30, "AUDIT_CONTROL", "配置内核审计子系统"),
    CAP_SETFCAP(31, "SETFCAP", "设置文件 Capabilities"),
    CAP_MAC_OVERRIDE(32, "MAC_OVERRIDE", "绕过强制访问控制 (LSM/SELinux)"),
    CAP_MAC_ADMIN(33, "MAC_ADMIN", "配置/修改策略 (LSM/SELinux)"),
    CAP_SYSLOG(34, "SYSLOG", "特权 syslog 操作/读取 /proc/kmsg"),
    CAP_WAKE_ALARM(35, "WAKE_ALARM", "设置定时唤醒系统的闹钟"),
    CAP_BLOCK_SUSPEND(36, "BLOCK_SUSPEND", "防止系统进入休眠状态"),
    CAP_AUDIT_READ(37, "AUDIT_READ", "读取审计日志流"),
    CAP_PERFMON(38, "PERFMON", "执行性能监控操作"),
    CAP_BPF(39, "BPF", "加载和管理 BPF 程序"),
    CAP_CHECKPOINT_RESTORE(40, "CHECKPOINT_RESTORE", "执行检查点和恢复操作"),
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

    val listState = LazyListState()

    private var _filterMode = mutableStateOf(FilterMode.USER)
    var filterMode: FilterMode
        get() = _filterMode.value
        set(value) {
            _filterMode.value = value
            updateFilteredApps()
        }

    private var _searchQuery = mutableStateOf("")
    var searchQuery: String
        get() = _searchQuery.value
        set(value) {
            _searchQuery.value = value
            updateFilteredApps()
        }

    var isSearching by mutableStateOf(false)

    var allApps by mutableStateOf<List<AppInfo>>(emptyList())
        private set

    var isLoaded by mutableStateOf(false)
        private set

    var appConfigs by mutableStateOf<Map<String, AppConfig>>(emptyMap())
        private set

    var filteredApps by mutableStateOf<List<AppInfo>>(emptyList())
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

    private fun updateFilteredApps() {
        val snapshot = appConfigs
        val q = searchQuery.trim().lowercase()
        filteredApps =
            allApps
                .filter { app ->
                    val passFilter =
                        when (filterMode) {
                            FilterMode.ALL -> true
                            FilterMode.LAUNCHABLE -> app.isLaunchable
                            FilterMode.SYSTEM -> app.isSystem
                            FilterMode.USER -> !app.isSystem
                        }
                    val passSearch =
                        q.isEmpty() ||
                            app.name.lowercase().contains(q) ||
                            app.packageName.lowercase().contains(q)
                    passFilter && passSearch
                }.sortedWith(
                    compareByDescending<AppInfo> { snapshot.containsKey(it.packageName) }
                        .thenBy { it.name.lowercase() },
                )
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
                    } catch (_: Exception) {
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            withContext(Dispatchers.Main) { updateFilteredApps() }
        }
    }

    suspend fun loadApps(forceRefresh: Boolean = false) {
        withContext(Dispatchers.IO) {
            if (!forceRefresh) {
                val versionCode =
                    context.packageManager
                        .getPackageInfo(context.packageName, 0)
                        .longVersionCode
                val cacheKey = "apps_cache_$versionCode"

                val cached = prefs.getString(cacheKey, null)

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
                                name = ai.loadLabel(pm)?.toString()?.takeIf { it.isNotBlank() } ?: pkg.packageName,
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
        updateFilteredApps()

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
    val listState = viewModel.listState
    val apps = viewModel.filteredApps
    val filterMode = viewModel.filterMode
    val searchQuery = viewModel.searchQuery
    val isSearching = viewModel.isSearching
    var menuExpanded by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val refreshState = rememberPullToRefreshState()

    LaunchedEffect(Unit) { viewModel.loadApps() }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    LaunchedEffect(navBackStackEntry) {
        if (navBackStackEntry?.destination?.route?.startsWith("app_detail/") == false) {
            viewModel.refreshAppConfig("")
        }
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
                                viewModel.isSearching = false
                                viewModel.searchQuery = ""
                            }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
                        },
                        title = {
                            TextField(
                                value = searchQuery,
                                onValueChange = { viewModel.searchQuery = it },
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
                                        IconButton(onClick = { viewModel.searchQuery = "" }) {
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
                            IconButton(onClick = { viewModel.isSearching = true }) {
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
                                                viewModel.filterMode = mode
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppInfoItem(
    app: AppInfo,
    config: AppConfig?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
) {
    val isAllowed = config?.allowed == true
    val haptic = LocalHapticFeedback.current

    Card(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
            onClick()
        },
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (isAllowed) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    },
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 0.dp,
                pressedElevation = 2.dp,
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIcon(
                packageName = app.packageName,
                modifier = Modifier.size(44.dp),
            )
            Spacer(Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = app.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (app.isSystem) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                        ) {
                            Text(
                                text = "系统",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                }

                Text(
                    text = "${app.packageName}  ·  UID: ${app.uid}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (isAllowed && config.caps.isNotEmpty()) {
                    val capsText =
                        config.caps.take(3).joinToString(" · ") { it.label } +
                            if (config.caps.size > 3) " +${config.caps.size - 3}" else ""
                    Text(
                        text = capsText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else if (isAllowed) {
                    Text(
                        text = "无 Capabilities",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    )
                }
            }
        }
    }
}

private val iconCache = LruCache<String, ImageBitmap>(200)

@Composable
fun AppIcon(
    packageName: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var iconBitmap by remember(packageName) { mutableStateOf(iconCache.get(packageName)) }

    LaunchedEffect(packageName) {
        if (iconBitmap == null) {
            withContext(Dispatchers.IO) {
                try {
                    val bitmap =
                        context.packageManager
                            .getApplicationIcon(packageName)
                            .toBitmap()
                            .copy(Bitmap.Config.ARGB_8888, false)
                            .asImageBitmap()
                    iconCache.put(packageName, bitmap)
                    iconBitmap = bitmap
                } catch (e: Exception) {
                }
            }
        }
    }

    Crossfade(targetState = iconBitmap, label = "icon_crossfade") { bitmap ->
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "App Icon",
                modifier = modifier,
            )
        } else {
            Box(
                modifier = modifier,
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Android,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    modifier = Modifier.size(24.dp),
                )
            }
        }
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
