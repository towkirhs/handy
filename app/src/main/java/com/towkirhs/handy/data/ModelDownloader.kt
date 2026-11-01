package com.towkirhs.handy.data

import android.content.Context
import android.os.Build
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * One-time download of the on-device grammar model (Qwen2.5 0.5B Instruct, Apache-2.0).
 * It is too big to ship inside the APK. Interrupted downloads resume where they stopped.
 */
object ModelDownloader {
    sealed interface State {
        data object Missing : State
        data class Downloading(val percent: Int) : State
        data object Ready : State
        data class Failed(val message: String) : State
    }

    const val MODEL_URL =
        "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf"
    const val SIZE_LABEL = "about 400 MB"
    private const val MODEL_FILE = "qwen2.5-0.5b-instruct-q4_k_m.gguf"
    private const val MIN_BYTES = 300_000_000L

    // Lives as long as the app process, so leaving the settings screen doesn't stop the download.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    private val _state = MutableStateFlow<State>(State.Missing)
    val state: StateFlow<State> = _state

    /** The on-device model only has native code for 64-bit ARM phones, which is nearly all of them today. */
    fun supported(): Boolean = Build.SUPPORTED_ABIS.firstOrNull() == "arm64-v8a"

    fun file(context: Context): File = File(File(context.filesDir, "models"), MODEL_FILE)

    fun isReady(context: Context): Boolean = supported() && file(context).length() >= MIN_BYTES

    /** Re-reads what is on disk. Does nothing while a download is running. */
    fun refresh(context: Context) {
        if (job?.isActive == true) return
        _state.value = if (isReady(context)) State.Ready else State.Missing
    }

    fun start(context: Context) {
        if (job?.isActive == true || !supported()) return
        val app = context.applicationContext
        job = scope.launch {
            _state.value = State.Downloading(0)
            try {
                download(app)
                _state.value = State.Ready
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = State.Failed(e.message ?: "Download failed")
            }
        }
    }

    fun delete(context: Context) {
        if (job?.isActive == true) return
        file(context).delete()
        File(file(context).path + ".part").delete()
        _state.value = State.Missing
    }

    private fun download(app: Context) {
        val target = file(app)
        target.parentFile?.mkdirs()
        val part = File(target.path + ".part")
        val have = if (part.exists()) part.length() else 0L

        val conn = (URL(MODEL_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            if (have > 0) setRequestProperty("Range", "bytes=$have-")
        }
        try {
            val code = conn.responseCode
            if (code != 200 && code != 206) {
                part.delete() // start clean next time
                throw IOException("Server answered $code. Tap Download to try again.")
            }
            val append = code == 206
            val startAt = if (append) have else 0L
            val length = conn.contentLengthLong
            val total = if (length > 0) length + startAt else -1L

            FileOutputStream(part, append).use { out ->
                conn.inputStream.use { input ->
                    val buffer = ByteArray(64 * 1024)
                    var done = startAt
                    var lastPercent = -1
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        out.write(buffer, 0, n)
                        done += n
                        if (total > 0) {
                            val percent = (done * 100 / total).toInt()
                            if (percent != lastPercent) {
                                lastPercent = percent
                                _state.value = State.Downloading(percent)
                            }
                        }
                    }
                }
            }

            if (part.length() < MIN_BYTES || (total > 0 && part.length() != total)) {
                throw IOException("The download stopped early. Tap Download to resume.")
            }
            if (!part.renameTo(target)) throw IOException("Could not save the model.")
        } finally {
            conn.disconnect()
        }
    }
}
