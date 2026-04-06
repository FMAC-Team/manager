package me.nekosu.aqnya.ui.screens

import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.nekosu.aqnya.ncore
import me.nekosu.aqnya.util.DebugPreferences
import me.nekosu.aqnya.util.RootDbHelper
import me.nekosu.aqnya.util.RuleDbHelper
import me.nekosu.aqnya.util.getAppVersion

enum class InstallStatus {
    CHECKING,
    INSTALLED,
    NOT_INSTALLED,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToApps: () -> Unit = {},
    onNavigateToRules: () -> Unit = {},
) {
    val context = LocalContext.current
    var showInstallSheet by remember { mutableStateOf(false) }
    var installStatus by remember { mutableStateOf(InstallStatus.CHECKING) }
    var suCount by remember { mutableIntStateOf(0) }
    var ruleCount by remember { mutableIntStateOf(0) }
    val showRules by DebugPreferences.showRulesFlow(context).collectAsState(initial = false)

    LaunchedEffect(Unit) {
        installStatus =
            withContext(Dispatchers.IO) {
                val result = ncore().ctl(1)
                if (result == 0) {
                    ncore().ctl(3)
                    InstallStatus.INSTALLED
                } else {
                    InstallStatus.NOT_INSTALLED
                }
            }
    }

    LaunchedEffect(installStatus) {
        if (installStatus == InstallStatus.INSTALLED) {
            withContext(Dispatchers.IO) {
                val rootDb = RootDbHelper(context)
                val ruleDb = RuleDbHelper(context)
                suCount = rootDb.getAllowedCount()
                ruleCount = ruleDb.getCount()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "NekoSU",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
                colors =
                    TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .padding(bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StatusCard(
                status = installStatus,
                onClick = {
                    if (installStatus != InstallStatus.INSTALLED) {
                        showInstallSheet = true
                    } else {
                        Toast.makeText(context, "服务运行正常", Toast.LENGTH_SHORT).show()
                    }
                },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatCard(
                    label = "超级用户",
                    value = if (installStatus == InstallStatus.CHECKING) "—" else suCount.toString(),
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToApps,
                )
                if (showRules) {
                    StatCard(
                        label = "FMAC 规则",
                        value = if (installStatus == InstallStatus.CHECKING) "—" else ruleCount.toString(),
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToRules,
                    )
                }
            }
            DeviceInfoCard(modifier = Modifier.fillMaxWidth())
        }
    }

    if (showInstallSheet) {
        ncore().helloLog()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.65f),
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            )
        }
    }
}

@Composable
fun StatusCard(
    status: InstallStatus,
    opacity: Float = 0.06f,
    onClick: () -> Unit,
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val notInstalledContainerColor = if (isDark) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.error
    }

    val notInstalledContentColor = if (isDark) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onError
    }

    val containerColor =
        when (status) {
            InstallStatus.INSTALLED -> MaterialTheme.colorScheme.primaryContainer
            InstallStatus.NOT_INSTALLED -> notInstalledContainerColor
            InstallStatus.CHECKING -> MaterialTheme.colorScheme.surfaceVariant
        }
    val contentColor =
        when (status) {
            InstallStatus.INSTALLED -> MaterialTheme.colorScheme.onPrimaryContainer
            InstallStatus.NOT_INSTALLED -> notInstalledContentColor
            InstallStatus.CHECKING -> MaterialTheme.colorScheme.onSurfaceVariant
        }
    val iconVector =
        when (status) {
            InstallStatus.INSTALLED -> Icons.Filled.CheckCircle
            InstallStatus.NOT_INSTALLED -> Icons.Filled.SystemUpdate
            InstallStatus.CHECKING -> Icons.Filled.Refresh
        }
    val titleText =
        when (status) {
            InstallStatus.INSTALLED -> "已安装"
            InstallStatus.NOT_INSTALLED -> "未安装"
            InstallStatus.CHECKING -> "检查中..."
        }
    val subText =
        when (status) {
            InstallStatus.INSTALLED -> "服务运行正常"
            InstallStatus.NOT_INSTALLED -> "点击安装"
            InstallStatus.CHECKING -> "正在验证服务状态"
        }

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp)),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = onClick,
            colors =
                CardDefaults.cardColors(
                    containerColor = containerColor.copy(alpha = 0.75f),
                ),
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(50.dp)
                            .background(
                                color = contentColor.copy(alpha = opacity),
                                shape = CircleShape,
                            )
                            .drawBehind{
                                val glowRadius = size.minDimension / 1.8f
                                drawCircle (
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            contentColor.copy(alpha = 0.45f),
                                            Color.Transparent ),
                                        center = center,
                                        radius = glowRadius
                                    ),
                                    radius = glowRadius
                                )
                            }
                        .background(
                            color = contentColor.copy(alpha = opacity),
                            shape = CircleShape,
                        ),
                                ) {
                                    if (status == InstallStatus.CHECKING) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = contentColor,
                                            strokeWidth = 2.5.dp,
                                        )
                                    } else {
                                        Icon(
                                            imageVector = iconVector,
                                            contentDescription = null,
                                            tint = contentColor,
                                            modifier = Modifier.size(24.dp),
                                        )
                                    }
                                }

                                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(
                                        text = titleText,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = contentColor,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        text = subText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = contentColor.copy(alpha = 0.7f),
                                    )
                                }

                                        Spacer(modifier = Modifier.weight(1f))

                                        Box(
                                        contentAlignment = Alignment.Center,
                                    modifier =
                                        Modifier
                                            .size(30.dp)
                                            .background(
                                                color = contentColor.copy(alpha = opacity),
                                                shape = CircleShape,
                                            )
                                            .drawBehind{
                                            val glowRadius = size.minDimension / 1.8f
                                            drawCircle (
                                                brush = Brush.radialGradient(
                                                    colors = listOf(
                                                        contentColor.copy(alpha = 0.35f),
                                                        Color.Transparent ),
                                                    center = center,
                                                    radius = glowRadius
                                                ),
                                                radius = glowRadius
                                            )
                                        }
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                        contentDescription = null,
                                        tint = contentColor.copy(alpha = 0.6f),
                                        modifier = Modifier.size(13.dp),
                                    )
                                }
                            }
            }
        }
    }


data class StatusConfig(
    val containerColor: androidx.compose.ui.graphics.Color,
    val contentColor: androidx.compose.ui.graphics.Color,
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
)

@Composable
fun DeviceInfoCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val appVersion = remember { getAppVersion(context) }

    val items =
        listOf(
            Triple(Icons.Filled.Memory, "内核版本", System.getProperty("os.version") ?: "Unavailable"),
            Triple(Icons.Filled.Android, "Android 版本", Build.VERSION.RELEASE),
            Triple(Icons.Filled.PhoneAndroid, "设备", "${Build.MANUFACTURER} ${Build.MODEL}"),
            Triple(Icons.Filled.Settings, "管理器版本", appVersion),
        )

    Box(
        modifier = modifier.clip(RoundedCornerShape(28.dp)),
    ) {
        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .background(
                        brush =
                            Brush.verticalGradient(
                                colors =
                                    listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                        Color.Transparent,
                                    ),
                            ),
                    ).blur(24.dp),
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f),
                ),
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                items.forEachIndexed { index, (icon, title, value) ->
                    DeviceInfoItem(
                        icon = icon,
                        title = title,
                        value = value,
                    )
                    if (index < items.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceInfoItem(
    icon: ImageVector,
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .size(38.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(11.dp),
                    ),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    MaterialTheme {
        HomeScreen()
    }
}
