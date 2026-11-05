package com.towkirhs.handy.dictation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.towkirhs.handy.audio.Recorder
import com.towkirhs.handy.data.History
import com.towkirhs.handy.data.JarvisBridge
import com.towkirhs.handy.data.Prefs
import com.towkirhs.handy.data.Vocab
import com.towkirhs.handy.stt.GeminiStt
import com.towkirhs.handy.stt.Transcriber
import com.towkirhs.handy.text.GrammarFixer
import com.towkirhs.handy.text.TextCleaner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** One dictation at a time: record, transcribe on-device, clean up, hand back the text. */
class DictationController(private val context: Context, private val scope: CoroutineScope) {
    enum class State { IDLE, LISTENING, PROCESSING }

    sealed interface Outcome {
        data class Text(val text: String) : Outcome
        data object NoSpeech : Outcome
        data class Failed(val message: String) : Outcome
    }

    private var recorder: Recorder? = null

    private val _state = MutableStateFlow(State.IDLE)
    val state: StateFlow<State> = _state

    private val _level = MutableStateFlow(0f)
    val level: StateFlow<Float> = _level

    /** Returns false if the microphone permission is missing or the mic can't be opened. */
    fun start(): Boolean {
        if (_state.value != State.IDLE) return false
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) return false
        val rec = Recorder { _level.value = it }
        if (!rec.start(scope)) return false
        recorder = rec
        _state.value = State.LISTENING
        return true
    }

    fun cancel() {
        val rec = recorder ?: return
        recorder = null
        _state.value = State.IDLE
        _level.value = 0f
        scope.launch { rec.stop() }
    }

    /** toJarvis: this dictation was spoken to Jarvis, so it always goes there. */
    suspend fun finish(toJarvis: Boolean = false): Outcome {
        val rec = recorder ?: return Outcome.Failed("Not recording")
        recorder = null
        _state.value = State.PROCESSING
        _level.value = 0f
        return try {
            val audio = rec.stop()
            if (audio.samples.size < MIN_SAMPLES || audio.peak < SILENCE_PEAK) {
                Outcome.NoSpeech
            } else {
                val raw = (if (Prefs.sttGemini) GeminiStt.transcribe(audio.samples) else null)
                    ?: Transcriber.transcribe(context, audio.samples, Vocab.hint())
                var text = Vocab.apply(TextCleaner.clean(raw))
                if (text.isEmpty()) {
                    Outcome.NoSpeech
                } else {
                    text = GrammarFixer.fix(context, text)
                    History.add(text)
                    if (toJarvis || Prefs.sendAllToJarvis) JarvisBridge.send(context, text)
                    Outcome.Text(text)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Transcription failed", e)
            Outcome.Failed(e.message ?: "Transcription failed")
        } finally {
            _state.value = State.IDLE
        }
    }

    companion object {
        private const val TAG = "Dictation"
        private const val MIN_SAMPLES = Recorder.SAMPLE_RATE * 3 / 10 // 0.3 s: ignore accidental taps
        private const val SILENCE_PEAK = 0.008f // below this Whisper tends to hallucinate text
    }
}
