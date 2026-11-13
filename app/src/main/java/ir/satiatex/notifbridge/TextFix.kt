package ir.satiatex.notifbridge

/**
 * Converts Persian-only letters to the closest Arabic letters and
 * Persian/Arabic-Indic digits to Latin digits (for watches with weak Persian fonts).
 * Code points are written as escapes so file encoding can never break the mapping.
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

    fun convert(input: String, letters: Boolean = true, digits: Boolean = true): String {
        val sb = StringBuilder(input.length)
        for (c in input) {
            when {
                digits && c in '۰'..'۹' -> sb.append('0' + (c - '۰')) // ۰-۹
                digits && c in '٠'..'٩' -> sb.append('0' + (c - '٠')) // ٠-٩
                letters -> sb.append(letterMap[c] ?: c)
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }
}
