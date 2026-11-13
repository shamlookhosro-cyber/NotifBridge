package ir.satiatex.notifbridge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun RulesTab(vm: MainViewModel, st: UiState) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionCard(
                "کلمات مهم",
                "پیامی که یکی از این کلمات را داشته باشد از هر برنامه‌ای به ساعت می‌رود، حتی اگر آن برنامه را انتخاب نکرده باشی.",
            ) {
                WordsEditor(
                    words = st.allowWords,
                    hint = "مثلاً: فوری، کد تأیید، سفارش",
                    onAdd = { vm.addWord(true, it) },
                    onRemove = { vm.removeWord(true, it) },
                )
                VibPicker("ویبره گوشی برای کلمات مهم", st.kwVib) { v -> vm.edit { kwVib = v } }
                SwitchRow(
                    "در ساعت سکوت هم ارسال شود",
                    st.kwIgnoreQuiet,
                    { on -> vm.edit { kwIgnoreQuiet = on } },
                )
            }
        }

        item {
            SectionCard(
                "کلمات مسدود",
                "پیامی که یکی از این کلمات را داشته باشد هرگز ارسال نمی‌شود (مثلاً تبلیغ، کد تخفیف). این قانون از «کلمات مهم» قوی‌تر است.",
            ) {
                WordsEditor(
                    words = st.blockWords,
                    hint = "مثلاً: تبلیغ، تخفیف",
                    onAdd = { vm.addWord(false, it) },
                    onRemove = { vm.removeWord(false, it) },
                )
            }
        }

        item {
            SectionCard(
                "تماس",
                "تماس ورودی از هر برنامه‌ای (تماس عادی، واتساپ، تلگرام و ...) چند بار پشت‌سرهم ارسال می‌شود تا با پیام معمولی اشتباه نشود. وقتی جواب بدهی یا قطع شود، تکرار متوقف و نوتیف‌های تماس از ساعت پاک می‌شوند.",
            ) {
                SwitchRow("هشدار تکراری تماس", st.callOn, { on -> vm.edit { callOn = on } })
                if (st.callOn) {
                    LabeledSlider("فاصله تکرار (ثانیه)", st.callInterval, 2..10) { v -> vm.edit { callInterval = v } }
                    LabeledSlider("تعداد تکرار", st.callRepeats, 2..20) { v -> vm.edit { callRepeats = v } }
                    VibPicker("ویبره گوشی برای تماس", st.callVib) { v -> vm.edit { callVib = v } }
                    SwitchRow(
                        "در ساعت سکوت هم هشدار بده",
                        st.callQuietOk,
                        { on -> vm.edit { callQuietOk = on } },
                    )
                }
            }
        }

        item {
            SectionCard(
                "پیام‌رسان‌ها",
                "پیام‌رسان‌ها گاهی علاوه بر خود پیام، یک نوتیف خلاصه (مثل «۵ پیام جدید») یا عبارتی مثل «و ۴ پیام دیگر» می‌فرستند.",
            ) {
                SwitchRow(
                    "نوتیف‌های خلاصه ارسال نشوند",
                    st.skipSummary,
                    { on -> vm.edit { skipSummary = on } },
                    sub = "نوتیف‌های گروهی و فقط‌شمارنده مثل «۵ پیام جدید»",
                )
                SwitchRow(
                    "شمارنده را از متن پیام حذف کن",
                    st.stripCounts,
                    { on -> vm.edit { stripCounts = on } },
                    sub = "عبارت‌هایی مثل «(۴ پیام)»، «و ۴ پیام دیگر»، «and 4 more» از آخر عنوان و متن پاک می‌شود",
                )
            }
        }
    }
}
