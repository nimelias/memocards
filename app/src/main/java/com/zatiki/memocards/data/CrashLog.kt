package com.zatiki.memocards.data

import android.app.Application
import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CrashLog {
    private const val PREF = "memocards_crash"
    private const val KEY = "events"
    private const val LEGACY_KEY = "last"
    private const val MAX_CHARS = 96_000
    private const val MAX_EVENTS = 120

    fun install(app: Application) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching {
                record(app, "uncaught thread=${thread.name}\n${error.toPostmortem()}")
            }
            previous?.uncaughtException(thread, error)
        }
    }

    fun record(context: Context, text: String) {
        val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
        val event = "[$ts]\n${text.trim()}".trim()
        val previous = events(context)
        val joined = trimLog(listOf(event) + previous)
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, joined)
            .remove(LEGACY_KEY)
            .apply()
    }

    fun read(context: Context): String? =
        events(context).joinToString("\n\n---\n\n").takeIf { it.isNotBlank() }

    fun clear(context: Context) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY)
            .remove(LEGACY_KEY)
            .apply()
    }

    fun eventCount(context: Context): Int = events(context).size

    private fun events(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val stored = prefs.getString(KEY, null)
        val legacy = prefs.getString(LEGACY_KEY, null)
        val raw = when {
            !stored.isNullOrBlank() -> stored
            !legacy.isNullOrBlank() -> legacy
            else -> return emptyList()
        }
        return raw.split("\n\n---\n\n").map { it.trim() }.filter { it.isNotEmpty() }
    }

    private fun trimLog(events: List<String>): String {
        val kept = events.filter { it.isNotBlank() }.take(MAX_EVENTS).toMutableList()
        var joined = kept.joinToString("\n\n---\n\n")
        while (joined.length > MAX_CHARS && kept.size > 1) {
            kept.removeAt(kept.lastIndex)
            joined = kept.joinToString("\n\n---\n\n")
        }
        return joined.take(MAX_CHARS)
    }
}
