package com.example.zv.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.zv.data.database.AppDatabase
import com.example.zv.data.database.ThreatEntity
import com.example.zv.data.database.ThreatSeverity
import com.example.zv.data.database.ThreatType
import com.example.zv.auth.AppUser
import com.example.zv.history.ThreatHistoryViewModel
import com.example.zv.history.ThreatHistoryViewModelFactory
import com.google.firebase.auth.FirebaseUser
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ThreatHistoryScreen(
    user: FirebaseUser?,
    appUser: AppUser?,
    context: android.content.Context,
    onBack: () -> Unit
) {
    // Проверка на гостя
    if (appUser?.isAnonymous == true) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Доступ ограничен",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text(
                text = "Для просмотра истории угроз необходимо войти в аккаунт",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            Text(
                text = "Войдите в аккаунт для доступа",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }
    
    val database = remember { AppDatabase.getDatabase(context) }
    val viewModel: ThreatHistoryViewModel = viewModel(
        factory = ThreatHistoryViewModelFactory(database)
    )
    
    val threats by viewModel.threats.collectAsState()
    val selectedThreats by viewModel.selectedThreats.collectAsState()
    var isSelectionMode by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "История",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                if (threats.isNotEmpty()) {
                    Text(
                        text = "${threats.size} ${if (threats.size == 1) "запись" else "записей"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (threats.isNotEmpty()) {
                    if (isSelectionMode) {
                        if (selectedThreats.isNotEmpty()) {
                            TextButton(
                                onClick = { viewModel.deleteSelected() }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Удалить (${selectedThreats.size})")
                            }
                        }
                        TextButton(onClick = { 
                            isSelectionMode = false
                            viewModel.clearSelection()
                        }) {
                            Text("Отмена")
                        }
                    } else {
                        TextButton(onClick = { isSelectionMode = true }) {
                            Text("Выбрать")
                        }
                        TextButton(onClick = { viewModel.clearHistory() }) {
                            Text("Очистить")
                        }
                    }
                }
            }
        }
        
        if (threats.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "История пуста",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Угрозы, обнаруженные сканером,\nбудут отображаться здесь",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            if (isSelectionMode && threats.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { viewModel.selectAll(threats.map { it.id }) }
                    ) {
                        Text("Выбрать все")
                    }
                    Text(
                        text = "Выбрано: ${selectedThreats.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(threats) { threat ->
                    ThreatItem(
                        threat = threat,
                        isSelected = selectedThreats.contains(threat.id),
                        isSelectionMode = isSelectionMode,
                        onToggleSelection = { viewModel.toggleSelection(threat.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun ThreatItem(
    threat: ThreatEntity,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onToggleSelection: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    val dateString = dateFormat.format(Date(threat.detectedAt))
    
    // Получаем имя файла из пути или используем сохраненное имя
    val displayName = when {
        threat.type == ThreatType.FILE -> {
            val fileName = threat.name
            if (fileName.matches(Regex("^\\d+$")) || 
                fileName.startsWith("image:") || 
                fileName.startsWith("temp_") ||
                fileName.contains("/cache/")) {
                threat.hash?.take(16)?.let { "Файл (${it}...)" } ?: "Сканированный файл"
            } else {
                try {
                    File(fileName).name.takeIf { it.isNotBlank() && it != fileName } ?: fileName
                } catch (e: Exception) {
                    fileName
                }
            }
        }
        threat.type == ThreatType.URL -> {
            val url = threat.url ?: threat.name
            try {
                if (url.length > 50) {
                    url.take(47) + "..."
                } else {
                    url
                }
            } catch (e: Exception) {
                url
            }
        }
        else -> threat.name
    }
    
    val icon = when (threat.type) {
        ThreatType.FILE -> Icons.Default.Phone
        ThreatType.URL -> Icons.Default.Settings
        ThreatType.UNKNOWN -> Icons.Default.Info
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isSelectionMode) {
                    Modifier.clickable(onClick = onToggleSelection)
                } else {
                    Modifier
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                threat.severity == ThreatSeverity.HIGH -> MaterialTheme.colorScheme.errorContainer
                threat.severity == ThreatSeverity.MEDIUM -> MaterialTheme.colorScheme.tertiaryContainer
                threat.severity == ThreatSeverity.LOW -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelection() },
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = when (threat.severity) {
                    ThreatSeverity.HIGH -> MaterialTheme.colorScheme.error
                    ThreatSeverity.MEDIUM -> MaterialTheme.colorScheme.tertiary
                    ThreatSeverity.LOW -> MaterialTheme.colorScheme.secondary
                    ThreatSeverity.SAFE -> MaterialTheme.colorScheme.primary
                }
            )
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = getSeverityBadge(threat.severity),
                        style = MaterialTheme.typography.labelSmall,
                        color = when (threat.severity) {
                            ThreatSeverity.HIGH -> MaterialTheme.colorScheme.onErrorContainer
                            ThreatSeverity.MEDIUM -> MaterialTheme.colorScheme.onTertiaryContainer
                            ThreatSeverity.LOW -> MaterialTheme.colorScheme.onSecondaryContainer
                            ThreatSeverity.SAFE -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Тип: ${getTypeText(threat.type)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Источник: ${threat.source}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = dateString,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (!threat.details.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = threat.details,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun getSeverityBadge(severity: ThreatSeverity): String {
    return when (severity) {
        ThreatSeverity.HIGH -> "🔴 Высокий"
        ThreatSeverity.MEDIUM -> "🟡 Средний"
        ThreatSeverity.LOW -> "🟠 Низкий"
        ThreatSeverity.SAFE -> "🟢 Безопасно"
    }
}

private fun getTypeText(type: ThreatType): String {
    return when (type) {
        ThreatType.FILE -> "Файл"
        ThreatType.URL -> "URL"
        ThreatType.UNKNOWN -> "Неизвестно"
    }
}
