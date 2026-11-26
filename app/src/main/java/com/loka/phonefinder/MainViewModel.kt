package com.loka.phonefinder

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDb.get(app).dao()

    val history: StateFlow<List<HistoryEntry>> =
        dao.all().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val registry = MutableStateFlow(Registry.load(app))
    val refreshing = MutableStateFlow(false)
    val refreshFailed = MutableStateFlow(false)

    fun add(e: HistoryEntry) {
        viewModelScope.launch { dao.insert(e) }
    }

    fun clearHistory() {
        viewModelScope.launch { dao.clear() }
    }

    fun deleteHistory(id: Long) {
        viewModelScope.launch { dao.delete(id) }
    }

    fun refreshRegistry() {
        if (refreshing.value) return
        viewModelScope.launch {
            refreshing.value = true
            val d = withContext(Dispatchers.IO) { Registry.fetchRemote(getApplication<Application>()) }
            if (d != null) {
                registry.value = d
                refreshFailed.value = false
            } else {
                refreshFailed.value = true
            }
            refreshing.value = false
        }
    }
}
