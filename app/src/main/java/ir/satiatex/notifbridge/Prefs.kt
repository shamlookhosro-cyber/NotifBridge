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

    val enabledApps: Set<String>
        get() = HashSet(sp.getStringSet("apps", emptySet()) ?: emptySet())

    fun setAppEnabled(pkg: String, on: Boolean) {
        val set = HashSet(enabledApps)
        if (on) set.add(pkg) else set.remove(pkg)
        sp.edit().putStringSet("apps", set).apply()
    }

    fun vib(pkg: String): Vib =
        runCatching { Vib.valueOf(sp.getString("vib_$pkg", null) ?: Vib.SHORT.name) }
            .getOrDefault(Vib.SHORT)

    fun setVib(pkg: String, v: Vib) = sp.edit().putString("vib_$pkg", v.name).apply()

    fun allVib(): Map<String, Vib> =
        sp.all.keys.filter { it.startsWith("vib_") }.associate { it.removePrefix("vib_") to vib(it.removePrefix("vib_")) }

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

    var masterOn: Boolean
        get() = sp.getBoolean("master_on", true)
        set(v) = sp.edit().putBoolean("master_on", v).apply()

    var includeTitle: Boolean
        get() = sp.getBoolean("include_title", false)
        set(v) = sp.edit().putBoolean("include_title", v).apply()

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
