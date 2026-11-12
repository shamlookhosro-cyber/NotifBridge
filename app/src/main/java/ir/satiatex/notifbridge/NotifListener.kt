package ir.satiatex.notifbridge

import android.app.Notification
import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class NotifListener : NotificationListenerService() {

    private val lastSeen = HashMap<String, String>()

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return
        val prefs = Prefs(this)
        if (!prefs.masterOn) return
        if (sbn.packageName !in prefs.enabledApps) return

        val n = sbn.notification
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        if (n.flags and Notification.FLAG_ONGOING_EVENT != 0) return
        if (prefs.isQuietNow()) return

        val extras = n.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString().orEmpty()
        if (text.isBlank() && title.isBlank()) return

        val body = if (prefs.includeTitle && title.isNotBlank()) "$title: $text" else text.ifBlank { title }

        // Many apps re-post the same notification repeatedly; forward each distinct content once.
        val sig = "$title|$text"
        if (lastSeen[sbn.key] == sig) return
        if (lastSeen.size > 200) lastSeen.clear()
        lastSeen[sbn.key] = sig

        val label = try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(sbn.packageName, 0)).toString()
        } catch (_: Exception) {
            sbn.packageName
        }

        Forwarder.post(this, sbn.key.hashCode(), label, body, prefs.vib(sbn.packageName))
    }

    override fun onListenerConnected() {
        KeepAlive.start(this)
    }

    override fun onListenerDisconnected() {
        requestRebind(ComponentName(this, NotifListener::class.java))
    }
}
