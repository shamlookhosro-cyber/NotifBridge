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
    val enabled: Set<String> = emptySet(),
    val vib: Map<String, Vib> = emptyMap(),
    val quietOn: Boolean = true,
    val quietStart: Int = 23 * 60,
    val quietEnd: Int = 7 * 60,
    val includeTitle: Boolean = false,
    val masterOn: Boolean = true,
    val query: String = "",
)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = Prefs(app)

    private val _state = MutableStateFlow(
        UiState(
            enabled = prefs.enabledApps,
            vib = prefs.allVib(),
            quietOn = prefs.quietOn,
            quietStart = prefs.quietStart,
            quietEnd = prefs.quietEnd,
            includeTitle = prefs.includeTitle,
            masterOn = prefs.masterOn,
        )
    )
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val apps = withContext(Dispatchers.IO) { loadApps() }
            _state.update { it.copy(apps = apps) }
        }
    }

    private fun loadApps(): List<AppItem> {
        val pm = getApplication<Application>().packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, 0)
            .map { AppItem(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
            .filter { it.pkg != getApplication<Application>().packageName }
            .distinctBy { it.pkg }
            .sortedBy { it.label.lowercase() }
    }

    fun vibOf(pkg: String): Vib = _state.value.vib[pkg] ?: Vib.SHORT

    fun setQuery(q: String) = _state.update { it.copy(query = q) }

    fun setEnabled(pkg: String, on: Boolean) {
        prefs.setAppEnabled(pkg, on)
        _state.update { it.copy(enabled = prefs.enabledApps) }
    }

    fun setVib(pkg: String, v: Vib) {
        prefs.setVib(pkg, v)
        _state.update { it.copy(vib = it.vib + (pkg to v)) }
    }

    fun setQuietOn(on: Boolean) {
        prefs.quietOn = on
        _state.update { it.copy(quietOn = on) }
    }

    fun setQuietStart(min: Int) {
        prefs.quietStart = min
        _state.update { it.copy(quietStart = min) }
    }

    fun setQuietEnd(min: Int) {
        prefs.quietEnd = min
        _state.update { it.copy(quietEnd = min) }
    }

    fun setMasterOn(on: Boolean) {
        prefs.masterOn = on
        val ctx = getApplication<Application>()
        if (on) KeepAlive.start(ctx) else KeepAlive.stop(ctx)
        _state.update { it.copy(masterOn = on) }
    }

    fun setIncludeTitle(on: Boolean) {
        prefs.includeTitle = on
        _state.update { it.copy(includeTitle = on) }
    }
}
