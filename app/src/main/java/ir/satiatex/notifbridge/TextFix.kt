package ir.satiatex.notifbridge

/**
 * Text helpers. Code points are written as escapes so file encoding can never break the mapping.
 */
object TextFix {
    private val letterMap: Map<Char, Char> = mapOf(
        'گ' to 'ك', // گ -> ك
        'ک' to 'ك', // ک -> ك
        'چ' to 'ج', // چ -> ج
        'پ' to 'ب', // پ -> ب
        'ژ' to 'ز', // ژ -> ز
        'ی' to 'ي', // ی -> ي
    )

    /** For the watch: Persian-only letters -> closest Arabic letters, Persian/Arabic-Indic digits -> Latin. */
    fun convert(input: String, letters: Boolean = true, digits: Boolean = true): String {
        val sb = StringBuilder(input.length)
        for (c in input) {
            when {
                digits && c in '\u06F0'..'\u06F9' -> sb.append('0' + (c - '\u06F0'))
                digits && c in '\u0660'..'\u0669' -> sb.append('0' + (c - '\u0660'))
                letters -> sb.append(letterMap[c] ?: c)
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    /** Light normalisation: Latin digits and Persian letter forms (ی ک). Keeps case and spacing. */
    fun light(input: String): String {
        val sb = StringBuilder(input.length)
        for (c in input) {
            sb.append(
                when (c) {
                    in '۰'..'۹' -> '0' + (c - '۰')
                    in '٠'..'٩' -> '0' + (c - '٠')
                    'ي', 'ى' -> 'ی' // ي ى -> ی
                    'ك' -> 'ک'           // ك -> ک
                    else -> c
                }
            )
        }
        return sb.toString()
    }

    /** For matching: light() + lower-case, without ZWNJ and Arabic diacritics. */
    fun norm(input: String): String =
        light(input).lowercase().filterNot { it == '‌' || it in 'ً'..'ٟ' }
}
