package me.nekosu.aqnya.ui.screens

import android.content.ContentValues
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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

class RootDbHelper(context: Context) : SQLiteOpenHelper(context, "root_manager.db", null, 1) {
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
    
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS root_apps")
        onCreate(db)
    }

    fun setAllowed(packageName: String, allowed: Boolean) {
        val values = ContentValues().apply {
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
    val isLaunchable: Boolean
)

enum class FilterMode(@param:StringRes val labelRes: Int) {
    ALL(R.string.all_app),
    LAUNCHABLE(R.string.can_launch_app),
    SYSTEM(R.string.system_app),
    USER(R.string.user_app)
}

class AppViewModel(private val context: Context) : ViewModel() {
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
                                isLaunchable = pm.getLaunchIntentForPackage(
                                    pkg.packageName
                                ) != null
                            )
                        }
                    }.sortedBy { it.name.lowercase() }

            allApps = installed
            isLoaded = true
            prefs.edit().putString("apps_cache", gson.toJson(installed)).apply()
        }
    }

    fun toggleRootPermission(app: AppInfo, allow: Boolean) {
        allowedApps = if (allow) {
            allowedApps + app.packageName
        } else {
            allowedApps - app.packageName
        }

        viewModelScope.launch(Dispatchers.IO) {
            dbHelper.setAllowed(app.packageName, allow)
            if (allow) {
                try {
                //TODO
                } catch (e: UnsatisfiedLinkError) {
                    e.printStackTrace()
                }
            }
        }
    }
}

class AppViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = AppViewModel(context) as T
}

@Composable
fun AppIcon(packageName: String) {
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
        Image(bitmap = iconBitmap!!, contentDescription = null, modifier = Modifier.size(40.dp))
    } else {
        Icon(Icons.Default.Android, contentDescription = null, modifier = Modifier.size(40.dp))
    }
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
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text(stringResource(R.string.search_hint)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    } else {
                        Text(stringResource(filterMode.labelRes))
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
                            scope.launch {
                                viewModel.loadApps(forceRefresh = true)
                            }
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                        }
                    }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.FilterList, contentDescription = null)
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = {
                            menuExpanded =
                                false
                        }) {
                            FilterMode.values().forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(mode.labelRes)) },
                                    onClick = {
                                        filterMode = mode
                                        menuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            if (!viewModel.isLoaded) {
                CircularProgressIndicator()
            } else if (apps.isEmpty()) {
                Text(stringResource(R.string.no_app_found))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = 8.dp,
                        bottom = 96.dp + androidx.compose.foundation.layout.WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    )
                ) {
                    items(apps, key = { it.packageName }) { app ->
                        val isAllowed = viewModel.allowedApps.contains(app.packageName)

                        ListItem(
                            headlineContent = { Text(app.name) },
                            supportingContent = {
                                Column {
                                    Text(app.packageName)
                                    Text("UID: ${app.uid}")
                                }
                            },
                            leadingContent = { AppIcon(app.packageName) },
                            trailingContent = {
                                Switch(
                                    checked = isAllowed,
                                    onCheckedChange = { checked ->
                                        viewModel.toggleRootPermission(app, checked)
                                    }
                                )
                            },
                            modifier = Modifier.clickable { 
                                // viewModel.toggleRootPermission(app, !isAllowed)
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewHistoryScreen() {
    HistoryScreen()
}
