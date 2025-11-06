package com.example.agendados.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.agendados.AlarmActivity

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        AlarmStorage.clearAlarmTime(context)
        val launchIntent = Intent(context, AlarmActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(EXTRA_CONTACT_NAME, intent.getStringExtra(EXTRA_CONTACT_NAME))
            putExtra(EXTRA_AMOUNT, intent.getStringExtra(EXTRA_AMOUNT))
            putExtra(EXTRA_PHONE_NUMBER, intent.getStringExtra(EXTRA_PHONE_NUMBER))
        }
        context.startActivity(launchIntent)
    }
}
