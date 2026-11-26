package com.example.domain

enum class VoiceEntryMode {
    CASH,       // مصروف نقدي
    DEBT_OUT,   // دين عليك / كريدي
    RECEIVABLE  // مستحق لك (له)
}

data class ParsedVoiceEntry(
    val title: String,
    val amountText: String,
    val mode: VoiceEntryMode,
    val party: String
)

object VoiceParser {
    private val numRegex = Regex("""(\d+(?:[.,]\d+)?)""")
    private val beforeNumberGuard = Regex("""رقم\s*$""")
    private val beforeShopIndexGuard = Regex("""محل\s*$""")
    private val hasLahRegex = Regex("""(?:^|\s)(?:له|لي عند|مستحق لي)(?=$|\s)""")
    private val hasAlayhRegex = Regex("""(?:^|\s)(?:عليه|علي|دين|كريدي|بالدين|آجل)(?=$|\s)""")
    private val partyRegex = Regex("""(?:^|\s)(?:من عند|عند|من|لـ)\s+([^\s]+(?:\s+[^\s]+)?)""")
    private val noiseWordsRegex = Regex("""(?:^|\s)(?:دج|دينار|دين|كريدي|بالدين|آجل|نقدا|نقداً|له|عليه)(?=$|\s)""")
    private val stopWords = setOf("دين", "كريدي", "بالدين", "دج", "دينار", "له", "عليه")

    fun parseArabicVoiceEntry(raw: String): ParsedVoiceEntry {
        val text = MoneyFormat.normalizeDigits(raw).trim().replace(Regex("""\s+"""), " ")
        if (text.isEmpty()) {
            return ParsedVoiceEntry("", "", VoiceEntryMode.CASH, "")
        }
        val allNums = numRegex.findAll(text).toList()
        val nums = allNums.mapIndexedNotNull { idx, match ->
            val before = text.substring(0, match.range.first)
            val valStr = match.groupValues[1].replace(",", ".")
            val isSmallUnitIndex = valStr.length <= 2 && idx < allNums.lastIndex
            when {
                beforeNumberGuard.containsMatchIn(before) -> null
                isSmallUnitIndex && beforeShopIndexGuard.containsMatchIn(before) -> null
                else -> Triple(valStr, match.range, match.value)
            }
        }

        val chosen = nums.lastOrNull()
        val hasLah = hasLahRegex.containsMatchIn(text)
        val hasAlayh = hasAlayhRegex.containsMatchIn(text)
        val mode = when {
            hasLah && !hasAlayh -> VoiceEntryMode.RECEIVABLE
            hasAlayh -> VoiceEntryMode.DEBT_OUT
            else -> VoiceEntryMode.CASH
        }

        val partyMatch = partyRegex.find(text)
        val party = partyMatch?.groupValues?.getOrNull(1)
            ?.split(" ")
            ?.filter { it !in stopWords }
            ?.take(2)
            ?.joinToString(" ")
            .orEmpty()

        var clean = if (chosen != null) {
            text.removeRange(chosen.second)
        } else {
            text
        }
        if (partyMatch != null) {
            clean = clean.replace(partyMatch.value, " ")
        }
        clean = clean.replace(noiseWordsRegex, " ")
            .replace(Regex("""\s+"""), " ")
            .trim()

        return ParsedVoiceEntry(
            title = clean,
            amountText = chosen?.first.orEmpty(),
            mode = mode,
            party = party
        )
    }
}
