package com.example.agendados

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.agendados.alarm.AlarmNotifications
import com.example.agendados.alarm.AlarmPayload
import com.example.agendados.alarm.AlarmScheduler
import com.example.agendados.alarm.AlarmStorage
import com.example.agendados.alarm.EXTRA_AMOUNT
import com.example.agendados.alarm.EXTRA_CONTACT_NAME
import com.example.agendados.alarm.EXTRA_PHONE_NUMBER
import com.example.agendados.home.HomeActivity
import com.example.agendados.ui.theme.AgendadosTheme
import java.util.concurrent.TimeUnit

class AlarmActivity : ComponentActivity() {

    companion object {
        private const val SNOOZE_MINUTES = 10L
    }

    private var ringtone: Ringtone? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        makeVisibleOverLockscreen()

        val contactName = intent.getStringExtra(EXTRA_CONTACT_NAME) ?: ""
        val amount = intent.getStringExtra(EXTRA_AMOUNT) ?: ""
        val phoneNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER) ?: ""
        val payload = AlarmPayload(contactName, amount, phoneNumber)

        AlarmNotifications.cancelAlarmNotification(this)
        startAlarmSound()

        setContent {
            AgendadosTheme {
                AlarmScreen(
                    contactName = contactName,
                    amount = amount,
                    phoneNumber = phoneNumber,
                    onSnooze = {
                        scheduleSnooze(payload)
                    },
                    onDismiss = {
                        dismissAlarm(payload)
                    },
                    onCall = {
                        handleCall(payload)
                    },
                    onHome = {
                        navigateHome(payload)
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        AlarmNotifications.cancelAlarmNotification(this)
        stopAlarmSound()
        super.onDestroy()
    }

    private fun scheduleSnooze(payload: AlarmPayload) {
        val triggerAt = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(SNOOZE_MINUTES)
        stopAlarmSound()
        AlarmNotifications.cancelAlarmNotification(this)
        AlarmScheduler.scheduleExactAlarm(this, triggerAt, payload)
        AlarmStorage.saveAlarmTime(this, triggerAt)
        Toast.makeText(this, R.string.snooze_confirmation, Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun dismissAlarm(payload: AlarmPayload) {
        clearAlarmState(payload, showDismissedToast = true)
        finish()
    }

    private fun handleCall(payload: AlarmPayload) {
        AlarmScheduler.cancelAlarm(this, payload)
        AlarmStorage.clearAlarmTime(this)
        stopAlarmSound()
        AlarmNotifications.cancelAlarmNotification(this)
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(MainActivity.EXTRA_TRIGGER_CALL, true)
        }
        startActivity(mainIntent)
        finish()
    }

    private fun navigateHome(payload: AlarmPayload) {
        clearAlarmState(payload, showDismissedToast = false)
        val intent = Intent(this, HomeActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(intent)
        finish()
    }

    private fun clearAlarmState(payload: AlarmPayload, showDismissedToast: Boolean) {
        stopAlarmSound()
        AlarmNotifications.cancelAlarmNotification(this)
        AlarmScheduler.cancelAlarm(this, payload)
        AlarmStorage.clearAlarmTime(this)
        if (showDismissedToast) {
            Toast.makeText(this, R.string.alarm_dismissed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun startAlarmSound() {
        val toneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        ringtone = RingtoneManager.getRingtone(applicationContext, toneUri)?.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                isLooping = true
            }
            audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            play()
        }
    }

    private fun stopAlarmSound() {
        ringtone?.stop()
        ringtone = null
    }

    private fun makeVisibleOverLockscreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }
}
