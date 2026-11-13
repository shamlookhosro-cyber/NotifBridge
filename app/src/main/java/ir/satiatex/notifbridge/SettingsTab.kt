package ir.satiatex.notifbridge

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun SettingsTab(vm: MainViewModel, st: UiState) {
    val ctx = LocalContext.current
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionCard("ساعت سکوت", "در این بازه (مثلاً شب‌ها) هیچ نوتیفیکیشنی به ساعت ارسال نمی‌شود.") {
                SwitchRow("فعال", st.quietOn, { on -> vm.edit { quietOn = on } })
                if (st.quietOn) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("از")
                        OutlinedButton(onClick = {
                            TimePickerDialog(
                                ctx,
                                { _, h, m -> vm.edit { quietStart = h * 60 + m } },
                                st.quietStart / 60, st.quietStart % 60, true,
                            ).show()
                        }) { Text(fmtMinutes(st.quietStart)) }
                        Text("تا")
                        OutlinedButton(onClick = {
                            TimePickerDialog(
                                ctx,
                                { _, h, m -> vm.edit { quietEnd = h * 60 + m } },
                                st.quietEnd / 60, st.quietEnd % 60, true,
                            ).show()
                        }) { Text(fmtMinutes(st.quietEnd)) }
                    }
                }
            }
        }

        item {
            SectionCard("نمایش پیام") {
                SwitchRow(
                    "نام فرستنده را اول متن بیاور",
                    st.includeTitle,
                    { on -> vm.edit { includeTitle = on } },
                    sub = "مثلاً «علی: سلام» به‌جای «سلام»",
                )
                HorizontalDivider()
                SwitchRow(
                    "تبدیل حروف فارسی به عربی",
                    st.fixLetters,
                    { on -> vm.edit { fixLetters = on } },
                    sub = "گ چ پ ژ ی ک ← ك ج ب ز ي ك (برای ساعت‌هایی که این حروف را نشان نمی‌دهند)",
                )
                SwitchRow(
                    "تبدیل اعداد فارسی به لاتین",
                    st.fixDigits,
                    { on -> vm.edit { fixDigits = on } },
                    sub = "۱۲۳ ← 123",
                )
                Text(
                    "عنوان نوتیف روی ساعت همیشه نام برنامه است.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            SectionCard("درباره") {
                Text("پل نوتیف · نسخه 1.2", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "این برنامه دسترسی به اینترنت ندارد. نوتیفیکیشن‌ها فقط روی خود گوشی پردازش می‌شوند و جایی ارسال یا ذخیره نمی‌شوند.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
