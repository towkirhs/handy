package com.towkirhs.handy.text

import android.content.Context
import com.towkirhs.handy.data.Prefs
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Turns the offline-cleaned transcript into properly written text. Tries the online service first
 * (if the user added a key), then the on-device model, and otherwise leaves the text as it is.
 */
object GrammarFixer {
    private const val LOCAL_TIMEOUT_MS = 25_000L

    suspend fun fix(context: Context, text: String): String {
        // One or two words have no grammar to fix, and a model only risks changing them.
        if (!Prefs.llmEnabled || text.count { it == ' ' } < 2) return text
        if (Prefs.llmApiKey.isNotBlank()) LlmCleaner.clean(text)?.let { return it }
        return withTimeoutOrNull(LOCAL_TIMEOUT_MS) { LocalGrammar.fix(context, text) } ?: text
    }
}
