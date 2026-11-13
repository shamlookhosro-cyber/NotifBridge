package ir.satiatex.notifbridge

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/** Re-posts a notification as: title = [title], text = message (Persian-converted for the watch). */
object Forwarder {
    private fun channelId(v: Vib) = "fwd_${v.name.lowercase()}"

    private fun ensureChannel(ctx: Context, v: Vib) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        val id = channelId(v)
        if (nm.getNotificationChannel(id) != null) return
        val importance = if (v == Vib.OFF) NotificationManager.IMPORTANCE_LOW else NotificationManager.IMPORTANCE_DEFAULT
        val ch = NotificationChannel(id, "ویبره: ${v.label}", importance).apply {
            setSound(null, null)
            if (v.pattern != null) {
                enableVibration(true)
                vibrationPattern = v.pattern
            } else {
                enableVibration(false)
            }
        }
        nm.createNotificationChannel(ch)
    }

    @SuppressLint("MissingPermission")
    fun post(ctx: Context, id: Int, title: String, text: String, vib: Vib) {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        ensureChannel(ctx, vib)
        val p = Prefs(ctx)
        val t = TextFix.convert(title, p.fixLetters, p.fixDigits)
        val body = TextFix.convert(text, p.fixLetters, p.fixDigits)
        val n = NotificationCompat.Builder(ctx, channelId(vib))
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(t)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(ctx).notify(id, n)
    }

    fun cancel(ctx: Context, id: Int) {
        NotificationManagerCompat.from(ctx).cancel(id)
    }
}
