package me.nekosu.aqnya.ui.screens

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import me.nekosu.aqnya.ncore

const val FMAC_BIT_DENY = 0
const val FMAC_BIT_NOT_FOUND = 1

data class FmacRule(
    val path: String,
    val statusBits: Long,
)

class RuleDbHelper(context: Context) :
    SQLiteOpenHelper(context, "fmac_rules.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE rules (path TEXT PRIMARY KEY, status_bits INTEGER NOT NULL)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, old: Int, new: Int) {
        db.execSQL("DROP TABLE IF EXISTS rules")
        onCreate(db)
    }

    fun getAll(): List<FmacRule> {
        val list = mutableListOf<FmacRule>()
        readableDatabase.rawQuery("SELECT path, status_bits FROM rules", null).use { c ->
            while (c.moveToNext())
                list += FmacRule(c.getString(0), c.getLong(1))
        }
        return list
    }

    fun getCount(): Int =
        readableDatabase.rawQuery("SELECT COUNT(*) FROM rules", null)
            .use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }

    fun insert(rule: FmacRule) {
        val cv = ContentValues().apply {
            put("path", rule.path)
            put("status_bits", rule.statusBits)
        }
        writableDatabase.insertWithOnConflict(
            "rules", null, cv, SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun delete(path: String) {
        writableDatabase.delete("rules", "path = ?", arrayOf(path))
    }
}

class RulesViewModel(private val context: Context) : ViewModel() {

    private val db = RuleDbHelper(context)

    var rules by mutableStateOf<List<FmacRule>>(emptyList())
        private set
    var isLoaded by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun load() {
        viewModelScope.launch(Dispatchers.IO) {
            rules = db.getAll()
            isLoaded = true
        }
    }

  fun addRule(path: String, statusBits: Long, onDone: (Boolean) -> Unit) {
    viewModelScope.launch(Dispatchers.IO) {
        try {
            val rule = FmacRule(path.trim(), statusBits)
            ncore().addRule(rule.path, rule.statusBits)
            db.insert(rule)
            rules = db.getAll()
            withContext(Dispatchers.Main) { onDone(true) }
        } catch (e: Exception) {
            error = e.message
            withContext(Dispatchers.Main) { onDone(false) }
        }
    }
}

fun deleteRule(rule: FmacRule, onDone: (Boolean) -> Unit) {
    viewModelScope.launch(Dispatchers.IO) {
        try {
            ncore().delRule(rule.path)
            db.delete(rule.path)
            rules = db.getAll()
            withContext(Dispatchers.Main) { onDone(true) }
        } catch (e: Exception) {
            error = e.message
            withContext(Dispatchers.Main) { onDone(false) }
        }
    }
}
}

class RulesViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        RulesViewModel(context) as T
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesScreen() {
    val context = LocalContext.current.applicationContext
    val vm: RulesViewModel = viewModel(factory = RulesViewModelFactory(context))
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.load() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "FMAC 规则",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        if (vm.isLoaded) {
                            Text(
                                "${vm.rules.size} 条规则",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
            IconButton(onClick = { showAddDialog = true }) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "添加规则",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },

    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            when {
                !vm.isLoaded -> CircularProgressIndicator(strokeWidth = 3.dp)

                vm.rules.isEmpty() -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "暂无规则",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 88.dp),
                ) {
                    items(vm.rules, key = { it.path }) { rule ->
                        RuleItem(
                            rule = rule,
                            onDelete = {
                                vm.deleteRule(rule) { ok ->
                                    scope.launch {
                                        snackbar.showSnackbar(if (ok) "已删除" else "删除失败")
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddRuleDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { path, bits ->
                showAddDialog = false
                vm.addRule(path, bits) { ok ->
                    scope.launch {
                        snackbar.showSnackbar(if (ok) "规则已添加" else "添加失败")
                    }
                }
            },
        )
    }
}

@Composable
fun RuleItem(
    rule: FmacRule,
    onDelete: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val isDeny = (rule.statusBits shr FMAC_BIT_DENY) and 1L == 1L
    val accentColor = if (isDeny)
        MaterialTheme.colorScheme.error
    else
        MaterialTheme.colorScheme.primary

    val isDir = rule.path.endsWith("/")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        colors = CardDefaults.cardColors(
            containerColor = accentColor.copy(alpha = 0.06f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
            ) {
                Icon(
imageVector = if (isDir) Icons.Default.Folder else Icons.AutoMirrored.Filled.InsertDriveFile,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp),
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = rule.path,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    BitChip(
                        label = if (isDeny) "DENY" else "ALLOW",
                        color = accentColor,
                    )
                    Text(
                        text = "0x%x".format(rule.statusBits),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                }
            }

            IconButton(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onDelete()
            }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
            }
        }
    }
}

@Composable
private fun BitChip(label: String, color: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

@Composable
fun AddRuleDialog(
    onDismiss: () -> Unit,
    onConfirm: (path: String, statusBits: Long) -> Unit,
) {
    var path by remember { mutableStateOf("") }
    var deny by remember { mutableStateOf(true) }
    var pathError by remember { mutableStateOf(false) }

    fun computeBits() = if (deny) (1L shl FMAC_BIT_DENY) else 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = { Text("添加规则", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = path,
                    onValueChange = { path = it; pathError = false },
                    label = { Text("路径") },
                    placeholder = { Text("/data/local/tmp/") },
                    isError = pathError,
                    supportingText = if (pathError) ({ Text("路径不能为空") }) else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                )
                Text(
                    "权限位",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = deny,
                        onClick = { deny = !deny },
                        label = { Text("DENY") },
                        leadingIcon = if (deny) {
                            { Icon(Icons.Default.Block, null, Modifier.size(16.dp)) }
                        } else null,
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Text(
                        text = "status_bits = 0x%x".format(computeBits()),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (path.isBlank()) { pathError = true; return@Button }
                    onConfirm(path.trim(), computeBits())
                },
                shape = RoundedCornerShape(14.dp),
            ) { Text("添加") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
