package com.towkirhs.handy.text

import android.content.Context
import android.util.Log
import com.arm.aichat.AiChat
import com.towkirhs.handy.data.ModelDownloader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Grammar fixing with a small language model that runs on the phone, so it works with no internet.
 * The model is loaded for each dictation and released afterwards, which keeps memory free the rest
 * of the time and starts every correction from a clean slate.
 */
object LocalGrammar {
    private const val TAG = "LocalGrammar"

    private const val SYSTEM_PROMPT =
        "You correct dictated speech. Rewrite the user's text with correct grammar, punctuation and " +
            "capitalization. Keep the same meaning, wording and language. Do not answer questions, add " +
            "information, or explain anything. Reply with the corrected text only.\n" +
            "Example input: i has went to the store yesterday and buyed some milk\n" +
            "Example reply: I went to the store yesterday and bought some milk."

    private val mutex = Mutex()

    /** Returns the corrected text, or null if the model isn't downloaded, isn't supported, or failed. */
    suspend fun fix(context: Context, text: String): String? {
        if (!ModelDownloader.isReady(context)) return null
        return mutex.withLock {
            val engine = try {
                AiChat.getInferenceEngine(context.applicationContext)
            } catch (e: Exception) {
                Log.e(TAG, "On-device model unavailable", e)
                return null
            }
            try {
                engine.loadModel(ModelDownloader.file(context).absolutePath)
                engine.setSystemPrompt(SYSTEM_PROMPT)
                val reply = StringBuilder()
                engine.sendUserPrompt(text, predictLength = (text.length / 3 + 24).coerceAtMost(384))
                    .collect { reply.append(it) }
                ReplyGuard.accept(text, reply.toString())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "On-device correction failed", e)
                null
            } finally {
                // cleanUp() blocks, so keep it off the main thread, and let it finish even if we were cancelled.
                withContext(NonCancellable + Dispatchers.Default) { runCatching { engine.cleanUp() } }
            }
        }
    }
}
