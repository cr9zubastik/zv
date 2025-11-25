package com.example.zv.ui.screens

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.zv.data.database.ThreatSeverity
import com.example.zv.scanner.ScanState
import com.example.zv.scanner.ScannerViewModel
import com.example.zv.scanner.ScannerViewModelFactory
import java.io.File

@Composable
fun ScannerScreen(
    scannerViewModel: ScannerViewModel = viewModel(
        factory = ScannerViewModelFactory(LocalContext.current)
    )
) {
    val context = LocalContext.current
    val scanState by scannerViewModel.scanState.collectAsState()
    
    var filePath by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var scanType by remember { mutableStateOf<ScanType>(ScanType.FILE) }
    
    // Храним оригинальное имя файла
    var originalFileName by remember { mutableStateOf<String?>(null) }
    
    // Очищаем выбранный файл после завершения сканирования
    LaunchedEffect(scanState) {
        if (scanState !is ScanState.Idle && scanState !is ScanState.Scanning) {
            // Очищаем файл после завершения сканирования
            filePath = ""
            originalFileName = null
        }
    }
    
    // Файловый пикер через Storage Access Framework не требует разрешений
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            // Получаем оригинальное имя файла из URI
            originalFileName = try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0 && cursor.moveToFirst()) {
                        cursor.getString(nameIndex)
                    } else {
                        uri.lastPathSegment
                    }
                }
            } catch (e: Exception) {
                uri.lastPathSegment
            }
            
            val path = getPathFromUri(context, it, originalFileName)
            if (path != null) {
                filePath = path
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Заголовок с градиентом
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Сканер безопасности",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Проверка файлов и ссылок на угрозы",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
        
        // Выбор типа сканирования
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ScanTypeChip(
                    selected = scanType == ScanType.FILE,
                    onClick = { 
                        scanType = ScanType.FILE
                        filePath = "" // Сбрасываем при смене типа
                    },
                    label = "Файл",
                    icon = Icons.Default.Phone,
                    modifier = Modifier.weight(1f)
                )
                ScanTypeChip(
                    selected = scanType == ScanType.URL,
                    onClick = { 
                        scanType = ScanType.URL
                        url = "" // Сбрасываем при смене типа
                    },
                    label = "URL",
                    icon = Icons.Default.Settings,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        // Контент в зависимости от типа
        AnimatedContent(
            targetState = scanType,
            transitionSpec = {
                fadeIn() + slideInHorizontally() togetherWith fadeOut() + slideOutHorizontally()
            },
            label = "scan_type_animation"
        ) { type ->
            when (type) {
                ScanType.FILE -> {
                    FileScanContent(
                        filePath = filePath,
                        onSelectFile = { filePickerLauncher.launch("*/*") },
                        onClearFile = { filePath = "" }
                    )
                }
                ScanType.URL -> {
                    UrlScanContent(
                        url = url,
                        onUrlChange = { url = it }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Кнопка сканирования
        Button(
            onClick = {
                when (scanType) {
                    ScanType.FILE -> {
                        if (filePath.isNotBlank()) {
                            scannerViewModel.scanFile(filePath, originalFileName)
                        }
                    }
                    ScanType.URL -> {
                        if (url.isNotBlank()) {
                            scannerViewModel.scanUrl(url)
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = when (scanType) {
                ScanType.FILE -> filePath.isNotBlank() && scanState !is ScanState.Scanning
                ScanType.URL -> url.isNotBlank() && scanState !is ScanState.Scanning
            },
            shape = RoundedCornerShape(12.dp)
        ) {
            if (scanState is ScanState.Scanning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("Сканирование...")
            } else {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Начать сканирование", fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Результаты сканирования
        AnimatedVisibility(
            visible = scanState !is ScanState.Idle,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            when (val state = scanState) {
                is ScanState.Scanning -> {
                    ScanProgressCard(message = state.message)
                }
                is ScanState.ThreatFound -> {
                    ThreatFoundCard(
                        threatName = state.threatName,
                        severity = state.severity,
                        details = state.details,
                        onScanAgain = { 
                            scannerViewModel.resetState()
                            filePath = ""
                            originalFileName = null
                        }
                    )
                }
                is ScanState.Safe -> {
                    SafeCard(
                        message = state.message,
                        onScanAgain = { 
                            scannerViewModel.resetState()
                            filePath = ""
                            originalFileName = null
                        }
                    )
                }
                is ScanState.Error -> {
                    ErrorCard(
                        message = state.message,
                        onScanAgain = { 
                            scannerViewModel.resetState()
                            filePath = ""
                            originalFileName = null
                        }
                    )
                }
                else -> {}
            }
        }
    }
}

@Composable
fun ScanTypeChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
fun FileScanContent(
    filePath: String,
    onSelectFile: () -> Unit,
    onClearFile: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (filePath.isNotBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Выбранный файл",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = try {
                                File(filePath).name
                            } catch (e: Exception) {
                                filePath
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2
                        )
                    }
                    IconButton(onClick = onClearFile) {
                        Icon(
                            Icons.Default.Close, 
                            contentDescription = "Удалить",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        OutlinedButton(
            onClick = onSelectFile,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Settings, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (filePath.isBlank()) "Выбрать файл" else "Изменить файл")
        }
    }
}

@Composable
fun UrlScanContent(
    url: String,
    onUrlChange: (String) -> Unit
) {
    OutlinedTextField(
        value = url,
        onValueChange = onUrlChange,
        label = { Text("URL для сканирования") },
        placeholder = { Text("https://example.com") },
        leadingIcon = {
            Icon(Icons.Default.Settings, contentDescription = null)
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        singleLine = true
    )
}

@Composable
fun ScanProgressCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                strokeWidth = 3.dp
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ThreatFoundCard(
    threatName: String,
    severity: ThreatSeverity,
    details: String,
    onScanAgain: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (severity) {
                ThreatSeverity.HIGH -> MaterialTheme.colorScheme.errorContainer
                ThreatSeverity.MEDIUM -> MaterialTheme.colorScheme.tertiaryContainer
                ThreatSeverity.LOW -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Угроза обнаружена!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            InfoRow("Название", threatName)
            Spacer(modifier = Modifier.height(8.dp))
            InfoRow("Уровень", getSeverityText(severity))
            if (details.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onScanAgain,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Сканировать снова")
            }
        }
    }
}

@Composable
fun SafeCard(
    message: String,
    onScanAgain: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Безопасно",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onScanAgain,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Сканировать снова")
            }
        }
    }
}

@Composable
fun ErrorCard(
    message: String,
    onScanAgain: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Ошибка",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onScanAgain,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Попробовать снова")
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun getPathFromUri(context: Context, uri: android.net.Uri, originalFileName: String? = null): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val fileName = originalFileName ?: uri.lastPathSegment ?: "temp_scan_file"
        // Очищаем имя файла от недопустимых символов
        val safeFileName = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val file = File(context.cacheDir, safeFileName)
        inputStream?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        file.absolutePath
    } catch (e: Exception) {
        null
    }
}

private fun getSeverityText(severity: ThreatSeverity): String {
    return when (severity) {
        ThreatSeverity.HIGH -> "Высокий"
        ThreatSeverity.MEDIUM -> "Средний"
        ThreatSeverity.LOW -> "Низкий"
        ThreatSeverity.SAFE -> "Безопасно"
    }
}

private enum class ScanType {
    FILE, URL
}
