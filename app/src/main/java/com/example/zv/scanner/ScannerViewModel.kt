package com.example.zv.scanner

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zv.data.api.ApiClient
import com.example.zv.data.api.ApiKeys
import com.example.zv.data.database.AppDatabase
import com.example.zv.data.database.ThreatEntity
import com.example.zv.data.database.ThreatSeverity
import com.example.zv.data.database.ThreatType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.security.MessageDigest

class ScannerViewModel(context: Context) : ViewModel() {
    private val database = AppDatabase.getDatabase(context)
    private val threatDao = database.threatDao()
    
    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()
    
    fun scanFile(filePath: String, originalFileName: String? = null) {
        viewModelScope.launch {
            try {
                _scanState.value = ScanState.Scanning("Сканирование файла...")
                
                val file = File(filePath)
                if (!file.exists()) {
                    _scanState.value = ScanState.Error("Файл не найден")
                    return@launch
                }
                
                // Вычисляем хеш файла
                val hash = calculateFileHash(file)
                
                // Проверяем через OTX
                val otxResult = checkOtx(hash)
                
                // Проверяем через VirusTotal
                val vtResult = checkVirusTotal(hash)
                
                // Определяем результат
                val isThreat = otxResult.isThreat || vtResult.isThreat
                val severity = when {
                    otxResult.isThreat || (vtResult.positives ?: 0) > 10 -> ThreatSeverity.HIGH
                    (vtResult.positives ?: 0) > 5 -> ThreatSeverity.MEDIUM
                    (vtResult.positives ?: 0) > 0 -> ThreatSeverity.LOW
                    else -> ThreatSeverity.SAFE
                }
                
                val threatDetails = buildString {
                    if (otxResult.isThreat) {
                        append("OTX: Обнаружена угроза\n")
                    } else {
                        append("OTX: Угроз не обнаружено\n")
                    }
                    if (vtResult.positives != null && vtResult.total != null) {
                        if (vtResult.positives > 0) {
                            append("VirusTotal: ${vtResult.positives}/${vtResult.total} антивирусов обнаружили угрозу\n")
                        } else {
                            append("VirusTotal: ${vtResult.total} антивирусов проверили, угроз не обнаружено\n")
                        }
                    }
                }
                
                // Используем оригинальное имя файла или имя из пути
                val displayName = originalFileName ?: file.name
                
                // Сохраняем все сканы в базу данных
                val threat = ThreatEntity(
                    name = displayName,
                    type = ThreatType.FILE,
                    severity = severity,
                    source = when {
                        otxResult.isThreat -> "OTX"
                        vtResult.positives != null && vtResult.positives > 0 -> "VirusTotal"
                        else -> "OTX + VirusTotal"
                    },
                    details = threatDetails,
                    hash = hash
                )
                threatDao.insertThreat(threat)
                
                if (isThreat) {
                    _scanState.value = ScanState.ThreatFound(
                        threatName = displayName,
                        severity = severity,
                        details = threatDetails
                    )
                } else {
                    _scanState.value = ScanState.Safe("Файл безопасен")
                }
                
            } catch (e: Exception) {
                _scanState.value = ScanState.Error("Ошибка сканирования: ${e.message}")
            }
        }
    }
    
    fun scanUrl(url: String) {
        viewModelScope.launch {
            try {
                _scanState.value = ScanState.Scanning("Сканирование URL...")
                
                // Проверяем через OTX
                val otxResult = checkOtxUrl(url)
                
                // Проверяем через VirusTotal
                val vtResult = checkVirusTotalUrl(url)
                
                val isThreat = otxResult.isThreat || vtResult.isThreat
                val severity = when {
                    otxResult.isThreat || (vtResult.positives ?: 0) > 10 -> ThreatSeverity.HIGH
                    (vtResult.positives ?: 0) > 5 -> ThreatSeverity.MEDIUM
                    (vtResult.positives ?: 0) > 0 -> ThreatSeverity.LOW
                    else -> ThreatSeverity.SAFE
                }
                
                val threatDetails = buildString {
                    if (otxResult.isThreat) {
                        append("OTX: Обнаружена угроза\n")
                    } else {
                        append("OTX: Угроз не обнаружено\n")
                    }
                    if (vtResult.positives != null && vtResult.total != null) {
                        if (vtResult.positives > 0) {
                            append("VirusTotal: ${vtResult.positives}/${vtResult.total} антивирусов обнаружили угрозу\n")
                        } else {
                            append("VirusTotal: ${vtResult.total} антивирусов проверили, угроз не обнаружено\n")
                        }
                    }
                }
                
                // Сохраняем все сканы в базу данных
                val threat = ThreatEntity(
                    name = url,
                    type = ThreatType.URL,
                    severity = severity,
                    source = when {
                        otxResult.isThreat -> "OTX"
                        vtResult.positives != null && vtResult.positives > 0 -> "VirusTotal"
                        else -> "OTX + VirusTotal"
                    },
                    details = threatDetails,
                    url = url
                )
                threatDao.insertThreat(threat)
                
                if (isThreat) {
                    _scanState.value = ScanState.ThreatFound(
                        threatName = url,
                        severity = severity,
                        details = threatDetails
                    )
                } else {
                    _scanState.value = ScanState.Safe("URL безопасен")
                }
                
            } catch (e: Exception) {
                _scanState.value = ScanState.Error("Ошибка сканирования: ${e.message}")
            }
        }
    }
    
    private suspend fun checkOtx(hash: String): OtxCheckResult {
        return try {
            val response = ApiClient.otxApi.checkFileHash(ApiKeys.OTX_API_KEY, hash)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                OtxCheckResult(body.pulse_count > 0, body.pulse_count)
            } else {
                OtxCheckResult(false, 0)
            }
        } catch (e: Exception) {
            OtxCheckResult(false, 0)
        }
    }
    
    private suspend fun checkOtxUrl(url: String): OtxCheckResult {
        return try {
            val encodedUrl = java.net.URLEncoder.encode(url, "UTF-8")
            val response = ApiClient.otxApi.checkUrl(ApiKeys.OTX_API_KEY, encodedUrl)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                OtxCheckResult(body.pulse_count > 0, body.pulse_count)
            } else {
                OtxCheckResult(false, 0)
            }
        } catch (e: Exception) {
            OtxCheckResult(false, 0)
        }
    }
    
    private suspend fun checkVirusTotal(hash: String): VirusTotalCheckResult {
        return try {
            val response = ApiClient.virusTotalApi.getFileReport(
                ApiKeys.VIRUSTOTAL_API_KEY,
                hash
            )
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                VirusTotalCheckResult(
                    body.positives ?: 0,
                    body.total ?: 0,
                    body.positives != null && body.positives > 0
                )
            } else {
                VirusTotalCheckResult(0, 0, false)
            }
        } catch (e: Exception) {
            VirusTotalCheckResult(0, 0, false)
        }
    }
    
    private suspend fun checkVirusTotalUrl(url: String): VirusTotalCheckResult {
        return try {
            val response = ApiClient.virusTotalApi.getUrlReport(
                ApiKeys.VIRUSTOTAL_API_KEY,
                url
            )
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                VirusTotalCheckResult(
                    body.positives ?: 0,
                    body.total ?: 0,
                    body.positives != null && body.positives > 0
                )
            } else {
                VirusTotalCheckResult(0, 0, false)
            }
        } catch (e: Exception) {
            VirusTotalCheckResult(0, 0, false)
        }
    }
    
    private fun calculateFileHash(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                md.update(buffer, 0, bytesRead)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
    
    fun resetState() {
        _scanState.value = ScanState.Idle
    }
}

sealed class ScanState {
    object Idle : ScanState()
    data class Scanning(val message: String) : ScanState()
    data class ThreatFound(val threatName: String, val severity: ThreatSeverity, val details: String) : ScanState()
    data class Safe(val message: String) : ScanState()
    data class Error(val message: String) : ScanState()
}

private data class OtxCheckResult(val isThreat: Boolean, val pulseCount: Int)
private data class VirusTotalCheckResult(val positives: Int, val total: Int, val isThreat: Boolean)

