package com.towkirhs.handy.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

/** Records 16 kHz mono audio (what Whisper expects) and reports a 0..1 loudness for the waveform. */
class Recorder(private val onLevel: (Float) -> Unit) {
    class Audio(val samples: FloatArray, val peak: Float)

    private var record: AudioRecord? = null
    private var job: Job? = null
    private val chunks = ArrayList<ShortArray>()
    @Volatile private var peak = 0f

    @SuppressLint("MissingPermission")
    fun start(scope: CoroutineScope): Boolean {
        val minBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuffer <= 0) return false
        val chunkSize = SAMPLE_RATE / 20 // 50 ms
        val r = try {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                maxOf(minBuffer, chunkSize * 4)
            )
        } catch (e: Exception) {
            return false
        }
        if (r.state != AudioRecord.STATE_INITIALIZED) {
            r.release()
            return false
        }
        try {
            r.startRecording()
        } catch (e: IllegalStateException) {
            r.release()
            return false
        }
        record = r
        job = scope.launch(Dispatchers.IO) {
            val buf = ShortArray(chunkSize)
            while (true) {
                val n = r.read(buf, 0, buf.size)
                if (n <= 0) break
                var sum = 0.0
                for (i in 0 until n) {
                    val s = buf[i] / 32768.0
                    sum += s * s
                }
                val rms = sqrt(sum / n).toFloat()
                if (rms > peak) peak = rms
                synchronized(chunks) { chunks.add(buf.copyOf(n)) }
                onLevel((rms * 7f).coerceIn(0f, 1f))
            }
        }
        return true
    }

    /** Stops recording and returns everything captured. Safe to call once. */
    suspend fun stop(): Audio {
        record?.let {
            try {
                it.stop() // makes the blocking read() in the loop return
            } catch (e: IllegalStateException) {
                // already stopped
            }
        }
        job?.join()
        record?.release()
        record = null
        return withContext(Dispatchers.Default) {
            synchronized(chunks) {
                val out = FloatArray(chunks.sumOf { it.size })
                var o = 0
                for (c in chunks) {
                    for (s in c) out[o++] = s / 32768f
                }
                Audio(out, peak)
            }
        }
    }

    companion object {
        const val SAMPLE_RATE = 16000
    }
}
