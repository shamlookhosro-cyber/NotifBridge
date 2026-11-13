package ir.satiatex.notifbridge

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppItem(val pkg: String, val label: String)

data class UiState(
    val apps: List<AppItem> = emptyList(),
    val query: String = "",
    val enabled: Set<String> = emptySet(),
    val vib: Map<String, Vib> = emptyMap(),
    val masterOn: Boolean = true,
    val includeTitle: Boolean = false,
    val fixLetters: Boolean = true,
    val fixDigits: Boolean = true,
    val quietOn: Boolean = true,
    val quietStart: Int = 23 * 60,
    val quietEnd: Int = 7 * 60,
    val allowWords: List<String> = emptyList(),
    val blockWords: List<String> = emptyList(),
    val kwVib: Vib = Vib.TRIPLE,
    val kwIgnoreQuiet: Boolean = true,
    val skipSummary: Boolean = true,
    val stripCounts: Boolean = false,
    val callOn: Boolean = true,
    val callInterval: Int = 4,
    val callRepeats: Int = 8,
    val callVib: Vib = Vib.TRIPLE,
    val callQuietOk: Boolean = false,
)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = Prefs(app)

    private fun snapshot(base: UiState) = base.copy(
        enabled = prefs.enabledApps,
        vib = prefs.allVib(),
        masterOn = prefs.masterOn,
        includeTitle = prefs.includeTitle,
        fixLetters = prefs.fixLetters,
        fixDigits = prefs.fixDigits,
        quietOn = prefs.quietOn,
        quietStart = prefs.quietStart,
        quietEnd = prefs.quietEnd,
        allowWords = prefs.allowWords,
        blockWords = prefs.blockWords,
        kwVib = prefs.kwVib,
        kwIgnoreQuiet = prefs.kwIgnoreQuiet,
        skipSummary = prefs.skipSummary,
        stripCounts = prefs.stripCounts,
        callOn = prefs.callOn,
        callInterval = prefs.callInterval,
        callRepeats = prefs.callRepeats,
        callVib = prefs.callVib,
        callQuietOk = prefs.callQuietOk,
    )

    private val _state = MutableStateFlow(snapshot(UiState()))
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val apps = withContext(Dispatchers.IO) { loadApps() }
            _state.update { it.copy(apps = apps) }
        }
    }

    private fun loadApps(): List<AppItem> {
        val ctx = getApplication<Application>()
        val pm = ctx.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, 0)
            .map { AppItem(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
            .filter { it.pkg != ctx.packageName }
            .distinctBy { it.pkg }
            .sortedBy { it.label.lowercase() }
    }

    /** Applies a change to Prefs and refreshes the UI state from it. */
    fun edit(block: Prefs.() -> Unit) {
        prefs.block()
        _state.update { snapshot(it) }
    }

    fun setQuery(q: String) = _state.update { it.copy(query = q) }

    fun vibOf(pkg: String): Vib = _state.value.vib[pkg] ?: Vib.SHORT

    fun setMasterOn(on: Boolean) {
        edit { masterOn = on }
        val ctx = getApplication<Application>()
        if (on) KeepAlive.start(ctx) else KeepAlive.stop(ctx)
    }

    fun addWord(allow: Boolean, raw: String) {
        val w = raw.trim()
        if (w.isEmpty()) return
        edit {
            if (allow) {
                if (w !in allowWords) allowWords = allowWords + w
            } else {
                if (w !in blockWords) blockWords = blockWords + w
            }
        }
    }

    fun removeWord(allow: Boolean, w: String) {
        edit {
            if (allow) allowWords = allowWords - w else blockWords = blockWords - w
        }
    }
}
