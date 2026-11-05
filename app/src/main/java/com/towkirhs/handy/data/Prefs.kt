package com.towkirhs.handy.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object Prefs {
    const val DEFAULT_BASE_URL = "https://api.openai.com/v1"
    const val DEFAULT_MODEL = "gpt-4o-mini"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences("handy", Context.MODE_PRIVATE)
    }

    /** User wants the floating bar running (restored when the accessibility service reconnects). */
    var barEnabled: Boolean
        get() = prefs.getBoolean("bar_enabled", false)
        set(value) = prefs.edit { putBoolean("bar_enabled", value) }

    /** Centre of the floating bar in screen pixels; -1 means "use the default spot". */
    var barCenterX: Int
        get() = prefs.getInt("bar_cx", -1)
        set(value) = prefs.edit { putInt("bar_cx", value) }

    var barCenterY: Int
        get() = prefs.getInt("bar_cy", -1)
        set(value) = prefs.edit { putInt("bar_cy", value) }

    const val DEFAULT_STT_MODEL = "gemini-3.8-flash"

    /** Recognise speech with Gemini when online (falls back to the on-device model). */
    var sttGemini: Boolean
        get() = prefs.getBoolean("stt_gemini", false)
        set(value) = prefs.edit { putBoolean("stt_gemini", value) }

    /** Gemini key for speech. Falls back to the grammar key when that one is a Gemini key. */
    var geminiKey: String
        get() = prefs.getString("gemini_key", null)
            ?: if (llmBaseUrl.contains("generativelanguage")) llmApiKey else ""
        set(value) = prefs.edit { putString("gemini_key", value) }

    var sttModel: String
        get() = prefs.getString("stt_model", DEFAULT_STT_MODEL) ?: DEFAULT_STT_MODEL
        set(value) = prefs.edit { putString("stt_model", value) }

    /** Show the Jarvis bubble next to the dictation bar. */
    var jarvisButton: Boolean
        get() = prefs.getBoolean("jarvis_button", true)
        set(value) = prefs.edit { putBoolean("jarvis_button", value) }

    /** Also send every normal dictation (typed into other apps) to Jarvis. */
    var sendAllToJarvis: Boolean
        get() = prefs.getBoolean("send_all_to_jarvis", false)
        set(value) = prefs.edit { putBoolean("send_all_to_jarvis", value) }

    /** Centre of the Jarvis bubble in screen pixels; -1 means "use the default spot". */
    var jarvisCenterX: Int
        get() = prefs.getInt("jarvis_cx", -1)
        set(value) = prefs.edit { putInt("jarvis_cx", value) }

    var jarvisCenterY: Int
        get() = prefs.getInt("jarvis_cy", -1)
        set(value) = prefs.edit { putInt("jarvis_cy", value) }

    /** Grammar fixing on or off. The online key and the on-device model are both optional; this is the master switch. */
    var llmEnabled: Boolean
        get() = prefs.getBoolean("llm_enabled", true)
        set(value) = prefs.edit { putBoolean("llm_enabled", value) }

    var llmBaseUrl: String
        get() = prefs.getString("llm_base_url", DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
        set(value) = prefs.edit { putString("llm_base_url", value) }

    var llmApiKey: String
        get() = prefs.getString("llm_api_key", "") ?: ""
        set(value) = prefs.edit { putString("llm_api_key", value) }

    var llmModel: String
        get() = prefs.getString("llm_model", DEFAULT_MODEL) ?: DEFAULT_MODEL
        set(value) = prefs.edit { putString("llm_model", value) }
}
