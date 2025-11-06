package com.example.agendados.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.agendados.AlarmActivity

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        AlarmStorage.clearAlarmTime(appContext)
        val contactName = intent.getStringExtra(EXTRA_CONTACT_NAME) ?: ""
        val amount = intent.getStringExtra(EXTRA_AMOUNT) ?: ""
        val phoneNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER) ?: ""
        val payload = AlarmPayload(contactName, amount, phoneNumber)

        AlarmNotifications.showAlarmNotification(appContext, payload)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val launchIntent = Intent(appContext, AlarmActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(EXTRA_CONTACT_NAME, contactName)
                putExtra(EXTRA_AMOUNT, amount)
                putExtra(EXTRA_PHONE_NUMBER, phoneNumber)
            }
            appContext.startActivity(launchIntent)
        }
    }
}
