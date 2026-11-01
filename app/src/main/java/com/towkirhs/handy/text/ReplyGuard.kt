package com.towkirhs.handy.text

/**
 * Tidies what a language model sends back and rejects replies that don't look like a corrected
 * version of the dictation (an answer to a question, an essay, or a few stray words). Returning null
 * means "use the text as it was" so a misbehaving model can never put the wrong words in a field.
 */
object ReplyGuard {
    private val FENCE_START = Regex("^```[A-Za-z]*\\s*")
    private val FENCE_END = Regex("\\s*```$")
    private val PREAMBLE = Regex(
        "^(?:here(?:'s| is)[^:\\n]*|sure[^:\\n]*|corrected(?: text)?|output)\\s*:\\s*",
        RegexOption.IGNORE_CASE
    )

    fun accept(input: String, reply: String): String? {
        var t = reply.trim().replace(FENCE_START, "").replace(FENCE_END, "").trim()

        PREAMBLE.find(t)?.let { m ->
            val lead = m.value.trim().takeWhile { it.isLetter() || it == '\'' }
            // Leave it alone if the speaker really did start their sentence that way.
            if (!input.trimStart().startsWith(lead, ignoreCase = true)) {
                t = t.substring(m.range.last + 1).trim()
            }
        }

        if (t.length >= 2) {
            val wrapped = (t.first() == '"' && t.last() == '"') || (t.first() == '“' && t.last() == '”')
            if (wrapped && !input.trim().startsWith(t.first())) t = t.substring(1, t.length - 1).trim()
        }

        if (t.isEmpty()) return null
        val min = input.length / 2
        val max = input.length * 8 / 5 + 30
        return if (t.length in min..max) t else null
    }
}
