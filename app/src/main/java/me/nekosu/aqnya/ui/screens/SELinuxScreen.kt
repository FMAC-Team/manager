package me.nekosu.aqnya.ui.screens

import android.content.Context
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import me.nekosu.aqnya.ui.theme.NekosuTheme

private val Context.selinuxDataStore: DataStore<Preferences> by preferencesDataStore("selinux_rules")
private val KEY_GROUPS = stringPreferencesKey("groups")

private val json = Json { ignoreUnknownKeys = true }

object AvTab {
    const val ALLOWED = 1
    const val AUDITALLOW = 2
    const val AUDITDENY = 8
    const val TRANSITION = 16
}

@Serializable
data class SelinuxRule(
    val id: Long = System.currentTimeMillis(),
    val src: String = "",
    val tgt: String = "",
    val cls: String = "",
    val perm: String = "",
    val effect: Int = AvTab.ALLOWED,
    val invert: Boolean = false,
)

@Serializable
data class SelinuxGroup(
    val id: Long = System.currentTimeMillis(),
    val name: String,
    val required: Boolean = true,
    val rules: List<SelinuxRule> = emptyList(),
    val expanded: Boolean = true,
)

private suspend fun Context.saveGroups(groups: List<SelinuxGroup>) {
    selinuxDataStore.edit { it[KEY_GROUPS] = json.encodeToString(groups) }
}

private fun Context.groupsFlow() =
    selinuxDataStore.data.map { prefs ->
        prefs[KEY_GROUPS]?.let {
            runCatching { json.decodeFromString<List<SelinuxGroup>>(it) }.getOrNull()
        } ?: emptyList()
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelinuxRulesPage(
    onAddRule: (SelinuxRule) -> Int,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val persistedGroups by context.groupsFlow().collectAsState(initial = emptyList())
    var groups by remember { mutableStateOf<List<SelinuxGroup>>(emptyList()) }

    // Sync from DataStore on first load
    LaunchedEffect(persistedGroups) {
        if (groups.isEmpty() && persistedGroups.isNotEmpty()) {
            groups = persistedGroups
        }
    }

    SelinuxRulesContent(
        groups = groups,
        onPersist = { g ->
            groups = g
            scope.launch { context.saveGroups(g) }
        },
        onAddRule = onAddRule,
        onBackClick = onBackClick,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelinuxRulesContent(
    groups: List<SelinuxGroup>,
    onPersist: (List<SelinuxGroup>) -> Unit,
    onAddRule: (SelinuxRule) -> Int,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackState = remember { SnackbarHostState() }
    var snackMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(snackMsg) {
        snackMsg?.let {
            snackState.showSnackbar(it)
            snackMsg = null
        }
    }

    // Dialog state
    var showGroupDialog by remember { mutableStateOf(false) }
    var editGroupTarget by remember { mutableStateOf<SelinuxGroup?>(null) }
    var showRuleDialog by remember { mutableStateOf(false) }
    var editRuleTarget by remember { mutableStateOf<SelinuxRule?>(null) }
    var ruleGroupId by remember { mutableStateOf<Long?>(null) }

    val totalRules = groups.sumOf { it.rules.size }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackState) },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Apply all
                if (totalRules > 0) {
                    SmallFloatingActionButton(
                        onClick = {
                            var ok = 0
                            var fail = 0
                            groups.forEach { g ->
                                g.rules.forEach { r ->
                                    if (onAddRule(r) == 0) ok++ else fail++
                                }
                            }
                            snackMsg = "Applied $ok rule(s)" + if (fail > 0) ", $fail failed" else ""
                        },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Apply all")
                    }
                }
                ExtendedFloatingActionButton(
                    onClick = {
                        editGroupTarget = null
                        showGroupDialog = true
                    },
                    icon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null) },
                    text = { Text("New Group") },
                )
            }
        },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            "SELinux Rules",
                            style =
                                MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                ),
                        )
                        Text(
                            "${groups.size} group(s) · $totalRules rule(s)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    if (groups.isNotEmpty()) {
                        IconButton(onClick = { onPersist(emptyList()) }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear all")
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (groups.isEmpty()) {
            EmptyState(Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(groups, key = { it.id }) { group ->
                    GroupCard(
                        group = group,
                        onToggleExpand = {
                            onPersist(
                                groups.map {
                                    if (it.id == group.id) it.copy(expanded = !it.expanded) else it
                                },
                            )
                        },
                        onEditGroup = {
                            editGroupTarget = group
                            showGroupDialog = true
                        },
                        onDeleteGroup = { onPersist(groups.filter { it.id != group.id }) },
                        onApplyGroup = {
                            var ok = 0
                            var fail = 0
                            group.rules.forEach { r ->
                                if (onAddRule(r) == 0) ok++ else fail++
                            }
                            snackMsg = "[${group.name}] Applied $ok" +
                                if (fail > 0) ", $fail failed" else ""
                        },
                        onAddRule = {
                            ruleGroupId = group.id
                            editRuleTarget = null
                            showRuleDialog = true
                        },
                        onEditRule = { rule ->
                            ruleGroupId = group.id
                            editRuleTarget = rule
                            showRuleDialog = true
                        },
                        onDeleteRule = { rule ->
                            onPersist(
                                groups.map {
                                    if (it.id == group.id) {
                                        it.copy(rules = it.rules.filter { r -> r.id != rule.id })
                                    } else {
                                        it
                                    }
                                },
                            )
                        },
                        onApplyRule = { rule ->
                            val ret = onAddRule(rule)
                            snackMsg = if (ret == 0) "Rule applied" else "Failed (ret=$ret)"
                        },
                    )
                }
                item { Spacer(Modifier.height(100.dp)) }
            }
        }
    }

    if (showGroupDialog) {
        GroupEditorDialog(
            initial = editGroupTarget,
            onDismiss = { showGroupDialog = false },
            onConfirm = { name, required ->
                onPersist(
                    if (editGroupTarget != null) {
                        groups.map {
                            if (it.id == editGroupTarget!!.id) {
                                it.copy(name = name, required = required)
                            } else {
                                it
                            }
                        }
                    } else {
                        groups + SelinuxGroup(name = name, required = required)
                    },
                )
                showGroupDialog = false
            },
        )
    }

    if (showRuleDialog) {
        RuleEditorDialog(
            initial = editRuleTarget,
            onDismiss = { showRuleDialog = false },
            onConfirm = { rule ->
                val gid = ruleGroupId ?: return@RuleEditorDialog
                onPersist(
                    groups.map { g ->
                        if (g.id != gid) {
                            g
                        } else if (editRuleTarget != null) {
                            g.copy(
                                rules =
                                    g.rules.map {
                                        if (it.id == editRuleTarget!!.id) rule.copy(id = it.id) else it
                                    },
                            )
                        } else {
                            g.copy(rules = g.rules + rule)
                        }
                    },
                )
                showRuleDialog = false
            },
        )
    }
}

@Composable
private fun GroupCard(
    group: SelinuxGroup,
    onToggleExpand: () -> Unit,
    onEditGroup: () -> Unit,
    onDeleteGroup: () -> Unit,
    onApplyGroup: () -> Unit,
    onAddRule: () -> Unit,
    onEditRule: (SelinuxRule) -> Unit,
    onDeleteRule: (SelinuxRule) -> Unit,
    onApplyRule: (SelinuxRule) -> Unit,
) {
    val requiredColor = if (group.required) Color(0xFF1565C0) else MaterialTheme.colorScheme.outline

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .animateContentSize(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                if (group.expanded) Icons.Default.FolderOpen else Icons.Default.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    group.name,
                    style =
                        MaterialTheme.typography.titleSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                        ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(3.dp),
                        color = requiredColor.copy(alpha = 0.12f),
                    ) {
                        Text(
                            if (group.required) "required" else "optional",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                            style =
                                MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                ),
                            color = requiredColor,
                        )
                    }
                    Text(
                        "${group.rules.size} rule(s)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = onApplyGroup, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Apply group",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
            IconButton(onClick = onAddRule, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add rule",
                    modifier = Modifier.size(18.dp),
                )
            }
            IconButton(onClick = onEditGroup, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit group",
                    modifier = Modifier.size(18.dp),
                )
            }
            IconButton(onClick = onDeleteGroup, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete group",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 4.dp))

        if (group.expanded) {
            if (group.rules.isEmpty()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "No rules — tap + to add",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            } else {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    group.rules.forEach { rule ->
                        RuleCard(
                            rule = rule,
                            onEdit = { onEditRule(rule) },
                            onDelete = { onDeleteRule(rule) },
                            onApply = { onApplyRule(rule) },
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun RuleCard(
    rule: SelinuxRule,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onApply: () -> Unit,
) {
    val effectColor =
        when (rule.effect) {
            AvTab.ALLOWED -> if (rule.invert) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
            AvTab.AUDITDENY -> MaterialTheme.colorScheme.error
            AvTab.AUDITALLOW -> Color(0xFF1565C0)
            else -> MaterialTheme.colorScheme.outline
        }
    val effectLabel =
        when (rule.effect) {
            AvTab.ALLOWED -> if (rule.invert) "DENY" else "ALLOW"
            AvTab.AUDITDENY -> "AUDITDENY"
            AvTab.AUDITALLOW -> "AUDITALLOW"
            AvTab.TRANSITION -> "TRANSITION"
            else -> "effect=${rule.effect}"
        }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(4.dp), color = effectColor.copy(alpha = 0.15f)) {
                    Text(
                        effectLabel,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = effectColor,
                        style =
                            MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                            ),
                    )
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onApply, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Apply",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        modifier = Modifier.size(16.dp),
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            RuleRow("src", rule.src)
            RuleRow("tgt", rule.tgt)
            RuleRow("cls", rule.cls)
            RuleRow("perm", rule.perm)
        }
    }
}

@Composable
private fun RuleRow(
    label: String,
    value: String,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            label,
            modifier = Modifier.width(36.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value.ifBlank { "*" },
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color =
                if (value.isBlank()) {
                    MaterialTheme.colorScheme.outline
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                Icons.Outlined.FolderOff,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.outlineVariant,
            )
            Text(
                "No groups",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Tap \"New Group\" to get started",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun GroupEditorDialog(
    initial: SelinuxGroup?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, required: Boolean) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var required by remember { mutableStateOf(initial?.required ?: true) }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    if (initial == null) "New Group" else "Edit Group",
                    style =
                        MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                        ),
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Group name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle =
                        MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                        ),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { required = !required }
                            .padding(horizontal = 4.dp, vertical = 8.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Required", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "加载失败时中止",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = required, onCheckedChange = { required = it })
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(
                        onClick = { if (name.isNotBlank()) onConfirm(name.trim(), required) },
                        enabled = name.isNotBlank(),
                    ) { Text("Save") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RuleEditorDialog(
    initial: SelinuxRule?,
    onDismiss: () -> Unit,
    onConfirm: (SelinuxRule) -> Unit,
) {
    var src by remember { mutableStateOf(initial?.src ?: "") }
    var tgt by remember { mutableStateOf(initial?.tgt ?: "") }
    var cls by remember { mutableStateOf(initial?.cls ?: "") }
    var perm by remember { mutableStateOf(initial?.perm ?: "") }
    var effect by remember { mutableStateOf(initial?.effect ?: AvTab.ALLOWED) }
    var invert by remember { mutableStateOf(initial?.invert ?: false) }
    var effectExpanded by remember { mutableStateOf(false) }

    val effectOptions =
        listOf(
            AvTab.ALLOWED to "ALLOW",
            AvTab.AUDITALLOW to "AUDITALLOW",
            AvTab.AUDITDENY to "AUDITDENY",
            AvTab.TRANSITION to "TRANSITION",
        )
    val mono = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth().wrapContentHeight(),
        ) {
            Column(
                Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    if (initial == null) "Add Rule" else "Edit Rule",
                    style =
                        MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                        ),
                )

                val fm = Modifier.fillMaxWidth()
                RuleTextField(fm, "src (empty = *)", src, mono) { src = it }
                RuleTextField(fm, "tgt (empty = *)", tgt, mono) { tgt = it }
                RuleTextField(fm, "cls (empty = *)", cls, mono) { cls = it }
                RuleTextField(fm, "perm (empty = *)", perm, mono) { perm = it }

                ExposedDropdownMenuBox(
                    expanded = effectExpanded,
                    onExpandedChange = { effectExpanded = it },
                ) {
                    OutlinedTextField(
                        value = effectOptions.find { it.first == effect }?.second ?: "ALLOW",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Effect") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(effectExpanded) },
                        modifier = fm.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
                        textStyle = mono,
                    )
                    ExposedDropdownMenu(
                        expanded = effectExpanded,
                        onDismissRequest = { effectExpanded = false },
                    ) {
                        effectOptions.forEach { (v, label) ->
                            DropdownMenuItem(
                                text = { Text(label, fontFamily = FontFamily.Monospace) },
                                onClick = {
                                    effect = v
                                    effectExpanded = false
                                },
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { invert = !invert }
                            .padding(horizontal = 4.dp, vertical = 8.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Invert", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "ALLOW → DENY",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = invert, onCheckedChange = { invert = it })
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(onClick = {
                        onConfirm(
                            SelinuxRule(
                                src = src.trim(),
                                tgt = tgt.trim(),
                                cls = cls.trim(),
                                perm = perm.trim(),
                                effect = effect,
                                invert = invert,
                            ),
                        )
                    }) { Text("Save") }
                }
            }
        }
    }
}

@Composable
private fun RuleTextField(
    modifier: Modifier,
    label: String,
    value: String,
    textStyle: androidx.compose.ui.text.TextStyle,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier,
        textStyle = textStyle,
        singleLine = true,
    )
}

private val sampleRules =
    listOf(
        SelinuxRule(src = "untrusted_app", tgt = "sysfs", cls = "file", perm = "read", effect = AvTab.ALLOWED),
        SelinuxRule(src = "shell", tgt = "dalvikcache_data_file", cls = "file", perm = "write", effect = AvTab.AUDITALLOW),
        SelinuxRule(src = "system_app", tgt = "selinuxfs", cls = "file", perm = "write", effect = AvTab.ALLOWED, invert = true),
    )

private val sampleGroups =
    listOf(
        SelinuxGroup(id = 1L, name = "Common Apps", rules = sampleRules.take(2), expanded = true),
        SelinuxGroup(id = 2L, name = "System Rules", rules = sampleRules.drop(2), expanded = false),
        SelinuxGroup(id = 3L, name = "Empty Group", rules = emptyList(), expanded = true),
    )

@Preview(showBackground = true)
@Composable
fun SelinuxRulesPagePreview() {
    NekosuTheme {
        SelinuxRulesContent(
            groups = sampleGroups,
            onPersist = {},
            onBackClick = {},
            onAddRule = { 0 },
        )
    }
}

@Preview(showBackground = true)
@Composable
fun EmptyStatePreview() {
    NekosuTheme {
        SelinuxRulesContent(
            groups = emptyList(),
            onPersist = {},
            onBackClick = {},
            onAddRule = { 0 },
        )
    }
}
