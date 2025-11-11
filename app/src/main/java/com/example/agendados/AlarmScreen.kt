package com.example.agendados

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.agendados.ui.components.HomeNavigationButton

@Composable
fun AlarmScreen(
    contactName: String,
    amount: String,
    phoneNumber: String,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit,
    onCall: () -> Unit,
    onHome: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            HomeNavigationButton(
                onClick = onHome,
                modifier = Modifier.align(Alignment.TopStart)
            )

            Column(
                modifier = Modifier.align(Alignment.Center),
                verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            Text(
                text = stringResource(id = R.string.alarm_screen_title),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(id = R.string.alarm_message_name, contactName),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(id = R.string.alarm_message_amount, amount),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(id = R.string.alarm_phone_number_label, phoneNumber),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )

            Column(
                modifier = Modifier.align(Alignment.Center),
                verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(id = R.string.alarm_screen_title),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(id = R.string.alarm_message_name, contactName),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(id = R.string.alarm_message_amount, amount),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(id = R.string.alarm_phone_number_label, phoneNumber),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OutlinedButton(onClick = onSnooze) {
                        Text(text = stringResource(id = R.string.snooze_button))
                    }
                    OutlinedButton(onClick = onDismiss) {
                        Text(text = stringResource(id = R.string.dismiss_button))
                    }
                    Button(onClick = onCall) {
                        Text(text = stringResource(id = R.string.call_button))
                    }
                }
            }
        }
    }
}
