package com.towkirhs.handy.ime

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.towkirhs.handy.dictation.DictationController
import com.towkirhs.handy.overlay.FlowBarView
import com.towkirhs.handy.stt.Transcriber
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * A voice-first keyboard: one big dictation bar plus backspace, space and enter.
 * Use it where the floating bar can't appear, or switch to it for hands-free typing.
 */
class HandyKeyboardService : InputMethodService(), FlowBarView.Listener {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var controller: DictationController
    private var uiJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        controller = DictationController(applicationContext, scope)
        scope.launch {
            try {
                Transcriber.warmUp(applicationContext)
            } catch (e: Exception) {
                Log.e(TAG, "Could not load the speech model", e)
            }
        }
    }

    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onCreateInputView(): View {
        val density = resources.displayMetrics.density
        fun px(v: Int) = (v * density).toInt()

        val hint = TextView(this).apply {
            text = HINT_IDLE
            setTextColor(0xFFB5B0BE.toInt())
            textSize = 13f
            gravity = Gravity.CENTER
        }
        val flowBar = FlowBarView(this, 1.4f).also { it.listener = this }
        val barParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            topMargin = px(14)
            bottomMargin = px(14)
        }

        fun key(label: String, weight: Float, onClick: () -> Unit) = TextView(this).apply {
            text = label
            setTextColor(Color.WHITE)
            textSize = 16f
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                cornerRadius = 14 * density
                setColor(0xFF2B2B33.toInt())
            }
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(0, px(44), weight).apply {
                marginStart = px(4)
                marginEnd = px(4)
            }
        }

        val keys = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(key("Keyboards", 1.6f) {
                getSystemService(InputMethodManager::class.java).showInputMethodPicker()
            })
            addView(key("Space", 2.4f) { currentInputConnection?.commitText(" ", 1) })
            addView(key("⌫", 1f) { sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL) })
            addView(key("↵", 1f) { sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER) })
        }

        uiJob?.cancel()
        uiJob = scope.launch {
            launch {
                controller.state.collect { state ->
                    flowBar.mode = when (state) {
                        DictationController.State.IDLE -> FlowBarView.Mode.IDLE
                        DictationController.State.LISTENING -> FlowBarView.Mode.LISTENING
                        DictationController.State.PROCESSING -> FlowBarView.Mode.PROCESSING
                    }
                    hint.text = when (state) {
                        DictationController.State.IDLE -> HINT_IDLE
                        DictationController.State.LISTENING -> "Listening…"
                        DictationController.State.PROCESSING -> "Polishing…"
                    }
                }
            }
            launch { controller.level.collect { flowBar.pushLevel(it) } }
        }

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF17171C.toInt())
            setPadding(px(12), px(14), px(12), px(10))
            addView(hint)
            addView(flowBar, barParams)
            addView(keys)
        }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        controller.cancel()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onStartRequested() {
        if (!controller.start()) toast("Allow the microphone for Handy in the Handy app first.")
    }

    override fun onFinishRequested() {
        scope.launch {
            when (val outcome = controller.finish()) {
                is DictationController.Outcome.Text -> commit(outcome.text)
                DictationController.Outcome.NoSpeech -> toast("Didn't catch that")
                is DictationController.Outcome.Failed -> toast(outcome.message)
            }
        }
    }

    override fun onCancelRequested() = controller.cancel()

    private fun commit(text: String) {
        val ic = currentInputConnection ?: return
        val before = ic.getTextBeforeCursor(1, 0)
        val needsSpace = !before.isNullOrEmpty() && !before[0].isWhitespace()
        ic.commitText((if (needsSpace) " " else "") + text, 1)
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    companion object {
        private const val TAG = "HandyKeyboard"
        private const val HINT_IDLE = "Tap to dictate · hold to talk"
    }
}
