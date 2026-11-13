package ir.satiatex.notifbridge

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

/** Companion apps of common smartwatches; used only to offer an "open watch app" shortcut. */
private val WATCH_APPS = listOf(
    "com.garmin.android.apps.connectmobile",
    "com.huami.watch.hmwatchmanager",
    "com.xiaomi.hm.health",
    "com.mi.health",
    "com.huawei.health",
    "com.samsung.android.app.watchmanager",
    "com.google.android.wearable.app",
    "com.fitbit.FitbitMobile",
    "fi.polar.polarflow",
    "com.stt.android.suunto",
    "com.coros.coros",
)

@Composable
fun HomeTab(vm: MainViewModel, st: UiState) {
    val ctx = LocalContext.current

    var listenerOn by remember { mutableStateOf(isListenerEnabled(ctx)) }
    var postOn by remember { mutableStateOf(hasPostPermission(ctx)) }
    var batteryOk by remember { mutableStateOf(isBatteryUnrestricted(ctx)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        listenerOn = isListenerEnabled(ctx)
        postOn = hasPostPermission(ctx)
        batteryOk = isBatteryUnrestricted(ctx)
        KeepAlive.start(ctx)
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { postOn = it }

    val ready = listenerOn && postOn && batteryOk
    val watchApps = remember {
        WATCH_APPS.mapNotNull { pkg ->
            runCatching {
                if (ctx.packageManager.getLaunchIntentForPackage(pkg) == null) null
                else pkg to ctx.packageManager.getApplicationLabel(ctx.packageManager.getApplicationInfo(pkg, 0)).toString()
            }.getOrNull()
        }
    }

    fun openAppInfo() = ctx.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}"))
    )

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text("پل نوتیف", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text(
                    "نوتیفیکیشن برنامه‌های گوشی را روی ساعت هوشمندت ببین",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            Card(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (st.masterOn && ready) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
            ) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (st.masterOn) "ارسال به ساعت: روشن" else "ارسال به ساعت: خاموش",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            when {
                                !st.masterOn -> "هیچ نوتیفیکیشنی ارسال نمی‌شود"
                                ready -> "همه‌چیز آماده است و در پس‌زمینه فعال می‌ماند"
                                else -> "برای پایدار ماندن در پس‌زمینه، موارد زیر را کامل کن"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Switch(checked = st.masterOn, onCheckedChange = vm::setMasterOn)
                }
            }
        }

        item {
            SectionCard("پایداری در پس‌زمینه") {
                StatusRow(listenerOn, "دسترسی خواندن نوتیفیکیشن‌ها", "فعال‌سازی") {
                    ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }
                StatusRow(postOn, "اجازه ارسال نوتیفیکیشن", "اجازه") {
                    permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                StatusRow(batteryOk, "بدون محدودیت باتری", "برداشتن") {
                    ctx.startActivity(
                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${ctx.packageName}"))
                    )
                }
                Text(
                    "روی گوشی‌های شیائومی، سامسونگ، هوآوی، اپو و ...: «شروع خودکار» را روشن کن و باتری برنامه را روی «بدون محدودیت» بگذار. برنامه را از لیست برنامه‌های اخیر هم قفل کن تا بسته نشود.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = { openAppInfo() }) { Text("باز کردن تنظیمات برنامه") }
            }
        }

        if (!listenerOn) {
            item {
                SectionCard(
                    "اگر گوشی نگذاشت دسترسی نوتیفیکیشن فعال شود",
                    "اندروید ۱۳ به بعد برای برنامه‌هایی که از بیرون فروشگاه نصب شده‌اند، پیام «برای امنیت شما این تنظیم در دسترس نیست» نشان می‌دهد. این مراحل را به همین ترتیب برو:",
                ) {
                    Text(
                        "۱) اول «فعال‌سازی» را بزن و در لیست روی «پل نوتیف» بزن تا پیام امنیتی بیاید. آن را تأیید (OK) کن.\n" +
                            "۲) از تنظیمات برگرد و «باز کردن اطلاعات برنامه» را بزن.\n" +
                            "۳) در بالا-گوشه صفحه روی سه‌نقطه ⋮ بزن و «اجازه تنظیمات محدودشده» (Allow restricted settings) را انتخاب کن؛ با اثر انگشت یا رمز تأیید کن.\n" +
                            "۴) دوباره «فعال‌سازی» را بزن و «پل نوتیف» را روشن کن.\n\n" +
                            "نکته: گزینه ⋮ معمولاً فقط بعد از انجام مرحله ۱ ظاهر می‌شود؛ اگر هنوز آن را نمی‌بینی، مرحله ۱ را یک بار دیگر تکرار کن. اگر باز هم نبود، نام برنامه را در تنظیمات ← برنامه‌ها جستجو کن و همان صفحه را باز کن.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }) {
                            Text("فعال‌سازی")
                        }
                        OutlinedButton(onClick = { openAppInfo() }) { Text("اطلاعات برنامه") }
                    }
                }
            }
        }

        item {
            SectionCard("اتصال به ساعت") {
                Text(
                    "۱) در اپ همراه ساعتت (همان برنامه‌ای که ساعت را به گوشی وصل می‌کند) بخش اعلان‌ها / Notifications را باز کن و «پل نوتیف» را برای ارسال به ساعت روشن کن.\n" +
                        "۲) برنامه‌های اصلی (واتساپ، تلگرام و ...) را همان‌جا خاموش بگذار، وگرنه هر پیام دوبار روی ساعت می‌آید. اگر ساعتت همه اعلان‌ها را خودکار می‌گیرد، آن‌ها را از اپ همراه ساعت محدود کن.\n" +
                        "۳) شدت و الگوی ویبره‌ی ساعت از تنظیمات خود ساعت یا اپ همراه آن تعیین می‌شود؛ ویبره‌ی داخل این برنامه فقط مخصوص گوشی است.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                watchApps.forEach { (pkg, name) ->
                    Button(onClick = {
                        ctx.packageManager.getLaunchIntentForPackage(pkg)?.let { ctx.startActivity(it) }
                    }) { Text("باز کردن $name") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        if (hasPostPermission(ctx)) {
                            Forwarder.post(ctx, 1, "تست پل نوتیف", "سلام، گچ و پنیر و ژاله ۱۲۳۴۵ تومان", Vib.SHORT)
                        } else {
                            Toast.makeText(ctx, "اول اجازه ارسال نوتیفیکیشن را بده", Toast.LENGTH_SHORT).show()
                        }
                    }) { Text("نوتیف تست") }
                }
            }
        }
    }
}
