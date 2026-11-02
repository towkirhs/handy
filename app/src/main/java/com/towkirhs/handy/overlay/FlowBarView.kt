package com.towkirhs.handy.overlay

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.LinearInterpolator
import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * The floating dictation bar, drawn by hand.
 *  - IDLE: a small dark bubble. Tap to start/stop, or hold to talk and release to finish.
 *  - LISTENING: a pill with a live waveform, a cancel button on the left and a done button on the right.
 *  - PROCESSING: a pill with pulsing dots while the transcript is transcribed and polished.
 */
class FlowBarView(
    context: Context,
    private val scale: Float = 1f,
    /** When set, the idle bubble shows this letter on the accent colour (used for the Jarvis bubble). */
    private val letter: String? = null
) : View(context) {
    enum class Mode { IDLE, LISTENING, PROCESSING }

    interface Listener {
        fun onStartRequested()
        fun onFinishRequested()
        fun onCancelRequested()
        fun onDrag(dx: Int, dy: Int) {}
        fun onDragEnd() {}
    }

    var listener: Listener? = null

    private val levels = FloatArray(30)
    private var phase = 0f
    private val pulse = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 900
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            phase = it.animatedValue as Float
            invalidate()
        }
    }

    var mode: Mode = Mode.IDLE
        set(value) {
            if (field == value) return
            field = value
            if (value == Mode.LISTENING) levels.fill(0f)
            if (value == Mode.PROCESSING) pulse.start() else pulse.cancel()
            scaleX = 0.92f
            scaleY = 0.92f
            animate().scaleX(1f).scaleY(1f).setDuration(120).start()
            requestLayout()
            invalidate()
        }

    private fun dp(v: Float) = v * resources.displayMetrics.density * scale

    val barHeight: Int get() = dp(52f).roundToInt()

    fun widthFor(m: Mode): Int = when (m) {
        Mode.IDLE -> dp(52f)
        Mode.LISTENING -> dp(224f)
        Mode.PROCESSING -> dp(132f)
    }.roundToInt()

    fun pushLevel(level: Float) {
        System.arraycopy(levels, 1, levels, 0, levels.size - 1)
        levels[levels.size - 1] = level
        if (mode == Mode.LISTENING) invalidate()
    }

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = WHITE
    }
    private val rect = RectF()
    private val path = Path()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(widthFor(mode), barHeight)
    }

    override fun onDetachedFromWindow() {
        pulse.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val r = h / 2f

        stroke.strokeWidth = dp(1f)
        stroke.color = BORDER
        fill.color = if (letter != null && mode == Mode.IDLE) ACCENT else BACKGROUND
        fill.alpha = 255
        rect.set(dp(0.5f), dp(0.5f), w - dp(0.5f), h - dp(0.5f))
        canvas.drawRoundRect(rect, r, r, fill)
        canvas.drawRoundRect(rect, r, r, stroke)

        when (mode) {
            Mode.IDLE -> drawIdle(canvas, w, h)
            Mode.LISTENING -> drawListening(canvas, w, h)
            Mode.PROCESSING -> drawProcessing(canvas, w, h)
        }
    }

    private fun drawIdle(canvas: Canvas, w: Float, h: Float) {
        if (letter != null) {
            fill.color = WHITE
            fill.alpha = 255
            fill.textSize = dp(22f)
            fill.textAlign = Paint.Align.CENTER
            fill.isFakeBoldText = true
            canvas.drawText(letter, w / 2f, h / 2f - (fill.ascent() + fill.descent()) / 2f, fill)
            return
        }
        val heights = floatArrayOf(8f, 14f, 20f, 14f, 8f)
        val barW = dp(3f)
        val gap = dp(3.5f)
        val total = heights.size * barW + (heights.size - 1) * gap
        var x = (w - total) / 2f
        fill.color = WHITE
        fill.alpha = 215
        for (hp in heights) {
            val bh = dp(hp)
            rect.set(x, (h - bh) / 2f, x + barW, (h + bh) / 2f)
            canvas.drawRoundRect(rect, barW / 2f, barW / 2f, fill)
            x += barW + gap
        }
    }

    private fun drawListening(canvas: Canvas, w: Float, h: Float) {
        val cy = h / 2f
        val buttonR = h * 0.34f

        // Cancel
        fill.color = CIRCLE
        fill.alpha = 255
        canvas.drawCircle(h / 2f, cy, buttonR, fill)
        stroke.color = WHITE
        stroke.strokeWidth = dp(2f)
        val d = dp(4.5f)
        canvas.drawLine(h / 2f - d, cy - d, h / 2f + d, cy + d, stroke)
        canvas.drawLine(h / 2f - d, cy + d, h / 2f + d, cy - d, stroke)

        // Done
        val dx = w - h / 2f
        fill.color = ACCENT
        fill.alpha = 255
        canvas.drawCircle(dx, cy, buttonR, fill)
        stroke.color = WHITE
        stroke.strokeWidth = dp(2.2f)
        path.reset()
        path.moveTo(dx - dp(5.5f), cy + dp(0.5f))
        path.lineTo(dx - dp(1.5f), cy + dp(4.5f))
        path.lineTo(dx + dp(5.5f), cy - dp(4f))
        canvas.drawPath(path, stroke)

        // Waveform: newest sample on the right
        val left = h + dp(2f)
        val right = w - h - dp(2f)
        val barW = dp(3f)
        val gap = dp(3f)
        val slot = barW + gap
        val n = minOf(levels.size, ((right - left + gap) / slot).toInt())
        var x = left + ((right - left) - (n * slot - gap)) / 2f
        for (i in 0 until n) {
            val level = levels[levels.size - n + i]
            val bh = dp(4f) + level * (h * 0.5f)
            fill.color = ACCENT
            fill.alpha = 90 + 165 * (i + 1) / n
            rect.set(x, cy - bh / 2f, x + barW, cy + bh / 2f)
            canvas.drawRoundRect(rect, barW / 2f, barW / 2f, fill)
            x += slot
        }
    }

    private fun drawProcessing(canvas: Canvas, w: Float, h: Float) {
        val radius = dp(3.5f)
        val spacing = dp(13f)
        val cy = h / 2f
        fill.color = WHITE
        for (i in 0 until 3) {
            val p = 0.5f + 0.5f * sin(2.0 * PI * (phase - i * 0.18f)).toFloat()
            fill.alpha = (90 + 165 * p).toInt()
            canvas.drawCircle(w / 2f + (i - 1) * spacing, cy, radius, fill)
        }
    }

    private val slop = ViewConfiguration.get(context).scaledTouchSlop
    private var downX = 0f
    private var downY = 0f
    private var lastRawX = 0f
    private var lastRawY = 0f
    private var downMode = Mode.IDLE
    private var dragging = false
    private var pushToTalk = false

    private val holdRunnable = Runnable {
        if (!dragging && mode == Mode.IDLE) {
            pushToTalk = true
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            listener?.onStartRequested()
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = e.x
                downY = e.y
                lastRawX = e.rawX
                lastRawY = e.rawY
                downMode = mode
                dragging = false
                pushToTalk = false
                if (mode == Mode.IDLE) postDelayed(holdRunnable, HOLD_MS)
            }
            MotionEvent.ACTION_MOVE -> {
                if (downMode == Mode.IDLE && !pushToTalk) {
                    if (!dragging && hypot(e.x - downX, e.y - downY) > slop) {
                        dragging = true
                        removeCallbacks(holdRunnable)
                    }
                    if (dragging) {
                        listener?.onDrag((e.rawX - lastRawX).roundToInt(), (e.rawY - lastRawY).roundToInt())
                        lastRawX = e.rawX
                        lastRawY = e.rawY
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                removeCallbacks(holdRunnable)
                val isUp = e.actionMasked == MotionEvent.ACTION_UP
                when {
                    pushToTalk -> {
                        pushToTalk = false
                        if (mode == Mode.LISTENING) listener?.onFinishRequested()
                    }
                    dragging -> {
                        dragging = false
                        listener?.onDragEnd()
                    }
                    isUp && downMode == Mode.IDLE && mode == Mode.IDLE -> {
                        performClick()
                        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        listener?.onStartRequested()
                    }
                    isUp && downMode == Mode.LISTENING && mode == Mode.LISTENING -> {
                        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        if (downX < height) listener?.onCancelRequested() else listener?.onFinishRequested()
                    }
                }
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    companion object {
        private const val HOLD_MS = 300L
        private val BACKGROUND = 0xFF17171C.toInt()
        private val BORDER = 0x33FFFFFF
        private val CIRCLE = 0xFF2B2B33.toInt()
        private val ACCENT = 0xFF8B7CFF.toInt()
        private val WHITE = 0xFFFFFFFF.toInt()
    }
}
