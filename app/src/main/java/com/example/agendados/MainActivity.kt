package com.example.agendados

import android.Manifest
import android.app.AlarmManager
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.agendados.alarm.AlarmPayload
import com.example.agendados.alarm.AlarmScheduler
import com.example.agendados.alarm.AlarmStorage
import com.example.agendados.ui.theme.AgendadosTheme
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_TRIGGER_CALL = "extra_trigger_call"
    }

    private val phoneNumber: String by lazy { getString(R.string.phone_number) }
    private val fichaState = mutableStateOf(FichaUiState())

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startPhoneCall()
            } else {
                Toast.makeText(
                    this,
                    R.string.call_permission_required,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                Toast.makeText(
                    this,
                    R.string.notification_permission_required,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        fichaState.value = fichaState.value.copy(
            scheduledTimeMillis = AlarmStorage.getAlarmTime(this)
        )

        setContent {
            AgendadosTheme {
                val uiState by fichaState
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FichaScreen(
                        phoneNumber = stringResource(id = R.string.phone_number),
                        contactName = stringResource(id = R.string.contact_name_label_value),
                        amount = stringResource(id = R.string.amount_label_value),
                        rate = stringResource(id = R.string.rate_label_value),
                        debt = stringResource(id = R.string.debt_label_value),
                        appVersion = getAppVersionName(),
                        selectedTime = uiState.selectedTime,
                        scheduledTimeMillis = uiState.scheduledTimeMillis,
                        onCallClick = { initiateCall() },
                        onSelectAlarmTime = {
                            showTimePicker(uiState.selectedTime) { chosenTime ->
                                fichaState.value = fichaState.value.copy(selectedTime = chosenTime)
                            }
                        },
                        onSaveAlarm = {
                            val selectedTime = fichaState.value.selectedTime
                            if (selectedTime == null) {
                                Toast.makeText(
                                    this@MainActivity,
                                    R.string.select_time_prompt,
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                val triggerAtMillis = computeNextTriggerTime(selectedTime)
                                val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                                    Toast.makeText(
                                        this@MainActivity,
                                        R.string.exact_alarm_permission_required,
                                        Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    AlarmScheduler.scheduleExactAlarm(
                                        context = this@MainActivity,
                                        triggerAtMillis = triggerAtMillis,
                                        payload = buildAlarmPayload()
                                    )
                                    AlarmStorage.saveAlarmTime(this@MainActivity, triggerAtMillis)
                                    fichaState.value = fichaState.value.copy(
                                        scheduledTimeMillis = triggerAtMillis
                                    )
                                    Toast.makeText(
                                        this@MainActivity,
                                        R.string.alarm_saved_confirmation,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    )
                }
            }
        }

        handleCallRequest(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleCallRequest(intent)
    }

    override fun onResume() {
        super.onResume()
        val currentIntent = intent
        val storedAlarmTime = AlarmStorage.getAlarmTime(this)
        fichaState.value = fichaState.value.copy(scheduledTimeMillis = storedAlarmTime)
        handleCallRequest(currentIntent)
    }

    private fun handleCallRequest(intent: Intent?) {
        intent ?: return
        if (intent.getBooleanExtra(EXTRA_TRIGGER_CALL, false)) {
            initiateCall()
            intent.removeExtra(EXTRA_TRIGGER_CALL)
            setIntent(intent)
        }
    }

    private fun buildAlarmPayload(): AlarmPayload {
        return AlarmPayload(
            contactName = getString(R.string.contact_name_label_value),
            amount = getString(R.string.amount_label_value),
            phoneNumber = phoneNumber
        )
    }

    private fun computeNextTriggerTime(selectedTime: LocalTime): Long {
        val now = LocalDateTime.now()
        var nextOccurrence = now.withHour(selectedTime.hour)
            .withMinute(selectedTime.minute)
            .withSecond(0)
            .withNano(0)
        if (nextOccurrence.isBefore(now)) {
            nextOccurrence = nextOccurrence.plusDays(1)
        }
        return nextOccurrence.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun showTimePicker(
        currentSelection: LocalTime?,
        onTimePicked: (LocalTime) -> Unit
    ) {
        val initial = currentSelection ?: LocalTime.now()
        TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                onTimePicked(LocalTime.of(hourOfDay, minute))
            },
            initial.hour,
            initial.minute,
            true
        ).show()
    }

    private fun initiateCall() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED -> {
                startPhoneCall()
            }

            shouldShowRequestPermissionRationale(Manifest.permission.CALL_PHONE) -> {
                Toast.makeText(
                    this,
                    R.string.call_permission_required,
                    Toast.LENGTH_SHORT
                ).show()
                requestPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
            }

            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
            }
        }
    }

    private fun startPhoneCall() {
        val intent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse("tel:$phoneNumber")
        }
        startActivity(intent)
        keepAppInForeground()
    }

    private fun keepAppInForeground() {
        Handler(Looper.getMainLooper()).postDelayed({
            val resumeIntent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(resumeIntent)
        }, 500)
    }

    private fun getAppVersionName(): String {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                val legacyInfo = packageManager.getPackageInfo(packageName, 0)
                legacyInfo
            }
            packageInfo.versionName ?: ""
        } catch (ignored: PackageManager.NameNotFoundException) {
            ""
        }
    }
}

@Composable
fun FichaScreen(
    phoneNumber: String,
    contactName: String,
    amount: String,
    rate: String,
    debt: String,
    appVersion: String,
    selectedTime: LocalTime?,
    scheduledTimeMillis: Long?,
    onCallClick: () -> Unit,
    onSelectAlarmTime: () -> Unit,
    onSaveAlarm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("HH:mm")
    }
    val scheduledTimeText = remember(scheduledTimeMillis) {
        scheduledTimeMillis?.let {
            val dateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(it),
                ZoneId.systemDefault()
            )
            dateTime.format(dateFormatter)
        }
    }
    val selectedTimeText = remember(selectedTime) {
        selectedTime?.format(dateFormatter)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.clip(RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = contactName,
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = phoneNumber,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(id = R.string.amount_label, amount),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(id = R.string.rate_label, rate),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(id = R.string.debt_label, debt),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(onClick = onSelectAlarmTime) {
                    Text(text = stringResource(id = R.string.select_time_button))
                }
                Text(
                    text = selectedTimeText?.let { stringResource(id = R.string.selected_time_label, it) }
                        ?: stringResource(id = R.string.no_time_selected_label),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Button(onClick = onSaveAlarm) {
                    Text(text = stringResource(id = R.string.save_alarm_button))
                }
                Text(
                    text = scheduledTimeText?.let { stringResource(id = R.string.alarm_scheduled_label, it) }
                        ?: stringResource(id = R.string.no_alarm_scheduled_label),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }

            Button(onClick = onCallClick) {
                Text(text = stringResource(id = R.string.call_button))
            }
        }

        Text(
            text = stringResource(id = R.string.version_label, appVersion),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.align(Alignment.BottomCenter),
            textAlign = TextAlign.Center
        )
    }
}

data class FichaUiState(
    val selectedTime: LocalTime? = null,
    val scheduledTimeMillis: Long? = null
)
