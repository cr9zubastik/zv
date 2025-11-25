package com.example.zv.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "threats")
data class ThreatEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: ThreatType,
    val severity: ThreatSeverity,
    val source: String, // "VirusTotal" or "OTX"
    val detectedAt: Long = System.currentTimeMillis(),
    val details: String? = null,
    val hash: String? = null,
    val url: String? = null
)

enum class ThreatType {
    FILE,
    URL,
    UNKNOWN
}

enum class ThreatSeverity {
    HIGH,
    MEDIUM,
    LOW,
    SAFE
}

