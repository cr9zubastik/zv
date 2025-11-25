package com.example.zv.ui.screens

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.io.File

@Composable
fun DeviceInfoScreen() {
    val deviceInfoItems = remember {
        listOf(
            DeviceInfoItem(
                title = "Модель устройства",
                value = Build.MODEL,
                icon = Icons.Default.Phone
            ),
            DeviceInfoItem(
                title = "Производитель",
                value = Build.MANUFACTURER.replaceFirstChar { it.uppercaseChar() },
                icon = Icons.Default.Settings
            ),
            DeviceInfoItem(
                title = "Версия Android",
                value = Build.VERSION.RELEASE,
                icon = Icons.Default.Info
            ),
            DeviceInfoItem(
                title = "SDK версия",
                value = Build.VERSION.SDK_INT.toString(),
                icon = Icons.Default.List
            ),
            DeviceInfoItem(
                title = "Архитектура",
                value = Build.SUPPORTED_ABIS.joinToString(", "),
                icon = Icons.Default.Settings
            ),
            DeviceInfoItem(
                title = "Root доступ",
                value = if (isRooted()) "Доступен" else "Недоступен",
                icon = Icons.Default.Warning,
                isWarning = isRooted()
            ),
            DeviceInfoItem(
                title = "Общий объем памяти",
                value = getTotalMemory(),
                icon = Icons.Default.Info
            ),
            DeviceInfoItem(
                title = "Свободная память",
                value = getAvailableMemory(),
                icon = Icons.Default.Info
            )
        )
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text(
            text = "Устройство",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Информация о вашем устройстве",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(deviceInfoItems) { item ->
                InfoCard(
                    title = item.title,
                    value = item.value,
                    icon = item.icon,
                    isWarning = item.isWarning
                )
            }
        }
    }
}

data class DeviceInfoItem(
    val title: String,
    val value: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val isWarning: Boolean = false
)

@Composable
fun InfoCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isWarning: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isWarning) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (isWarning) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private fun isRooted(): Boolean {
    val paths = arrayOf(
        "/system/app/Superuser.apk",
        "/sbin/su",
        "/system/bin/su",
        "/system/xbin/su",
        "/data/local/xbin/su",
        "/data/local/bin/su",
        "/system/sd/xbin/su",
        "/system/bin/failsafe/su",
        "/data/local/su",
        "/su/bin/su"
    )
    
    for (path in paths) {
        if (File(path).exists()) {
            return true
        }
    }
    
    return false
}

private fun getTotalMemory(): String {
    return try {
        val memInfo = android.os.Debug.MemoryInfo()
        android.os.Debug.getMemoryInfo(memInfo)
        val totalMemory = Runtime.getRuntime().maxMemory()
        formatBytes(totalMemory)
    } catch (e: Exception) {
        "Недоступно"
    }
}

private fun getAvailableMemory(): String {
    return try {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val availableMemory = runtime.maxMemory() - usedMemory
        formatBytes(availableMemory)
    } catch (e: Exception) {
        "Недоступно"
    }
}

private fun formatBytes(bytes: Long): String {
    val kb = bytes / 1024
    val mb = kb / 1024
    val gb = mb / 1024.0
    
    return when {
        gb >= 1 -> String.format("%.2f GB", gb)
        mb >= 1 -> String.format("%.2f MB", mb.toDouble())
        kb >= 1 -> String.format("%.2f KB", kb.toDouble())
        else -> "$bytes B"
    }
}
