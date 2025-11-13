package com.example.agendados.home

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.agendados.R
import com.example.agendados.addclient.AddClientActivity
import com.example.agendados.data.ClientRepository
import com.example.agendados.iterate.IterateActivity
import com.example.agendados.records.RecordsActivity
import com.example.agendados.ui.theme.AgendadosTheme

class HomeActivity : ComponentActivity() {

    private val repository by lazy { ClientRepository.getInstance(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AgendadosTheme {
                HomeScreen(
                    onAddClick = { openAddClient() },
                    onEliminarClick = { showComingSoonToast(R.string.home_option_delete) },
                    onRegistrosClick = { openRecords() },
                    onIterarClick = { openIterate() },
                    onEventosClick = { openUpcomingEvents() },
                    onTicketClick = { showComingSoonToast(R.string.home_option_ticket) }
                )
            }
        }
    }

    private fun openAddClient() {
        startActivity(Intent(this, AddClientActivity::class.java))
    }

    private fun openIterate() {
        if (repository.clients.value.isEmpty()) {
            Toast.makeText(this, R.string.home_no_clients_message, Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(Intent(this, IterateActivity::class.java))
    }

    private fun openRecords() {
        startActivity(Intent(this, RecordsActivity::class.java))
    }

    private fun showComingSoonToast(optionRes: Int) {
        val message = getString(R.string.home_option_coming_soon, getString(optionRes))
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun HomeScreen(
    onAddClick: () -> Unit,
    onEliminarClick: () -> Unit,
    onRegistrosClick: () -> Unit,
    onIterarClick: () -> Unit,
    onEventosClick: () -> Unit,
    onTicketClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(id = R.string.home_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(id = R.string.home_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }

            MenuOption(
                title = stringResource(id = R.string.home_option_add),
                description = stringResource(id = R.string.home_option_add_description),
                onClick = onAddClick
            )
            MenuOption(
                title = stringResource(id = R.string.home_option_delete),
                description = stringResource(id = R.string.home_option_delete_description),
                onClick = onEliminarClick
            )
            MenuOption(
                title = stringResource(id = R.string.home_option_records),
                description = stringResource(id = R.string.home_option_records_description),
                onClick = onRegistrosClick
            )
            MenuOption(
                title = stringResource(id = R.string.home_option_iterate),
                description = stringResource(id = R.string.home_option_iterate_description),
                onClick = onIterarClick
            )
            MenuOption(
                title = stringResource(id = R.string.home_option_events),
                description = stringResource(id = R.string.home_option_events_description),
                onClick = onEventosClick
            )
            MenuOption(
                title = stringResource(id = R.string.home_option_ticket),
                description = stringResource(id = R.string.home_option_pending),
                onClick = onTicketClick
            )
        }
    }
}

@Composable
private fun MenuOption(
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
