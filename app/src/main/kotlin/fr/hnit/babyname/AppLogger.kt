package fr.hnit.babyname

import android.util.Log

object AppLogger {
    private fun contextString(context: Any): String {
        if (context is String) {
            return context
        } else {
            return context::class.java.name.substringAfterLast('.')
        }
    }

    fun d(context: Any, message: String) {
        if (BuildConfig.DEBUG) {
            val tag = contextString(context)
            Log.d(tag, message)
        }
    }

    fun i(context: Any, message: String) {
        if (BuildConfig.DEBUG) {
            val tag = contextString(context)
            Log.i(tag, message)
        }
    }

    fun w(context: Any, message: String) {
        if (BuildConfig.DEBUG) {
            val tag = contextString(context)
            Log.w(tag, message)
        }
    }

    fun e(context: Any, message: String) {
        val tag = contextString(context)
        Log.e(tag, message)
    }
}
