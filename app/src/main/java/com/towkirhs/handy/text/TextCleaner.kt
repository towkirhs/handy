package com.towkirhs.handy.text

/**
 * Offline clean-up of a raw Whisper transcript: drops non-speech tags and filler words,
 * collapses stutters, handles "scratch that" / "new line", and fixes spacing and capitals.
 * Whisper already adds punctuation, so this only repairs what removal leaves behind.
 */
object TextCleaner {
    private val ANNOTATION = Regex(
        """\[[^\]]*]|♪[^♪]*♪?|\*[^*\n]{1,30}\*|\((?:music|laughter|applause|silence|noise|inaudible|blank[_ ]audio)[^)]*\)""",
        RegexOption.IGNORE_CASE
    )
    private val FILLER = Regex("""(?:,\s*)?\b(?:um+|uh+|uhm+|erm|er|hmm+|mhm)\b,?""", RegexOption.IGNORE_CASE)
    private val INTRO = Regex("""(^|[.!?]\s+)(?:you know|i mean|like),\s*""", RegexOption.IGNORE_CASE)
    private val STUTTER = Regex(
        """\b(i|a|the|to|and|we|it|in|of|so|but|my|you|he|she|they|is|are|was|if|on|for|at|with)\b(?:,?\s+\1\b)+""",
        RegexOption.IGNORE_CASE
    )

    private val SENTENCE_BREAK = Regex("""(?<=[.!?])\s+""")
    private val SCRATCH = Regex("""scratch that[.!?]*""", RegexOption.IGNORE_CASE)
    private val NEW_LINE = Regex("""new line[.!?]*""", RegexOption.IGNORE_CASE)
    private val NEW_PARAGRAPH = Regex("""new paragraph[.!?]*""", RegexOption.IGNORE_CASE)

    private val SPACES = Regex("""[ \t]+""")
    private val SPACE_BEFORE_PUNCT = Regex("""[ \t]+([,.!?;:])""")
    private val AROUND_NEWLINE = Regex("""[ \t]*\n[ \t]*""")
    private val LEADING_JUNK = Regex("""^[\s,;:]+""")
    private val LOWER_I = Regex("""\bi\b(?!\.)""")
    private val SENTENCE_START = Regex("""([.!?])(\s+)([a-z])""")
    private val LINE_START = Regex("""(\n)([a-z])""")

    fun clean(raw: String): String {
        var t = raw.replace(ANNOTATION, " ")
        t = t.replace(FILLER, " ").trim()
        t = t.replace(INTRO, "\$1")
        t = t.replace(STUTTER, "\$1")
        t = applyCommands(t)
        return tidy(t)
    }

    /** Sentence-level voice commands: "scratch that" drops the previous sentence, "new line" breaks the line. */
    private fun applyCommands(text: String): String {
        val out = ArrayList<String>()
        for (part in text.split(SENTENCE_BREAK)) {
            val s = part.trim()
            when {
                s.isEmpty() -> {}
                SCRATCH.matches(s) -> {
                    val i = out.indexOfLast { !it.startsWith("\n") }
                    if (i >= 0) out.removeAt(i)
                }
                NEW_LINE.matches(s) -> out.add("\n")
                NEW_PARAGRAPH.matches(s) -> out.add("\n\n")
                else -> out.add(s)
            }
        }
        val sb = StringBuilder()
        for (token in out) {
            if (sb.isNotEmpty() && !token.startsWith("\n") && !sb.endsWith('\n')) sb.append(' ')
            sb.append(token)
        }
        return sb.toString()
    }

    private fun tidy(input: String): String {
        var t = input
            .replace(SPACES, " ")
            .replace(SPACE_BEFORE_PUNCT, "\$1")
            .replace(AROUND_NEWLINE, "\n")
            .replace(LEADING_JUNK, "")
            .trim()
        t = t.replace(LOWER_I, "I")

        val afterI = t
        t = SENTENCE_START.replace(afterI) { m ->
            val i = m.range.first
            // Skip abbreviations like "e.g. the": the two characters before the dot must be letters/digits.
            if (i >= 2 && afterI[i - 1].isLetterOrDigit() && afterI[i - 2].isLetterOrDigit()) {
                m.groupValues[1] + m.groupValues[2] + m.groupValues[3].uppercase()
            } else {
                m.value
            }
        }
        t = LINE_START.replace(t) { m -> m.groupValues[1] + m.groupValues[2].uppercase() }
        return t.replaceFirstChar { it.uppercase() }
    }
}
