package me.nekosu.aqnya.ui.screens

import android.content.ContentValues
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.annotation.StringRes
import androidx.compose.animation.*
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.nekosu.aqnya.R

class RootDbHelper(
    context: Context,
) : SQLiteOpenHelper(context, "root_manager.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE root_apps (packageName TEXT PRIMARY KEY, allowed INTEGER)")
    }

    fun getAllowedPackages(): Set<String> {
        val set = mutableSetOf<String>()
        readableDatabase.rawQuery("SELECT packageName FROM root_apps WHERE allowed = 1", null).use { cursor ->
            while (cursor.moveToNext()) {
                set.add(cursor.getString(0))
            }
        }
        return set
    }

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int,
    ) {
        db.execSQL("DROP TABLE IF EXISTS root_apps")
        onCreate(db)
    }

    fun setAllowed(
        packageName: String,
        allowed: Boolean,
    ) {
        val values =
            ContentValues().apply {
                put("packageName", packageName)
                put("allowed", if (allowed) 1 else 0)
            }
        writableDatabase.insertWithOnConflict("root_apps", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }
}

data class AppInfo(
    val name: String,
    val packageName: String,
    val uid: Int,
    val isSystem: Boolean,
    val isLaunchable: Boolean,
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

    var allowedApps by mutableStateOf<Set<String>>(emptySet())
        private set

    init {
        viewModelScope.launch(Dispatchers.IO) {
            allowedApps = dbHelper.getAllowedPackages()
        }
    }

    suspend fun loadApps(forceRefresh: Boolean = false) {
        withContext(Dispatchers.IO) {
            if (!forceRefresh) {
                val cached = prefs.getString("apps_cache", null)
                if (cached != null) {
                    val type = object : TypeToken<List<AppInfo>>() {}.type
                    val list: List<AppInfo> = gson.fromJson(cached, type)
                    allApps = list
                    isLoaded = true
                    if (allApps.isNotEmpty()) return@withContext
                }
            }

            val pm = context.packageManager
            val installed =
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

            allApps = installed
            isLoaded = true
            prefs.edit().putString("apps_cache", gson.toJson(installed)).apply()
        }
    }

    fun toggleRootPermission(
        app: AppInfo,
        allow: Boolean,
    ) {
        allowedApps =
            if (allow) {
                allowedApps + app.packageName
            } else {
                allowedApps - app.packageName
            }

        viewModelScope.launch(Dispatchers.IO) {
            dbHelper.setAllowed(app.packageName, allow)
            // TODO
        }
    }
}

class AppViewModelFactory(
    private val context: Context,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = AppViewModel(context) as T
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen() {
    val context = LocalContext.current.applicationContext
    val viewModel: AppViewModel = viewModel(factory = AppViewModelFactory(context))
    var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var filterMode by remember { mutableStateOf(FilterMode.USER) }
    var searchQuery by remember { mutableStateOf("") }
    var menuExpanded by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.loadApps()
    }

    LaunchedEffect(viewModel.allApps, filterMode, searchQuery) {
        apps =
            viewModel.allApps.filter { app ->
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
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearching) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("搜索应用...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors =
                                TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                ),
                        )
                    } else {
                        Text(
                            text = stringResource(filterMode.labelRes),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                },
                navigationIcon = {
                    if (isSearching) {
                        IconButton(onClick = {
                            isSearching = false
                            searchQuery = ""
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    }
                },
                actions = {
                    if (!isSearching) {
                        IconButton(onClick = { isSearching = true }) {
                            Icon(Icons.Default.Search, contentDescription = null)
                        }
                        IconButton(onClick = {
                            scope.launch { viewModel.loadApps(forceRefresh = true) }
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                        }
                    }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.FilterList, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            FilterMode.entries.forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(mode.labelRes)) },
                                    onClick = {
                                        filterMode = mode
                                        menuExpanded = false
                                    },
                                )
                            }
                        }
                    }
                },
                colors =
                    TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
            )
        },
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (!viewModel.isLoaded) {
                CircularProgressIndicator(strokeWidth = 3.dp)
            } else if (apps.isEmpty()) {
                Text(
                    text = "未找到相关应用",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding =
                        PaddingValues(
                            top = 12.dp,
                            bottom = 80.dp,
                            start = 0.dp,
                            end = 0.dp,
                        ),
                ) {
                    items(apps, key = { it.packageName }) { app ->
                        AppInfoItem(
                            app = app,
                            isAllowed = viewModel.allowedApps.contains(app.packageName),
                            onToggle = { checked ->
                                viewModel.toggleRootPermission(app, checked)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppInfoItem(
    app: AppInfo,
    isAllowed: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var isOverflown by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .animateContentSize(spring(Spring.DampingRatioNoBouncy, Spring.StiffnessHigh))
                    .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(0.08f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                AppIcon(app.packageName, Modifier.size(30.dp))
            }

            Spacer(Modifier.width(16.dp))

            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(end = 24.dp)
                        .let { modifier ->
                            if (isOverflown || expanded) {
                                modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { expanded = !expanded }
                            } else {
                                modifier
                            }
                        },
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = app.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                AnimatedContent(
                    targetState = expanded,
                    transitionSpec = {
                        val fastSpec = tween<IntSize>(durationMillis = 250)
                        val fastFadeSpec = tween<Float>(durationMillis = 200)

                        (fadeIn(fastFadeSpec) + expandHorizontally(animationSpec = fastSpec, expandFrom = Alignment.Start))
                            .togetherWith(
                                fadeOut(fastFadeSpec) + shrinkHorizontally(animationSpec = fastSpec, shrinkTowards = Alignment.Start),
                            )
                    },
                    label = "textReveal",
                ) { isExpanded ->
                    Text(
                        text = "${app.packageName} • UID: ${app.uid}",
                        style =
                            MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                            ),
                        maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                        overflow = TextOverflow.Ellipsis,
                        onTextLayout = { textLayoutResult ->
                            if (!isExpanded) {
                                isOverflown = textLayoutResult.hasVisualOverflow
                            }
                        },
                    )
                }
            }

            Switch(
                checked = isAllowed,
                onCheckedChange = onToggle,
                thumbContent =
                    if (isAllowed) {
                        { Icon(Icons.Filled.CheckCircle, null, Modifier.size(SwitchDefaults.IconSize)) }
                    } else {
                        null
                    },
            )
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
                } catch (e: Exception) {
                    null
                }
            }
    }

    if (iconBitmap != null) {
        Image(
            bitmap = iconBitmap!!,
            contentDescription = null,
            modifier = modifier,
        )
    } else {
        Icon(
            imageVector = Icons.Default.Android,
            contentDescription = null,
            modifier = modifier,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewHistoryScreen() {
    MaterialTheme {
        HistoryScreen()
    }
}
