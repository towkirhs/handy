package com.towkirhs.handy.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject

/** The last few dictations, stored only on this device. */
object History {
    data class Entry(val text: String, val time: Long)

    private const val KEY = "entries"
    private const val MAX = 50

    private lateinit var prefs: SharedPreferences
    private val _entries = MutableStateFlow<List<Entry>>(emptyList())
    val entries: StateFlow<List<Entry>> = _entries

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences("handy_history", Context.MODE_PRIVATE)
        _entries.value = load()
    }

    fun add(text: String) {
        val list = (listOf(Entry(text, System.currentTimeMillis())) + _entries.value).take(MAX)
        _entries.value = list
        save(list)
    }

    fun clear() {
        _entries.value = emptyList()
        save(emptyList())
    }

    private fun load(): List<Entry> = try {
        val array = JSONArray(prefs.getString(KEY, "[]"))
        List(array.length()) {
            val o = array.getJSONObject(it)
            Entry(o.getString("t"), o.getLong("ts"))
        }
    } catch (e: Exception) {
        emptyList()
    }

    private fun save(list: List<Entry>) {
        val array = JSONArray()
        list.forEach { array.put(JSONObject().put("t", it.text).put("ts", it.time)) }
        prefs.edit { putString(KEY, array.toString()) }
    }
}
