package ir.satiatex.notifbridge

import android.content.Context
import java.util.Calendar

enum class Vib(val label: String, val pattern: LongArray?) {
    OFF("بدون ویبره", null),
    SHORT("کوتاه", longArrayOf(0, 200)),
    LONG("بلند", longArrayOf(0, 800)),
    DOUBLE("دوتایی", longArrayOf(0, 200, 150, 200)),
    TRIPLE("سه‌تایی", longArrayOf(0, 150, 100, 150, 100, 150)),
}

class Prefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private fun vibOf(key: String, def: Vib): Vib =
        runCatching { Vib.valueOf(sp.getString(key, null) ?: def.name) }.getOrDefault(def)

    private fun words(key: String): List<String> =
        (sp.getString(key, "") ?: "").split('\n').map { it.trim() }.filter { it.isNotEmpty() }

    private fun putWords(key: String, list: List<String>) =
        sp.edit().putString(key, list.joinToString("\n")).apply()

    // ---- apps ----
    val enabledApps: Set<String>
        get() = HashSet(sp.getStringSet("apps", emptySet()) ?: emptySet())

    fun setAppEnabled(pkg: String, on: Boolean) {
        val set = HashSet(enabledApps)
        if (on) set.add(pkg) else set.remove(pkg)
        sp.edit().putStringSet("apps", set).apply()
    }

    fun vib(pkg: String): Vib = vibOf("vib_$pkg", Vib.SHORT)

    fun setVib(pkg: String, v: Vib) = sp.edit().putString("vib_$pkg", v.name).apply()

    fun allVib(): Map<String, Vib> =
        sp.all.keys.filter { it.startsWith("vib_") }
            .associate { it.removePrefix("vib_") to vib(it.removePrefix("vib_")) }

    // ---- general ----
    var masterOn: Boolean
        get() = sp.getBoolean("master_on", true)
        set(v) = sp.edit().putBoolean("master_on", v).apply()

    var fixLetters: Boolean
        get() = sp.getBoolean("fix_letters", true)
        set(v) = sp.edit().putBoolean("fix_letters", v).apply()

    var fixDigits: Boolean
        get() = sp.getBoolean("fix_digits", true)
        set(v) = sp.edit().putBoolean("fix_digits", v).apply()

    var includeTitle: Boolean
        get() = sp.getBoolean("include_title", false)
        set(v) = sp.edit().putBoolean("include_title", v).apply()

    var quietOn: Boolean
        get() = sp.getBoolean("quiet_on", true)
        set(v) = sp.edit().putBoolean("quiet_on", v).apply()

    /** minutes since midnight */
    var quietStart: Int
        get() = sp.getInt("quiet_start", 23 * 60)
        set(v) = sp.edit().putInt("quiet_start", v).apply()

    var quietEnd: Int
        get() = sp.getInt("quiet_end", 7 * 60)
        set(v) = sp.edit().putInt("quiet_end", v).apply()

    // ---- keyword rules ----
    var allowWords: List<String>
        get() = words("allow_words")
        set(v) = putWords("allow_words", v)

    var blockWords: List<String>
        get() = words("block_words")
        set(v) = putWords("block_words", v)

    var kwVib: Vib
        get() = vibOf("kw_vib", Vib.TRIPLE)
        set(v) = sp.edit().putString("kw_vib", v.name).apply()

    var kwIgnoreQuiet: Boolean
        get() = sp.getBoolean("kw_ignore_quiet", true)
        set(v) = sp.edit().putBoolean("kw_ignore_quiet", v).apply()

    // ---- messenger summaries ----
    var skipSummary: Boolean
        get() = sp.getBoolean("skip_summary", true)
        set(v) = sp.edit().putBoolean("skip_summary", v).apply()

    var stripCounts: Boolean
        get() = sp.getBoolean("strip_counts", false)
        set(v) = sp.edit().putBoolean("strip_counts", v).apply()

    // ---- calls ----
    var callOn: Boolean
        get() = sp.getBoolean("call_on", true)
        set(v) = sp.edit().putBoolean("call_on", v).apply()

    var callInterval: Int
        get() = sp.getInt("call_interval", 4)
        set(v) = sp.edit().putInt("call_interval", v).apply()

    var callRepeats: Int
        get() = sp.getInt("call_repeats", 8)
        set(v) = sp.edit().putInt("call_repeats", v).apply()

    var callVib: Vib
        get() = vibOf("call_vib", Vib.TRIPLE)
        set(v) = sp.edit().putString("call_vib", v.name).apply()

    var callQuietOk: Boolean
        get() = sp.getBoolean("call_quiet_ok", false)
        set(v) = sp.edit().putBoolean("call_quiet_ok", v).apply()

    fun isQuietNow(): Boolean {
        if (!quietOn) return false
        val cal = Calendar.getInstance()
        val now = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val s = quietStart
        val e = quietEnd
        return when {
            s == e -> false
            s < e -> now in s until e
            else -> now >= s || now < e // wraps past midnight
        }
    }
}
