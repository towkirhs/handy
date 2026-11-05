package com.towkirhs.handy.stt

import android.util.Base64
import android.util.Log
import com.towkirhs.handy.data.Prefs
import com.towkirhs.handy.data.Vocab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

/** Sends the recording to Gemini, which understands Banglish and accents well. Returns null on any failure. */
object GeminiStt {
    private const val TAG = "GeminiStt"

    /** 16 kHz mono float samples to a 16-bit WAV file. */
    private fun wav(samples: FloatArray, rate: Int = 16000): ByteArray {
        val data = ByteArray(samples.size * 2)
        for (k in samples.indices) {
            val v = (samples[k].coerceIn(-1f, 1f) * 32767).toInt()
            data[2 * k] = (v and 0xff).toByte()
            data[2 * k + 1] = (v shr 8 and 0xff).toByte()
        }
        val out = ByteArrayOutputStream()
        fun int(x: Int) = out.write(byteArrayOf(x.toByte(), (x shr 8).toByte(), (x shr 16).toByte(), (x shr 24).toByte()))
        fun short(x: Int) = out.write(byteArrayOf(x.toByte(), (x shr 8).toByte()))
        out.write("RIFF".toByteArray()); int(36 + data.size); out.write("WAVE".toByteArray())
        out.write("fmt ".toByteArray()); int(16); short(1); short(1); int(rate); int(rate * 2); short(2); short(16)
        out.write("data".toByteArray()); int(data.size); out.write(data)
        return out.toByteArray()
    }

    suspend fun transcribe(samples: FloatArray): String? = withContext(Dispatchers.IO) {
        val key = Prefs.geminiKey.trim()
        if (key.isEmpty()) return@withContext null
        var conn: HttpURLConnection? = null
        try {
            val hint = Vocab.hint()
            val prompt = "Transcribe this voice note exactly as spoken, word for word. The speaker often mixes " +
                "Bangla and English. Write Bangla words in English letters, the way the speaker would type them " +
                "(for example: kal, tarikh, sondhay, ase), and English words normally. Add normal punctuation. " +
                (if (hint.isNotEmpty()) "Words this speaker often uses: $hint. " else "") +
                "Do not translate, answer or add anything. Return only the transcript."
            val body = JSONObject()
                .put("contents", JSONArray().put(JSONObject().put("parts", JSONArray()
                    .put(JSONObject().put("text", prompt))
                    .put(JSONObject().put("inline_data", JSONObject()
                        .put("mime_type", "audio/wav")
                        .put("data", Base64.encodeToString(wav(samples), Base64.NO_WRAP)))))))
                .put("generationConfig", JSONObject().put("temperature", 0))
            val model = Prefs.sttModel.trim().ifEmpty { Prefs.DEFAULT_STT_MODEL }
            conn = (URL("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent")
                .openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 5000
                readTimeout = 20000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("x-goog-api-key", key)
            }
            conn.outputStream.use { it.write(body.toString().toByteArray()) }
            if (conn.responseCode != 200) {
                Log.w(TAG, "Gemini returned ${conn.responseCode}: ${conn.errorStream?.bufferedReader()?.readText()?.take(300)}")
                return@withContext null
            }
            val parts = JSONObject(conn.inputStream.bufferedReader().readText())
                .getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts")
            val text = buildString {
                for (k in 0 until parts.length()) {
                    val p = parts.getJSONObject(k)
                    if (!p.optBoolean("thought")) append(p.optString("text"))
                }
            }.trim()
            text.ifEmpty { null }
        } catch (e: Exception) {
            Log.w(TAG, "Gemini transcription failed, using the on-device model", e)
            null
        } finally {
            conn?.disconnect()
        }
    }
}
