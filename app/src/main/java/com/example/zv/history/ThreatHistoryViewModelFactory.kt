package com.example.zv.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.zv.data.database.AppDatabase

class ThreatHistoryViewModelFactory(private val database: AppDatabase) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ThreatHistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ThreatHistoryViewModel(database) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

