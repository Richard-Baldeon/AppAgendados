package com.example.agendados.records

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class RecordsActivity : ComponentActivity() {

    private val repository by lazy { ClientRepository.getInstance(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AgendadosTheme {
                val clients by repository.clients.collectAsState()
                RecordsScreen(
                    clients = clients,
                    onClientClick = { openDetail(it.id) },
                    onHomeClick = { openHome() }
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

    private fun openDetail(clientId: String) {
        val intent = Intent(this, ClientDetailActivity::class.java)
            .putExtra(ClientDetailActivity.EXTRA_CLIENT_ID, clientId)
        startActivity(intent)
    }
}

@Composable
fun RecordsScreen(
    clients: List<ClientRecord>,
    onClientClick: (ClientRecord) -> Unit,
    onHomeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = modifier.fillMaxSize()) {
    val dateFormatter = rememberRecordsDateFormatter()
    val timeFormatter = rememberRecordsTimeFormatter()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            HomeNavigationButton(onClick = onHomeClick)
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(id = R.string.records_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(id = R.string.records_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }

            if (clients.isEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(id = R.string.records_empty_message),
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
                    items(clients) { client ->
                        RecordListItem(
                            record = client,
                            dateFormatter = dateFormatter,
                            timeFormatter = timeFormatter,
                            onClick = { onClientClick(client) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordListItem(
    record: ClientRecord,
    dateFormatter: DateTimeFormatter,
    timeFormatter: DateTimeFormatter,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val createdAt = remember(record.createdAtMillis) {
                Instant.ofEpochMilli(record.createdAtMillis).atZone(ZoneId.systemDefault())
            }
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
                    id = R.string.records_schedule,
                    record.scheduledDate.format(dateFormatter),
                    record.scheduledTime.format(timeFormatter)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(
                    id = R.string.records_created_date,
                    createdAt.format(dateFormatter)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(
                    id = R.string.records_created_time,
                    createdAt.format(timeFormatter)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun rememberRecordsDateFormatter(): DateTimeFormatter {
    return remember { DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault()) }
}

@Composable
private fun rememberRecordsTimeFormatter(): DateTimeFormatter {
    return remember { DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault()) }
}

private fun formatPhoneNumber(raw: String): String {
    val digits = raw.filter { it.isDigit() }
    return if (digits.length == 9) digits.chunked(3).joinToString(" ") else raw
}
