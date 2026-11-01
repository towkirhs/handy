package com.towkirhs.handy.access

import android.accessibilityservice.AccessibilityService
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.towkirhs.handy.data.Prefs
import com.towkirhs.handy.overlay.OverlayService

/**
 * Does two small jobs: tells the floating bar when a text field is focused, and puts dictated
 * text into that field. It only looks at the focused field (to find the cursor), nothing else.
 */
class HandyAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        if (Prefs.barEnabled && !OverlayService.running) OverlayService.start(this)
        updateFocus()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val source = event?.source
        if (event?.eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED &&
            source != null && source.isEditable && !source.isPassword
        ) {
            FocusState.editable.value = true // the event already names the focused field
            return
        }
        updateFocus()
    }

    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        FocusState.editable.value = false
        return super.onUnbind(intent)
    }

    private fun focusedField(): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null
        val node = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)?.takeIf { it.isEditable }
            ?: findEditableFocus(root)
        return node?.takeIf { !it.isPassword }
    }

    /** Jetpack Compose fields don't answer findFocus, so walk the screen looking for the focused text field. */
    private fun findEditableFocus(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var visited = 0
        while (queue.isNotEmpty() && visited < MAX_NODES) {
            val node = queue.removeFirst()
            visited++
            if (node.isEditable && node.isFocused) return node
            for (i in 0 until node.childCount) node.getChild(i)?.let { queue.add(it) }
        }
        return null
    }

    private fun updateFocus() {
        FocusState.editable.value = focusedField() != null
    }

    private fun insert(text: String): Boolean {
        val node = focusedField() ?: return false
        val current = if (node.isShowingHintText) "" else node.text?.toString().orEmpty()
        val rawStart = node.textSelectionStart
        val start = (if (rawStart >= 0) rawStart else current.length).coerceIn(0, current.length)
        val rawEnd = node.textSelectionEnd
        val end = (if (rawEnd >= 0) rawEnd else start).coerceIn(start, current.length)
        val needsSpace = start > 0 && !current[start - 1].isWhitespace()
        val payload = (if (needsSpace) " " else "") + text

        // Paste works in the widest range of apps (including web views and custom editors).
        val clipboard = getSystemService(ClipboardManager::class.java)
        val clip = ClipData.newPlainText("Handy", payload)
        if (Build.VERSION.SDK_INT >= 33) {
            clip.description.extras = PersistableBundle().apply {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true) // hides the clipboard preview popup
            }
        }
        clipboard.setPrimaryClip(clip)
        if (node.performAction(AccessibilityNodeInfo.ACTION_PASTE)) return true

        // Fallback: write the whole field value ourselves.
        val args = Bundle().apply {
            putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                current.substring(0, start) + payload + current.substring(end)
            )
        }
        if (!node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)) return false
        val caret = start + payload.length
        node.performAction(
            AccessibilityNodeInfo.ACTION_SET_SELECTION,
            Bundle().apply {
                putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, caret)
                putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, caret)
            }
        )
        return true
    }

    companion object {
        private const val MAX_NODES = 400

        @Volatile
        private var instance: HandyAccessibilityService? = null

        /** Inserts [text] at the cursor of the focused field. Returns false if the service is off or nothing is focused. */
        fun insertText(text: String): Boolean = instance?.insert(text) ?: false
    }
}
