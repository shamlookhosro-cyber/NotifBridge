package ir.satiatex.notifbridge

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class HistoryEntry(val time: Long, val app: String, val text: String, val result: String)

/** In-memory log of the last forwarding decisions (never written to disk, never sent anywhere). */
object History {
    private val _items = MutableStateFlow<List<HistoryEntry>>(emptyList())
    val items: StateFlow<List<HistoryEntry>> = _items.asStateFlow()

    fun add(app: String, text: String, result: String) {
        val e = HistoryEntry(System.currentTimeMillis(), app, text.take(120), result)
        _items.update { (listOf(e) + it).take(100) }
    }

    fun clear() {
        _items.value = emptyList()
    }
}
