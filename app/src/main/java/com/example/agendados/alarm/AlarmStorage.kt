package com.example.agendados.alarm

import android.content.Context

private const val PREFS_NAME = "alarm_storage"
private const val KEY_TRIGGER_AT = "key_trigger_at"

object AlarmStorage {

    fun saveAlarmTime(context: Context, triggerAtMillis: Long) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_TRIGGER_AT, triggerAtMillis)
            .apply()
    }

    fun getAlarmTime(context: Context): Long? {
        val stored = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_TRIGGER_AT, -1L)
        return if (stored > 0L) stored else null
    }

    fun clearAlarmTime(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_TRIGGER_AT)
            .apply()
    }
}
