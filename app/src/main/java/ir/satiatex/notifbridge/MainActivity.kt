package ir.satiatex.notifbridge

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

private const val GARMIN_PKG = "com.garmin.android.apps.connectmobile"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        KeepAlive.start(this)
        setContent {
            val dark = isSystemInDarkTheme()
            val scheme = when {
                Build.VERSION.SDK_INT >= 31 && dark -> dynamicDarkColorScheme(this)
                Build.VERSION.SDK_INT >= 31 -> dynamicLightColorScheme(this)
                dark -> darkColorScheme()
                else -> lightColorScheme()
            }
            MaterialTheme(colorScheme = scheme) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(Modifier.fillMaxSize()) { MainScreen() }
                }
            }
        }
    }
}

private fun isListenerEnabled(ctx: Context) =
    NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName)

private fun hasPostPermission(ctx: Context) =
    Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

private fun isBatteryUnrestricted(ctx: Context) =
    ctx.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(ctx.packageName)

private fun fmt(min: Int) = "%02d:%02d".format(min / 60, min % 60)

@Composable
private fun StatusRow(ok: Boolean, text: String, actionLabel: String, onAction: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(if (ok) "✅" else "⚠️")
        Text(text, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        if (!ok) FilledTonalButton(onClick = onAction) { Text(actionLabel) }
    }
}

@Composable
fun MainScreen(vm: MainViewModel = viewModel()) {
    val ctx = LocalContext.current
    val st by vm.state.collectAsStateWithLifecycle()

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
    val shown = remember(st.apps, st.query) {
        if (st.query.isBlank()) st.apps
        else st.apps.filter { it.label.contains(st.query, ignoreCase = true) || it.pkg.contains(st.query, ignoreCase = true) }
    }

    LazyColumn(
        Modifier.fillMaxSize().systemBarsPadding(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp)) {
                Text("پل نوتیف", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text(
                    "نوتیفیکیشن برنامه‌ها ← Garmin Fenix 7 Pro",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Master switch
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

        // Permissions / background health
        item {
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("پایداری در پس‌زمینه", style = MaterialTheme.typography.titleMedium)
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
                        "روی گوشی‌های شیائومی، سامسونگ، هوآوی، اپو و ...: «شروع خودکار» را روشن کن و باتری برنامه را روی «بدون محدودیت» بگذار. برنامه را هم از لیست برنامه‌های اخیر نبند (قفل کن).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = {
                        ctx.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}"))
                        )
                    }) { Text("باز کردن تنظیمات برنامه") }
                }
            }
        }

        // Garmin
        item {
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("اتصال به ساعت Garmin", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "۱) در Garmin Connect به تنظیمات اعلان‌های ساعت (Notifications / Smart Notifications ← Apps) برو و «پل نوتیف» را روشن کن.\n" +
                            "۲) برنامه‌های اصلی (واتساپ، تلگرام و ...) را همان‌جا خاموش بگذار، وگرنه هر پیام دوبار روی ساعت می‌آید.\n" +
                            "۳) شدت و الگوی ویبره‌ی ساعت از تنظیمات خود ساعت/Garmin Connect تعیین می‌شود، نه از این برنامه.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            val i = ctx.packageManager.getLaunchIntentForPackage(GARMIN_PKG)
                            if (i != null) ctx.startActivity(i)
                            else Toast.makeText(ctx, "Garmin Connect نصب نیست", Toast.LENGTH_SHORT).show()
                        }) { Text("باز کردن Garmin Connect") }
                        OutlinedButton(onClick = {
                            Forwarder.post(ctx, 1, "تست پل نوتیف", "سلام، گچ و پنیر و ژاله ۱۲۳۴۵ تومان", Vib.SHORT)
                        }) { Text("نوتیف تست") }
                    }
                }
            }
        }

        // Settings
        item {
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("ساعت سکوت", style = MaterialTheme.typography.titleMedium)
                            Text("در این بازه نوتیفیکیشنی ارسال نمی‌شود", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(checked = st.quietOn, onCheckedChange = vm::setQuietOn)
                    }
                    if (st.quietOn) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("از")
                            OutlinedButton(onClick = {
                                TimePickerDialog(ctx, { _, h, m -> vm.setQuietStart(h * 60 + m) }, st.quietStart / 60, st.quietStart % 60, true).show()
                            }) { Text(fmt(st.quietStart)) }
                            Text("تا")
                            OutlinedButton(onClick = {
                                TimePickerDialog(ctx, { _, h, m -> vm.setQuietEnd(h * 60 + m) }, st.quietEnd / 60, st.quietEnd % 60, true).show()
                            }) { Text(fmt(st.quietEnd)) }
                        }
                    }
                    HorizontalDivider()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("نام فرستنده را اول متن بیاور", Modifier.weight(1f))
                        Switch(checked = st.includeTitle, onCheckedChange = vm::setIncludeTitle)
                    }
                }
            }
        }

        // Apps
        item {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "برنامه‌ها (${st.enabled.size} انتخاب‌شده)",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
                OutlinedTextField(
                    value = st.query,
                    onValueChange = vm::setQuery,
                    label = { Text("جستجوی برنامه") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        items(shown, key = { it.pkg }) { app ->
            val enabled = app.pkg in st.enabled
            var menu by remember { mutableStateOf(false) }
            Column {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(app.label, style = MaterialTheme.typography.bodyLarge)
                        if (enabled) {
                            Box {
                                TextButton(onClick = { menu = true }) { Text("ویبره گوشی: ${vm.vibOf(app.pkg).label}") }
                                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                    Vib.entries.forEach { v ->
                                        DropdownMenuItem(text = { Text(v.label) }, onClick = {
                                            vm.setVib(app.pkg, v)
                                            menu = false
                                        })
                                    }
                                }
                            }
                        }
                    }
                    Switch(checked = enabled, onCheckedChange = { vm.setEnabled(app.pkg, it) })
                }
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}
