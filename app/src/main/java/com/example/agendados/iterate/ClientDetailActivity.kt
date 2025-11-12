package com.example.agendados.iterate

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.agendados.R
import com.example.agendados.data.ClientRecord
import com.example.agendados.data.ClientRepository
import com.example.agendados.home.HomeActivity
import com.example.agendados.ui.components.HomeNavigationButton
import com.example.agendados.ui.theme.AgendadosTheme
import java.time.format.DateTimeFormatter
import java.util.Locale

class ClientDetailActivity : ComponentActivity() {

    private val repository by lazy { ClientRepository.getInstance(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val clientId = intent.getStringExtra(EXTRA_CLIENT_ID)
        if (clientId.isNullOrBlank()) {
            finish()
            return
        }
        setContent {
            AgendadosTheme {
                val clients by repository.clients.collectAsState()
                val record = clients.firstOrNull { it.id == clientId }
                ClientDetailScreen(
                    record = record,
                    onHomeClick = { openHome() },
                    onCallClick = { startDial(it) }
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

    private fun startDial(phone: String) {
        val digits = phone.filter { it.isDigit() }
        if (digits.isEmpty()) return
        val uri = Uri.parse("tel:$digits")
        startActivity(Intent(Intent.ACTION_DIAL, uri))
    }

    companion object {
        const val EXTRA_CLIENT_ID = "client_id"
    }
}

@Composable
private fun ClientDetailScreen(
    record: ClientRecord?,
    onHomeClick: () -> Unit,
    onCallClick: (String) -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        val scrollState = rememberScrollState()
        val dateFormatter = rememberDetailDateFormatter()
        val timeFormatter = rememberDetailTimeFormatter()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            HomeNavigationButton(onClick = onHomeClick)
            Text(
                text = stringResource(id = R.string.client_detail_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            if (record == null) {
                Text(
                    text = stringResource(id = R.string.client_detail_not_found),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                DetailTextRow(
                    label = stringResource(id = R.string.client_detail_phone_label),
                    value = formatPhoneNumber(record.celular)
                )
                DetailTextRow(
                    label = stringResource(id = R.string.client_detail_schedule_label),
                    value = stringResource(
                        id = R.string.client_detail_schedule,
                        record.scheduledDate.format(dateFormatter),
                        record.scheduledTime.format(timeFormatter)
                    )
                )
                DetailTextRow(
                    label = stringResource(id = R.string.client_detail_amount_pp_label),
                    value = record.montoPP.ifBlank { stringResource(id = R.string.client_detail_placeholder) }
                )
                DetailTextRow(
                    label = stringResource(id = R.string.client_detail_rate_pp_label),
                    value = record.tasaPP.ifBlank { stringResource(id = R.string.client_detail_placeholder) }
                )
                DetailTextRow(
                    label = stringResource(id = R.string.client_detail_debt_label),
                    value = record.deuda.ifBlank { stringResource(id = R.string.client_detail_placeholder) }
                )
                DetailTextRow(
                    label = stringResource(id = R.string.client_detail_amount_cd_label),
                    value = record.montoCD.ifBlank { stringResource(id = R.string.client_detail_placeholder) }
                )
                DetailTextRow(
                    label = stringResource(id = R.string.client_detail_rate_cd_label),
                    value = record.tasaCD.ifBlank { stringResource(id = R.string.client_detail_placeholder) }
                )
                Text(
                    text = stringResource(id = R.string.client_detail_comments_label),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = record.comentarios.ifBlank { stringResource(id = R.string.client_detail_no_comments) },
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = { onCallClick(record.celular) }, modifier = Modifier.fillMaxWidth()) {
                    Text(text = stringResource(id = R.string.call_button), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DetailTextRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
        Text(text = label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun rememberDetailDateFormatter(): DateTimeFormatter {
    return remember { DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault()) }
}

@Composable
private fun rememberDetailTimeFormatter(): DateTimeFormatter {
    return remember { DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault()) }
}

private fun formatPhoneNumber(raw: String): String {
    val digits = raw.filter { it.isDigit() }
    return if (digits.length == 9) digits.chunked(3).joinToString(" ") else raw
}
