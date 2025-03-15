package fr.hnit.babyname

import android.util.Log

object Log {
    private fun contextString(context: Any): String {
        if (context is String) {
            return context
        } else {
            return context::class.java.name.substringAfterLast('.')
        }
    }

    // debug
    fun d(context: Any, message: String) {
        if (BuildConfig.DEBUG) {
            val tag = contextString(context)
            Log.d(tag, message)
        }
    }

    // info
    fun i(context: Any, message: String) {
        if (BuildConfig.DEBUG) {
            val tag = contextString(context)
            Log.i(tag, message)
        }
    }

    // warning
    fun w(context: Any, message: String) {
        if (BuildConfig.DEBUG) {
            val tag = contextString(context)
            Log.w(tag, message)
        }
    }

    // error
    fun e(context: Any, message: String) {
        val tag = contextString(context)
        Log.e(tag, message)
    }
}
