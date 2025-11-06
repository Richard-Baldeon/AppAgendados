package com.example.agendados.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.agendados.AlarmActivity
import com.example.agendados.R

private const val CHANNEL_ID = "agendados_alarm_channel"
private const val NOTIFICATION_ID = 93450

object AlarmNotifications {

    fun showAlarmNotification(context: Context, payload: AlarmPayload) {
        val appContext = context.applicationContext
        val notificationManager = ContextCompat.getSystemService(
            appContext,
            NotificationManager::class.java
        ) ?: return

        createChannelIfNeeded(notificationManager, appContext)

        val alarmIntent = Intent(appContext, AlarmActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(EXTRA_CONTACT_NAME, payload.contactName)
            putExtra(EXTRA_AMOUNT, payload.amount)
            putExtra(EXTRA_PHONE_NUMBER, payload.phoneNumber)
        }

        val fullScreenIntent = PendingIntent.getActivity(
            appContext,
            0,
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(
                appContext.getString(R.string.alarm_notification_title, payload.contactName)
            )
            .setContentText(
                appContext.getString(R.string.alarm_notification_body, payload.amount)
            )
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenIntent, true)
            .setContentIntent(fullScreenIntent)
            .build()

        NotificationManagerCompat.from(appContext).notify(NOTIFICATION_ID, notification)
    }

    fun cancelAlarmNotification(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    private fun createChannelIfNeeded(manager: NotificationManager, context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.alarm_notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.alarm_notification_channel_description)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            manager.createNotificationChannel(channel)
        }
    }
}
