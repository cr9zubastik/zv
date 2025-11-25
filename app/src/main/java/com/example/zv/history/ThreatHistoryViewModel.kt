package com.example.zv.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zv.data.database.AppDatabase
import com.example.zv.data.database.ThreatEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ThreatHistoryViewModel(database: AppDatabase) : ViewModel() {
    private val threatDao = database.threatDao()
    
    val threats: StateFlow<List<ThreatEntity>> = threatDao.getAllThreats()
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    private val _selectedThreats = MutableStateFlow<Set<Long>>(emptySet())
    val selectedThreats: StateFlow<Set<Long>> = _selectedThreats.asStateFlow()
    
    fun toggleSelection(threatId: Long) {
        _selectedThreats.value = if (_selectedThreats.value.contains(threatId)) {
            _selectedThreats.value - threatId
        } else {
            _selectedThreats.value + threatId
        }
    }
    
    fun selectAll(threatIds: List<Long>) {
        _selectedThreats.value = threatIds.toSet()
    }
    
    fun clearSelection() {
        _selectedThreats.value = emptySet()
    }
    
    fun deleteSelected() {
        viewModelScope.launch {
            _selectedThreats.value.forEach { id ->
                threatDao.deleteThreat(id)
            }
            _selectedThreats.value = emptySet()
        }
    }
    
    fun clearHistory() {
        viewModelScope.launch {
            threatDao.deleteAllThreats()
            _selectedThreats.value = emptySet()
        }
    }
}

