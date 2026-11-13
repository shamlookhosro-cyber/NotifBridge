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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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

/** Companion apps of popular smartwatches; shown as shortcuts only when installed. */
private val WATCH_APPS = listOf(
    "Garmin Connect" to "com.garmin.android.apps.connectmobile",
    "Zepp" to "com.huami.watch.hmwatchmanager",
    "Mi Fitness" to "com.xiaomi.wearable",
    "Galaxy Wearable" to "com.samsung.android.app.watchmanager",
    "Wear OS" to "com.google.android.wearable.app",
    "Huawei Health" to "com.huawei.health",
    "Fitbit" to "com.fitbit.FitbitMobile",
    "Polar Flow" to "fi.polar.polarflow",
    "Suunto" to "com.stt.android.suunto",
)

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

private fun openAppInfo(ctx: Context) {
    ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}")))
}

@Composable
private fun StatusRow(ok: Boolean, text: String, actionLabel: String, onAction: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(if (ok) "✅" else "⚠️")
        Text(text, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        if (!ok) FilledTonalButton(onClick = onAction) { Text(actionLabel) }
    }
}

@Composable
private fun SectionCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { content() }
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

    var tab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("خانه" to "🏠", "برنامه‌ها" to "📱", "تنظیمات" to "⚙️")

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { i, (label, emoji) ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Text(emoji) },
                        label = { Text(label) },
                    )
                }
            }
        },
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            when (tab) {
                0 -> HomeTab(st, vm, listenerOn, postOn, batteryOk) {
                    permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                1 -> AppsTab(st, vm)
                else -> SettingsTab(st, vm)
            }
        }
    }
}

@Composable
private fun HomeTab(
    st: UiState,
    vm: MainViewModel,
    listenerOn: Boolean,
    postOn: Boolean,
    batteryOk: Boolean,
    requestPost: () -> Unit,
) {
    val ctx = LocalContext.current
    val ready = listenerOn && postOn && batteryOk
    val companions = remember { WATCH_APPS.filter { ctx.packageManager.getLaunchIntentForPackage(it.second) != null } }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp)) {
                Text("پل نوتیف", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text(
                    "نوتیفیکیشن برنامه‌ها ← ساعت هوشمند",
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
            SectionCard {
                Text("دسترسی‌ها و پایداری", style = MaterialTheme.typography.titleMedium)
                StatusRow(listenerOn, "دسترسی خواندن نوتیفیکیشن‌ها", "فعال‌سازی") {
                    ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }
                StatusRow(postOn, "اجازه ارسال نوتیفیکیشن", "اجازه", requestPost)
                StatusRow(batteryOk, "بدون محدودیت باتری", "برداشتن") {
                    ctx.startActivity(
                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${ctx.packageName}"))
                    )
                }
                Text(
                    "روی گوشی‌های شیائومی، سامسونگ، هوآوی، اوپو و ...: «شروع خودکار» را روشن کن و برنامه را در لیست برنامه‌های اخیر قفل کن.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = { openAppInfo(ctx) }) { Text("باز کردن اطلاعات برنامه") }
            }
        }

        if (!listenerOn) {
            item {
                Card(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("اندروید اجازه فعال‌سازی نمی‌دهد؟", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "برای برنامه‌هایی که خارج از فروشگاه نصب شده‌اند، اندروید در ابتدا «تنظیمات محدودشده» را مسدود می‌کند و پیام امنیتی می‌دهد. این مراحل را انجام بده:\n" +
                                "۱) یک بار روی «فعال‌سازی» بزن و کلید برنامه را روشن کن تا پیام خطا بیاید.\n" +
                                "۲) «باز کردن اطلاعات برنامه» را بزن، منوی سه‌نقطه بالا را باز کن و «اجازه تنظیمات محدودشده» را انتخاب کن.\n" +
                                "۳) به همین صفحه برگرد و دوباره «فعال‌سازی» را بزن.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(onClick = { openAppInfo(ctx) }) { Text("اطلاعات برنامه") }
                            OutlinedButton(onClick = {
                                ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                            }) { Text("فعال‌سازی") }
                        }
                    }
                }
            }
        }

        item {
            SectionCard {
                Text("اتصال به ساعت", style = MaterialTheme.typography.titleMedium)
                Text(
                    "این برنامه با هر ساعتی کار می‌کند که نوتیفیکیشن‌های گوشی را نشان می‌دهد.\n" +
                        "۱) در برنامه همراه ساعتت، بخش اعلان‌ها (Notifications) را باز کن و «پل نوتیف» را روشن کن.\n" +
                        "۲) برنامه‌های اصلی (واتساپ، تلگرام و ...) را همان‌جا خاموش بگذار تا پیام تکراری نیاید.\n" +
                        "۳) ویبره‌ی ساعت از تنظیمات خود ساعت تعیین می‌شود، نه از این برنامه.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (companions.isNotEmpty()) {
                    Text("برنامه‌های ساعت روی گوشی تو:", style = MaterialTheme.typography.labelLarge)
                    companions.forEach { (name, pkg) ->
                        OutlinedButton(onClick = {
                            ctx.packageManager.getLaunchIntentForPackage(pkg)?.let { ctx.startActivity(it) }
                        }) { Text("باز کردن $name") }
                    }
                }
                OutlinedButton(onClick = {
                    Forwarder.post(ctx, 1, "تست پل نوتیف", "سلام، گچ و پنیر و ژاله ۱۲۳۴۵ تومان", Vib.SHORT)
                }) { Text("ارسال نوتیفیکیشن تست") }
            }
        }
    }
}

@Composable
private fun AppsTab(st: UiState, vm: MainViewModel) {
    val shown = remember(st.apps, st.query) {
        if (st.query.isBlank()) st.apps
        else st.apps.filter { it.label.contains(st.query, ignoreCase = true) || it.pkg.contains(st.query, ignoreCase = true) }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("برنامه‌ها", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "${st.enabled.size} برنامه انتخاب شده. فقط نوتیف‌های برنامه‌های روشن به ساعت می‌رسند.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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

@Composable
private fun SettingsTab(st: UiState, vm: MainViewModel) {
    val ctx = LocalContext.current
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                "تنظیمات",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp),
            )
        }
        item {
            SectionCard {
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
            }
        }
        item {
            SectionCard {
                Text("متن نوتیفیکیشن", style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("تبدیل حروف فارسی به عربی")
                        Text("گ چ پ ژ ی ک ← ك ج ب ز ي ك", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = st.fixLetters, onCheckedChange = vm::setFixLetters)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("تبدیل اعداد فارسی به لاتین")
                        Text("۱۲۳ ← 123", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = st.fixDigits, onCheckedChange = vm::setFixDigits)
                }
                HorizontalDivider()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("نام فرستنده را اول متن بیاور", Modifier.weight(1f))
                    Switch(checked = st.includeTitle, onCheckedChange = vm::setIncludeTitle)
                }
            }
        }
        item {
            SectionCard {
                Text("درباره", style = MaterialTheme.typography.titleMedium)
                Text("پل نوتیف — نسخه 1.1", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "این برنامه نوتیفیکیشن‌ها را فقط روی خود گوشی پردازش می‌کند و هیچ داده‌ای به اینترنت نمی‌فرستد.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
