package ir.satiatex.notifbridge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AppsTab(vm: MainViewModel, st: UiState) {
    var onlySelected by remember { mutableStateOf(false) }
    val shown = remember(st.apps, st.query, onlySelected, st.enabled) {
        st.apps.filter { a ->
            (!onlySelected || a.pkg in st.enabled) &&
                (st.query.isBlank() || a.label.contains(st.query, ignoreCase = true) || a.pkg.contains(st.query, ignoreCase = true))
        }
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)) {
        item {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("برنامه‌ها (${st.enabled.size} انتخاب‌شده)", style = MaterialTheme.typography.titleLarge)
                Text(
                    "نوتیفیکیشن برنامه‌های روشن‌شده به ساعت ارسال می‌شود.",
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
                FilterChip(
                    selected = onlySelected,
                    onClick = { onlySelected = !onlySelected },
                    label = { Text("فقط انتخاب‌شده‌ها") },
                )
            }
        }

        items(shown, key = { it.pkg }) { app ->
            val enabled = app.pkg in st.enabled
            Column {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(app.label, style = MaterialTheme.typography.bodyLarge)
                        if (enabled) {
                            VibPicker("ویبره گوشی", vm.vibOf(app.pkg)) { v -> vm.edit { setVib(app.pkg, v) } }
                        }
                    }
                    Switch(checked = enabled, onCheckedChange = { on -> vm.edit { setAppEnabled(app.pkg, on) } })
                }
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}
