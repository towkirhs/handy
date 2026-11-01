package com.towkirhs.handy.overlay

import android.Manifest
import android.app.PendingIntent
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.towkirhs.handy.HandyApplication
import com.towkirhs.handy.R
import com.towkirhs.handy.access.FocusState
import com.towkirhs.handy.access.HandyAccessibilityService
import com.towkirhs.handy.data.Prefs
import com.towkirhs.handy.dictation.DictationController
import com.towkirhs.handy.stt.Transcriber
import com.towkirhs.handy.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Foreground service that owns the floating bar and runs dictation. The bar only shows while a text
 * field is focused (or while a dictation is in progress), and can be dragged anywhere.
 */
class OverlayService : Service(), FlowBarView.Listener {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var wm: WindowManager
    private lateinit var bar: FlowBarView
    private lateinit var params: WindowManager.LayoutParams
    private lateinit var controller: DictationController
    private var attached = false
    private var centerX = 0
    private var centerY = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        // Must happen first: the system kills the app if a started foreground service doesn't call this quickly.
        try {
            ServiceCompat.startForeground(
                this, NOTIFICATION_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } catch (e: Exception) {
            Log.e(TAG, "Could not start in the foreground", e)
            Prefs.barEnabled = false
            stopSelf()
            return
        }
        if (!Settings.canDrawOverlays(this)) {
            Prefs.barEnabled = false
            stopSelf()
            return
        }

        running = true
        wm = getSystemService(WindowManager::class.java)
        controller = DictationController(applicationContext, scope)
        bar = FlowBarView(this).also {
            it.listener = this
            it.visibility = View.GONE
            it.contentDescription = "Handy dictation"
        }
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START }

        val dm = resources.displayMetrics
        centerX = Prefs.barCenterX.takeIf { it >= 0 }
            ?: (dm.widthPixels - bar.widthFor(FlowBarView.Mode.IDLE) / 2 - (8 * dm.density).toInt())
        centerY = Prefs.barCenterY.takeIf { it >= 0 } ?: (dm.heightPixels * 0.6f).toInt()
        applyPosition()
        wm.addView(bar, params)
        attached = true

        scope.launch {
            controller.state.collect { state ->
                bar.mode = when (state) {
                    DictationController.State.IDLE -> FlowBarView.Mode.IDLE
                    DictationController.State.LISTENING -> FlowBarView.Mode.LISTENING
                    DictationController.State.PROCESSING -> FlowBarView.Mode.PROCESSING
                }
                applyPosition()
            }
        }
        scope.launch { controller.level.collect { bar.pushLevel(it) } }
        scope.launch {
            combine(FocusState.editable, controller.state) { typing, state ->
                typing || state != DictationController.State.IDLE
            }.distinctUntilChanged().collect { show ->
                bar.visibility = if (show) View.VISIBLE else View.GONE
            }
        }
        scope.launch {
            try {
                Transcriber.warmUp(applicationContext)
            } catch (e: Exception) {
                Log.e(TAG, "Could not load the speech model", e)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            Prefs.barEnabled = false
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        if (attached) applyPosition()
    }

    override fun onDestroy() {
        running = false
        if (::controller.isInitialized) controller.cancel()
        scope.cancel()
        if (attached) {
            wm.removeView(bar)
            attached = false
        }
        super.onDestroy()
    }

    private fun applyPosition() {
        val dm = resources.displayMetrics
        val w = bar.widthFor(bar.mode)
        val h = bar.barHeight
        params.x = (centerX - w / 2).coerceIn(0, maxOf(0, dm.widthPixels - w))
        params.y = (centerY - h / 2).coerceIn(0, maxOf(0, dm.heightPixels - h))
        if (attached) wm.updateViewLayout(bar, params)
    }

    override fun onStartRequested() {
        if (!controller.start()) toast("Microphone unavailable. Check Handy's microphone permission.")
    }

    override fun onFinishRequested() {
        scope.launch {
            when (val outcome = controller.finish()) {
                is DictationController.Outcome.Text -> deliver(outcome.text)
                DictationController.Outcome.NoSpeech -> toast("Didn't catch that")
                is DictationController.Outcome.Failed -> toast(outcome.message)
            }
        }
    }

    override fun onCancelRequested() = controller.cancel()

    override fun onDrag(dx: Int, dy: Int) {
        centerX += dx
        centerY += dy
        applyPosition()
    }

    override fun onDragEnd() {
        Prefs.barCenterX = centerX
        Prefs.barCenterY = centerY
    }

    private fun deliver(text: String) {
        if (HandyAccessibilityService.insertText(text)) return
        val clipboard = getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText("Handy", text))
        toast("Copied. Turn on Handy in Accessibility settings to insert text automatically.")
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    private fun buildNotification() = NotificationCompat.Builder(this, HandyApplication.CHANNEL_BAR)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle("Handy is ready")
        .setContentText("Tap the floating bar in any text field to dictate.")
        .setOngoing(true)
        .setContentIntent(
            PendingIntent.getActivity(
                this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
            )
        )
        .addAction(
            0,
            "Turn off",
            PendingIntent.getService(
                this, 1, Intent(this, OverlayService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE
            )
        )
        .build()

    companion object {
        private const val TAG = "OverlayService"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_STOP = "com.towkirhs.handy.STOP"

        @Volatile
        var running = false
            private set

        /** Starts the bar. Returns false if a permission is missing or Android refused the start. */
        fun start(context: Context): Boolean {
            val app = context.applicationContext
            if (!Settings.canDrawOverlays(app)) return false
            val micGranted = ContextCompat.checkSelfPermission(app, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
            if (!micGranted) return false
            return try {
                ContextCompat.startForegroundService(app, Intent(app, OverlayService::class.java))
                Prefs.barEnabled = true
                true
            } catch (e: Exception) {
                Log.w(TAG, "Android would not start the bar from here", e)
                false
            }
        }

        fun stop(context: Context) {
            val app = context.applicationContext
            Prefs.barEnabled = false
            app.stopService(Intent(app, OverlayService::class.java))
        }
    }
}
