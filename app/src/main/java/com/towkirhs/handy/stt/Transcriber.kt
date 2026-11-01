package com.towkirhs.handy.stt

import android.content.Context
import com.whispercpp.whisper.WhisperContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Runs Whisper fully on-device. The model ships inside the APK, so no network is ever needed. */
object Transcriber {
    private const val MODEL_ASSET = "models/ggml-base.en-q5_1.bin"

    private val mutex = Mutex()
    private var whisper: WhisperContext? = null

    private suspend fun context(app: Context): WhisperContext =
        whisper ?: withContext(Dispatchers.IO) {
            WhisperContext.createContextFromAsset(app.applicationContext.assets, MODEL_ASSET)
        }.also { whisper = it }

    /** Loads the model ahead of the first dictation so it starts fast. */
    suspend fun warmUp(app: Context) {
        mutex.withLock { context(app) }
    }

    suspend fun transcribe(app: Context, samples: FloatArray): String =
        mutex.withLock { context(app).transcribeData(samples, printTimestamp = false) }
}
