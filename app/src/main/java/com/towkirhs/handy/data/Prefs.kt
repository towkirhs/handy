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

    /** Also send every finished dictation to the Jarvis app (does nothing if it isn't installed). */
    var sendToJarvis: Boolean
        get() = prefs.getBoolean("send_to_jarvis", true)
        set(value) = prefs.edit { putBoolean("send_to_jarvis", value) }

    var llmEnabled: Boolean
        get() = prefs.getBoolean("llm_enabled", false)
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
