package com.example.agendados.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.agendados.MainActivity

const val EXTRA_CONTACT_NAME = "extra_contact_name"
const val EXTRA_AMOUNT = "extra_amount"
const val EXTRA_PHONE_NUMBER = "extra_phone_number"
const val EXTRA_TRIGGER_AT = "extra_trigger_at"

private const val ALARM_REQUEST_CODE = 9345

data class AlarmPayload(
    val contactName: String,
    val amount: String,
    val phoneNumber: String
)

object AlarmScheduler {

    fun scheduleExactAlarm(
        context: Context,
        triggerAtMillis: Long,
        payload: AlarmPayload
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmIntent = createAlarmIntent(context, payload, triggerAtMillis)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val showIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent)
        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
    }

    fun cancelAlarm(context: Context, payload: AlarmPayload) {
        val alarmIntent = createAlarmIntent(context, payload, triggerAtMillis = 0L)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            alarmIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
        }
    }

    private fun createAlarmIntent(
        context: Context,
        payload: AlarmPayload,
        triggerAtMillis: Long
    ): Intent {
        return Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_CONTACT_NAME, payload.contactName)
            putExtra(EXTRA_AMOUNT, payload.amount)
            putExtra(EXTRA_PHONE_NUMBER, payload.phoneNumber)
            putExtra(EXTRA_TRIGGER_AT, triggerAtMillis)
        }
    }
}
