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
    val names = MutableStateFlow(NamesResult("idle", CardStatus.READY, emptyList(), "", "", 0L))

    fun searchNames(n: Normalized, provider: String) {
        val ctx = getApplication<Application>()
        val label = if (provider == "brave") "Brave" else "Serper.dev"
        val key = SecureStore.getKey(ctx, provider)
        if (key.isEmpty()) {
            names.value = NamesResult("nokey", CardStatus.AUTH, emptyList(), "", label, System.currentTimeMillis())
            return
        }
        viewModelScope.launch {
            names.value = NamesResult("loading", CardStatus.READY, emptyList(), "", label, System.currentTimeMillis())
            val queries = listOf(
                n.combined,
                n.queries.firstOrNull { it.startsWith("مين صاحب") } ?: n.combined
            ).distinct()
            val all = ArrayList<Hit>()
            var detail = ""
            var fatal: ApiException? = null
            var failures = 0
            withContext(Dispatchers.IO) {
                for (q in queries) {
                    try {
                        all.addAll(SearchApi.fetch(provider, key, q))
                    } catch (e: ApiException) {
                        failures++
                        fatal = e
                        detail = e.message ?: ""
                    } catch (e: Exception) {
                        failures++
                        detail = e.javaClass.simpleName
                    }
                }
            }
            val hits = SearchApi.mentions(all, n)
            val now = System.currentTimeMillis()
            names.value = if (failures == queries.size) {
                val code = fatal?.code ?: 0
                val st = if (code == 401 || code == 403) CardStatus.AUTH else if (code == 429) CardStatus.ERROR else CardStatus.UNAVAILABLE
                val msg = when (code) {
                    401, 403 -> "The provider rejected the key (HTTP $code). Check the key in Settings."
                    429 -> "Quota or rate limit reached (HTTP 429)."
                    else -> "The search provider could not be reached ($detail)."
                }
                NamesResult("error", st, emptyList(), msg, label, now)
            } else {
                NamesResult(
                    "done", if (hits.isEmpty()) CardStatus.NO_RESULTS else CardStatus.SUCCESS, hits,
                    if (failures > 0) "One of the searches failed ($detail)." else "", label, now
                )
            }
        }
    }

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
