package ir.satiatex.notifbridge

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryTab() {
    val list by History.items.collectAsStateWithLifecycle()
    val fmt = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("تاریخچه (${list.size})", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "برای فهمیدن اینکه چرا یک پیام ارسال نشد. فقط در حافظه است و با بسته شدن برنامه پاک می‌شود.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = { History.clear() }) { Text("پاک کردن") }
            }
        }
        if (list.isEmpty()) {
            item {
                Text(
                    "هنوز چیزی ثبت نشده.",
                    Modifier.padding(horizontal = 20.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        items(list) { e ->
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text("${fmt.format(Date(e.time))} · ${e.app}", style = MaterialTheme.typography.labelMedium)
                Text(e.text, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                Text(
                    e.result,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (e.result.startsWith("رد")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        }
    }
}
