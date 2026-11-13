package ir.satiatex.notifbridge

/** Keyword matching and removal of messenger counters like "4 new messages" / "and 4 more". */
object MessageRules {

    private val summaryEn = Regex(
        """\s*\d+\s*(new\s+|unread\s+)?(messages?|chats?|conversations?|notifications?)\b[\s\S]*""",
        RegexOption.IGNORE_CASE,
    )
    private val summaryFa = Regex(
        """\s*\d+\s*(پیام|گفتگو|چت|اعلان|مورد)(\s*(جدید|خوانده\s*نشده|دیگر))?[\s\S]*"""
    )

    /** True when the whole text is just a counter such as "5 new messages" or "5 پیام جدید". */
    fun isSummary(text: String): Boolean {
        val t = TextFix.norm(text).trim()
        if (t.isEmpty() || t.length > 45) return false
        return summaryEn.matches(t) || summaryFa.matches(t)
    }

    private val tails = listOf(
        Regex("""\s*[(\[]\s*\d+\s*(new\s+|more\s+|other\s+|unread\s+)*(messages?|chats?|others?|more)[^)\]]*[)\]]\s*$""", RegexOption.IGNORE_CASE),
        Regex("""\s*[•·|:\-–]\s*\d+\s+(new\s+|more\s+|unread\s+)?(messages?|chats?)\s*$""", RegexOption.IGNORE_CASE),
        Regex("""\s*[(\[]?\s*(\+\s*\d+|(and|&|و)\s+\d+\s*(more|others?|other\s+messages?|پیام\s*دیگر|مورد\s*دیگر|دیگر))\s*[)\]]?\s*$""", RegexOption.IGNORE_CASE),
        Regex("""\s*[(\[]\s*\d+\s*(پیام|مورد)(\s*(جدید|دیگر))?\s*[)\]]\s*$"""),
        Regex("""\s*[•·|:\-–]\s*\d+\s*(پیام|مورد)(\s*(جدید|دیگر))?\s*$"""),
    )

    /** Removes a trailing counter from a title or text. */
    fun strip(input: String): String {
        var s = TextFix.light(input)
        for (re in tails) s = re.replace(s, "")
        return s.trim()
    }

    /** Returns the first keyword found in [haystack] (normalised comparison), or null. */
    fun firstHit(words: List<String>, haystack: String): String? {
        if (words.isEmpty()) return null
        val h = TextFix.norm(haystack)
        return words.firstOrNull { w ->
            val n = TextFix.norm(w).trim()
            n.isNotEmpty() && h.contains(n)
        }
    }
}
