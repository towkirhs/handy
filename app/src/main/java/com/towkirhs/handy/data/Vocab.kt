package com.towkirhs.handy.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * "My words": the speaker's own words (names, places, Banglish) and fixes learned from their corrections
 * ("ultapalt" -> "ulta palta"). Used as a hint before recognition and to correct known mistakes after it.
 */
object Vocab {
    data class Fix(val heard: String, val meant: String)

    private lateinit var prefs: SharedPreferences
    private val _words = MutableStateFlow<List<String>>(emptyList())
    private val _fixes = MutableStateFlow<List<Fix>>(emptyList())
    val words: StateFlow<List<String>> = _words
    val fixes: StateFlow<List<Fix>> = _fixes

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences("handy_vocab", Context.MODE_PRIVATE)
        _words.value = JSONArray(prefs.getString("words", "[]")).let { a -> List(a.length()) { a.getString(it) } }
        _fixes.value = JSONArray(prefs.getString("fixes", "[]")).let { a ->
            List(a.length()) { a.getJSONObject(it).let { o -> Fix(o.getString("h"), o.getString("m")) } }
        }
    }

    private fun save() {
        prefs.edit {
            putString("words", JSONArray(_words.value).toString())
            putString("fixes", JSONArray().apply {
                _fixes.value.forEach { put(JSONObject().put("h", it.heard).put("m", it.meant)) }
            }.toString())
        }
    }

    fun addWord(word: String) {
        val w = word.trim()
        if (w.isEmpty() || _words.value.any { it.equals(w, ignoreCase = true) }) return
        _words.value = _words.value + w
        save()
    }

    fun removeWord(word: String) {
        _words.value = _words.value - word
        save()
    }

    fun addFix(heard: String, meant: String) {
        val h = heard.trim()
        val m = meant.trim()
        if (h.isEmpty() || m.isEmpty() || h.equals(m, ignoreCase = true)) return
        _fixes.value = _fixes.value.filterNot { it.heard.equals(h, ignoreCase = true) } + Fix(h, m)
        save()
    }

    fun removeFix(fix: Fix) {
        _fixes.value = _fixes.value - fix
        save()
    }

    /** Replaces known mistakes, longest first, as whole words, ignoring case. */
    fun apply(text: String): String {
        var out = text
        for (f in _fixes.value.sortedByDescending { it.heard.length }) {
            out = Regex("(?i)(?<![\\p{L}\\p{N}])${Regex.escape(f.heard)}(?![\\p{L}\\p{N}])").replace(out, Regex.escapeReplacement(f.meant))
        }
        return out
    }

    /** The speaker's words, for the recognizer to expect. Kept short: Whisper only reads about 200 tokens of hint. */
    fun hint(): String {
        val all = (_words.value + _fixes.value.map { it.meant }).distinctBy { it.lowercase() }
        val sb = StringBuilder()
        for (w in all.asReversed()) { // newest first
            if (sb.length + w.length > 600) break
            if (sb.isNotEmpty()) sb.append(", ")
            sb.append(w)
        }
        return sb.toString()
    }

    private fun tokens(s: String) = s.split(Regex("\\s+")).filter { it.isNotEmpty() }
    private fun bare(t: String) = t.trim { !it.isLetterOrDigit() }.lowercase()

    /**
     * Compares what was heard with what the person corrected it to, and learns each changed stretch
     * of up to 4 words as a fix. Returns how many fixes were learned.
     */
    fun learnFromEdit(heard: String, meant: String): Int {
        val a = tokens(heard)
        val b = tokens(meant)
        val n = a.size
        val m = b.size
        // Longest common subsequence of the words (ignoring case and punctuation).
        val lcs = Array(n + 1) { IntArray(m + 1) }
        for (i in n - 1 downTo 0) for (j in m - 1 downTo 0) {
            lcs[i][j] = if (bare(a[i]) == bare(b[j])) lcs[i + 1][j + 1] + 1 else maxOf(lcs[i + 1][j], lcs[i][j + 1])
        }
        var i = 0
        var j = 0
        var learned = 0
        val gapA = mutableListOf<String>()
        val gapB = mutableListOf<String>()
        fun flush() {
            if (gapA.isNotEmpty() && gapB.isNotEmpty() && gapA.size <= 4 && gapB.size <= 4) {
                addFix(gapA.joinToString(" ") { bare(it) }, gapB.joinToString(" ") { it.trim { c -> !c.isLetterOrDigit() } })
                learned++
            }
            gapA.clear()
            gapB.clear()
        }
        while (i < n || j < m) {
            when {
                i < n && j < m && bare(a[i]) == bare(b[j]) -> { flush(); i++; j++ }
                j < m && (i == n || lcs[i][j + 1] >= lcs[i + 1][j]) -> gapB.add(b[j++])
                else -> gapA.add(a[i++])
            }
        }
        flush()
        return learned
    }
}
