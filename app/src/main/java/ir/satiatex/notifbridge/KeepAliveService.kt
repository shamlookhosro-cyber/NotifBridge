package ir.satiatex.notifbridge

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat

/**
 * Foreground service whose only job is to keep this app's process at high priority,
 * so the system does not kill the NotificationListenerService in the background.
 */
class KeepAliveService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "فعال بودن در پس‌زمینه", NotificationManager.IMPORTANCE_MIN).apply {
                    setShowBadge(false)
                }
            )
        }
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n: Notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle("پل نوتیف فعال است")
            .setContentText("نوتیفیکیشن‌ها به ساعت ارسال می‌شوند")
            .setContentIntent(open)
            .setOngoing(true)
            .setLocalOnly(true) // never bridge this status notification to the watch
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

        if (Build.VERSION.SDK_INT >= 34) {
            ServiceCompat.startForeground(this, ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(ID, n)
        }
        return START_STICKY
    }

    companion object {
        private const val CHANNEL = "keepalive"
        private const val ID = 4242
    }
}

object KeepAlive {
    fun start(ctx: Context) {
        if (!Prefs(ctx).masterOn) return
        try {
            ContextCompat.startForegroundService(ctx, Intent(ctx, KeepAliveService::class.java))
        } catch (_: Exception) {
            // Background-start restrictions: it will be started next time the app is opened.
        }
    }

    fun stop(ctx: Context) {
        ctx.stopService(Intent(ctx, KeepAliveService::class.java))
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) KeepAlive.start(context)
    }
}
