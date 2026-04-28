package me.nekosu.aqnya.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun CapsDialog(
    current: Set<LinuxCap>,
    onDismiss: () -> Unit,
    onConfirm: (Set<LinuxCap>) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    var draft by remember { mutableStateOf(current) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Capabilities  ·  ${draft.size} / ${LinuxCap.entries.size}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                            draft = LinuxCap.entries.toSet()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                    ) { Text("全选", fontSize = 12.sp) }
                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                            draft = DEFAULT_CAPS
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                    ) { Text("默认", fontSize = 12.sp) }
                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                            draft = emptySet()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                    ) { Text("清空", fontSize = 12.sp) }
                }

                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(4.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    LinuxCap.entries.forEach { cap ->
                        val checked = draft.contains(cap)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    draft = if (checked) draft - cap else draft + cap
                                }
                                .padding(vertical = 4.dp, horizontal = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = checked, onCheckedChange = null)
                            Spacer(Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "CAP_${cap.label}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace,
                                )
                                Text(
                                    text = cap.description,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                )
                            }
                            Text(
                                text = cap.value.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                                modifier = Modifier.padding(start = 6.dp),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                onConfirm(draft)
            }) {
                Text("确定", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailScreen(
    app: AppInfo,
    config: AppConfig?,
    onSave: (AppConfig) -> Unit,
    onBack: () -> Unit,
    navController: NavController,
) {
    var allowed by remember { mutableStateOf(config?.allowed ?: false) }
    var caps by remember { mutableStateOf(config?.caps ?: DEFAULT_CAPS) }
    var domain by remember { mutableStateOf(config?.selinuxDomain ?: "u:r:nksu:s0") }
    var ns by remember { mutableStateOf(config?.namespace ?: NksuNamespace.INHERITED) }
    var showCapsDialog by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    if (showCapsDialog) {
        CapsDialog(
            current = caps,
            onDismiss = { showCapsDialog = false },
            onConfirm = { selected ->
                caps = selected
                showCapsDialog = false
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                title = {
                    Column {
                        Text(
                            text = app.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = app.packageName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                actions = {
                    TextButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                        onSave(
                            AppConfig(
                                allowed = allowed,
                                caps = caps,
                                selinuxDomain = domain,
                                namespace = ns,
                            )
                        )
                        onBack()
                    }) {
                        Text("保存", fontWeight = FontWeight.SemiBold)
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        AppIcon(
                            packageName = app.packageName,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(12.dp)),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = app.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "UID: ${app.uid}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            )
                        }
                        if (app.isSystem) {
                            AppTag(label = "system", color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            item {
                SectionLabel("Root 授权")
                Spacer(Modifier.height(4.dp))
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (allowed)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f)
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = if (allowed) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (allowed)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "允许 Root 访问",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                if (allowed) "已授权" else "已拒绝",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            )
                        }
                        Switch(
                            checked = allowed,
                            onCheckedChange = {
                                haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                                allowed = it
                                if (!it) {
                                    caps = emptySet()
                                    domain = "u:r:nksu:s0"
                                    ns = NksuNamespace.INHERITED
                                } else if (caps.isEmpty()) {
                                    caps = DEFAULT_CAPS
                                }
                            },
                            thumbContent = if (allowed) {
                                { Icon(Icons.Filled.CheckCircle, null, Modifier.size(SwitchDefaults.IconSize)) }
                            } else null,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            item {
                SectionLabel("Capabilities")
                Spacer(Modifier.height(4.dp))
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            !allowed -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f)
                            caps.isNotEmpty() -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.14f)
                            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)
                        },
                    ),
                    onClick = {
                        if (allowed) {
                            haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                            showCapsDialog = true
                        }
                    },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = if (allowed)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${caps.size} / ${LinuxCap.entries.size} 已选",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (allowed)
                                    MaterialTheme.colorScheme.onSurface
                                else
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                            )
                            if (allowed) {
                                Text(
                                    text = if (caps.isEmpty()) {
                                        "无 capabilities"
                                    } else {
                                        caps.take(4).joinToString(" · ") { it.label } +
                                            if (caps.size > 4) " +${caps.size - 4}" else ""
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            } else {
                                Text(
                                    text = "请先启用 Root 授权",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                )
                            }
                        }
                        if (allowed) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "编辑",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            item {
                SectionLabel("SELinux Domain")
                Spacer(Modifier.height(4.dp))
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (allowed && domain != "u:r:nksu:s0")
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.14f)
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    ) {
                        OutlinedTextField(
                            value = domain,
                            onValueChange = { domain = it },
                            label = {
                                Text(
                                    "domain",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                )
                            },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = allowed,
                            trailingIcon = {
                                if (domain != "u:r:nksu:s0") {
                                    IconButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                                            domain = "u:r:nksu:s0"
                                        },
                                    ) {
                                        Icon(
                                            Icons.Default.Refresh,
                                            contentDescription = "重置",
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                        )
                        if (!allowed) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "请先启用 Root 授权",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            item {
                SectionLabel("Mount 命名空间")
                Spacer(Modifier.height(4.dp))
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (allowed && ns != NksuNamespace.INHERITED)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.14f)
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
                    ),
                ) {
                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        NksuNamespace.entries.forEach { option ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable(
                                        enabled = allowed,
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                    ) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        ns = option
                                    }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = ns == option,
                                    onClick = null,
                                    enabled = allowed,
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = option.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (allowed)
                                            MaterialTheme.colorScheme.onSurface
                                        else
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                    )
                                    Text(
                                        text = option.description,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
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