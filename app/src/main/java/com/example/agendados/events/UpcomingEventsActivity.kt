package com.example.agendados.events

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.agendados.R
import com.example.agendados.data.ClientRecord
import com.example.agendados.data.ClientRepository
import com.example.agendados.home.HomeActivity
import com.example.agendados.iterate.ClientDetailActivity
import com.example.agendados.ui.components.HomeNavigationButton
import com.example.agendados.ui.theme.AgendadosTheme
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class UpcomingEventsActivity : ComponentActivity() {

    private val repository by lazy { ClientRepository.getInstance(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AgendadosTheme {
                val clients by repository.clients.collectAsState()
                UpcomingEventsScreen(
                    clients = clients,
                    onHomeClick = { openHome() },
                    onDisableAlarm = { id ->
                        repository.updateAlarmStatus(id, false)
                        Toast.makeText(this, R.string.upcoming_events_alarm_disabled, Toast.LENGTH_SHORT).show()
                    },
                    onOpenDetail = { openDetail(it) }
                )
            }
        }
    }

    private fun openHome() {
        val intent = Intent(this, HomeActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(intent)
        finish()
    }

    private fun openDetail(id: String) {
        val intent = Intent(this, ClientDetailActivity::class.java)
            .putExtra(ClientDetailActivity.EXTRA_CLIENT_ID, id)
        startActivity(intent)
    }
}

@Composable
fun UpcomingEventsScreen(
    clients: List<ClientRecord>,
    onHomeClick: () -> Unit,
    onDisableAlarm: (String) -> Unit,
    onOpenDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val upcoming = remember(clients) { filterUpcoming(clients) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault()) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault()) }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            HomeNavigationButton(onClick = onHomeClick)
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(id = R.string.upcoming_events_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(id = R.string.upcoming_events_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
            if (upcoming.isEmpty()) {
                Text(
                    text = stringResource(id = R.string.upcoming_events_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(upcoming) { record ->
                        UpcomingEventCard(
                            record = record,
                            dateFormatter = dateFormatter,
                            timeFormatter = timeFormatter,
                            onDisableAlarm = { onDisableAlarm(record.id) },
                            onOpenDetail = { onOpenDetail(record.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UpcomingEventCard(
    record: ClientRecord,
    dateFormatter: DateTimeFormatter,
    timeFormatter: DateTimeFormatter,
    onDisableAlarm: () -> Unit,
    onOpenDetail: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = record.nombre.ifBlank { stringResource(id = R.string.client_detail_unknown_name) },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(id = R.string.records_phone, formatPhoneNumber(record.celular)),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(
                    id = R.string.upcoming_events_schedule,
                    record.scheduledDate.format(dateFormatter),
                    record.scheduledTime.format(timeFormatter)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (record.comentarios.isNotBlank()) {
                Text(
                    text = stringResource(id = R.string.upcoming_events_comment, record.comentarios),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(onClick = onOpenDetail, modifier = Modifier.weight(1f)) {
                    Text(text = stringResource(id = R.string.upcoming_events_view_detail))
                }
                Button(onClick = onDisableAlarm, modifier = Modifier.weight(1f)) {
                    Text(text = stringResource(id = R.string.upcoming_events_disable_button), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun filterUpcoming(clients: List<ClientRecord>): List<ClientRecord> {
    val now = LocalDateTime.now()
    return clients.filter { record ->
        if (!record.alarmActive) {
            false
        } else {
            val scheduled = LocalDateTime.of(record.scheduledDate, record.scheduledTime)
            !scheduled.isBefore(now)
        }
    }
}

private fun formatPhoneNumber(raw: String): String {
    val digits = raw.filter { it.isDigit() }
    return if (digits.length == 9) digits.chunked(3).joinToString(" ") else raw
}
