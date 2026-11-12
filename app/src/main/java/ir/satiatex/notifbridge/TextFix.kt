package ir.satiatex.notifbridge

/**
 * Converts Persian-only letters to the closest Arabic letters and
 * Persian/Arabic-Indic digits to Latin digits (for watches with weak Persian fonts).
 * Code points are written as escapes so file encoding can never break the mapping.
 */
object TextFix {
    private val letters: Map<Char, Char> = mapOf(
        'گ' to 'ك', // گ -> ك
        'ک' to 'ك', // ک -> ك
        'چ' to 'ج', // چ -> ج
        'پ' to 'ب', // پ -> ب
        'ژ' to 'ز', // ژ -> ز
        'ی' to 'ي', // ی -> ي
    )

    fun convert(input: String): String {
        val sb = StringBuilder(input.length)
        for (c in input) {
            when (c) {
                in '۰'..'۹' -> sb.append('0' + (c - '۰')) // ۰-۹
                in '٠'..'٩' -> sb.append('0' + (c - '٠')) // ٠-٩
                else -> sb.append(letters[c] ?: c)
            }
        }
        return sb.toString()
    }
}
