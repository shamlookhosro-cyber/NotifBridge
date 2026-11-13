package ir.satiatex.notifbridge

import android.app.Notification
import android.content.ComponentName
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class NotifListener : NotificationListenerService() {

    private val lastSeen = HashMap<String, String>()
    private val handler = Handler(Looper.getMainLooper())
    private val callRunners = HashMap<String, Runnable>()
    private val callIds = HashMap<String, MutableList<Int>>()
    private var callSeq = 0

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return
        val prefs = Prefs(this)
        if (!prefs.masterOn) return

        val n = sbn.notification
        val extras = n.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString().orEmpty()
        val label = appLabel(sbn.packageName)

        // ---- calls ----
        if (prefs.callOn) {
            val caller = title.ifBlank { text }
            when (n.category) {
                Notification.CATEGORY_CALL -> {
                    if (isIncomingCall(n)) startCallAlert(sbn.key, caller, prefs) else stopCallAlert(sbn.key)
                    return
                }
                "missed_call" -> {
                    forwardMissedCall(sbn.key, caller, prefs)
                    return
                }
            }
        }

        // ---- normal notifications ----
        if (n.flags and Notification.FLAG_ONGOING_EVENT != 0) return
        val appSelected = sbn.packageName in prefs.enabledApps

        if (prefs.skipSummary &&
            (n.flags and Notification.FLAG_GROUP_SUMMARY != 0 || MessageRules.isSummary(text))
        ) {
            if (appSelected) History.add(label, text.ifBlank { title }, "رد شد: نوتیف خلاصه")
            return
        }

        var t = title
        var x = text
        if (prefs.stripCounts) {
            t = MessageRules.strip(t)
            x = MessageRules.strip(x)
        }
        if (t.isBlank() && x.isBlank()) return
        val shownText = x.ifBlank { t }

        val hay = "$t $x"
        if (MessageRules.firstHit(prefs.blockWords, hay) != null) {
            if (appSelected) History.add(label, shownText, "رد شد: کلمه مسدود")
            return
        }
        val hit = MessageRules.firstHit(prefs.allowWords, hay)
        if (!appSelected && hit == null) return

        if (prefs.isQuietNow() && !(hit != null && prefs.kwIgnoreQuiet)) {
            History.add(label, shownText, "رد شد: ساعت سکوت")
            return
        }

        // Many apps re-post the same notification repeatedly; forward each distinct content once.
        val sig = "$t|$x"
        if (lastSeen[sbn.key] == sig) return
        if (lastSeen.size > 200) lastSeen.clear()
        lastSeen[sbn.key] = sig

        val body = if (prefs.includeTitle && t.isNotBlank() && x.isNotBlank()) "$t: $x" else shownText
        val vib = if (hit != null) prefs.kwVib else prefs.vib(sbn.packageName)
        Forwarder.post(this, sbn.key.hashCode(), label, body, vib)
        History.add(label, body, if (hit != null) "ارسال شد (کلمه مهم: $hit)" else "ارسال شد")
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        stopCallAlert(sbn.key)
        lastSeen.remove(sbn.key)
    }

    override fun onListenerConnected() {
        KeepAlive.start(this)
    }

    override fun onListenerDisconnected() {
        requestRebind(ComponentName(this, NotifListener::class.java))
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    // ---------------- calls ----------------

    /** Ringing call (not an ongoing/active call). */
    private fun isIncomingCall(n: Notification): Boolean {
        val type = n.extras.getInt("android.callType", 0) // CallStyle: 1 incoming, 2 ongoing, 3 screening
        if (type == 1) return true
        if (type != 0) return false
        if (n.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER)) return false
        return n.fullScreenIntent != null || (n.actions?.size ?: 0) >= 2
    }

    private fun startCallAlert(key: String, caller: String, prefs: Prefs) {
        if (callRunners.containsKey(key)) return // already alerting for this call
        if (prefs.isQuietNow() && !prefs.callQuietOk) {
            History.add("تماس", caller, "رد شد: ساعت سکوت")
            return
        }
        val max = prefs.callRepeats
        val interval = prefs.callInterval * 1000L
        val vib = prefs.callVib
        val ids = mutableListOf<Int>()
        callIds[key] = ids
        var count = 0
        val runner = object : Runnable {
            override fun run() {
                if (count >= max) return
                count++
                val id = CALL_ID_BASE + (callSeq++ % 40)
                ids.add(id)
                Forwarder.post(this@NotifListener, id, "تماس ورودی", caller, vib)
                if (count == 1) History.add("تماس", caller, "تماس ورودی؛ تکرار هر ${prefs.callInterval} ثانیه")
                handler.postDelayed(this, interval)
            }
        }
        callRunners[key] = runner
        handler.post(runner)
    }

    private fun stopCallAlert(key: String) {
        callRunners.remove(key)?.let { handler.removeCallbacks(it) }
        callIds.remove(key)?.forEach { Forwarder.cancel(this, it) }
    }

    private fun forwardMissedCall(key: String, caller: String, prefs: Prefs) {
        if (prefs.isQuietNow() && !prefs.callQuietOk) return
        if (lastSeen[key] == caller) return
        lastSeen[key] = caller
        Forwarder.post(this, key.hashCode(), "تماس بی‌پاسخ", caller, prefs.callVib)
        History.add("تماس", caller, "تماس بی‌پاسخ ارسال شد")
    }

    private fun appLabel(pkg: String): String = try {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
    } catch (_: Exception) {
        pkg
    }

    companion object {
        private const val CALL_ID_BASE = 50000
    }
}
