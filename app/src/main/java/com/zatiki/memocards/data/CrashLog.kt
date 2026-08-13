package com.zatiki.memocards.data

import android.app.Application
import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CrashLog {
    private const val PREF = "memocards_crash"
    private const val KEY = "last"

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
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, "[$ts]\n$text".take(8000))
            .apply()
    }

    fun read(context: Context): String? =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString(KEY, null)
            ?.takeIf { it.isNotBlank() }
}
