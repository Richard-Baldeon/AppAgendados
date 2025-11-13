package com.example.agendados.iterate

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.example.agendados.ui.components.HomeNavigationButton
import com.example.agendados.ui.theme.AgendadosTheme

class IterateActivity : ComponentActivity() {

    private val repository by lazy { ClientRepository.getInstance(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AgendadosTheme {
                val clients by repository.clients.collectAsState()
                IterateScreen(
                    clients = clients,
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
}

@Composable
fun IterateScreen(
    clients: List<ClientRecord>,
    onHomeClick: () -> Unit,
    onCallClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
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
                    text = stringResource(id = R.string.iterate_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(id = R.string.iterate_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }

            var currentIndex by rememberSaveable { mutableStateOf(0) }
            LaunchedEffect(clients.size) {
                if (clients.isEmpty()) {
                    currentIndex = 0
                } else if (currentIndex > clients.lastIndex) {
                    currentIndex = clients.lastIndex
                }
            }

            if (clients.isEmpty()) {
                Text(
                    text = stringResource(id = R.string.iterate_empty_message),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                val record = clients[currentIndex]
                val scrollState = rememberScrollState()
                Text(
                    text = stringResource(
                        id = R.string.iterate_position_indicator,
                        currentIndex + 1,
                        clients.size
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth()
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    ClientDetailCard(
                        record = record,
                        onCallClick = onCallClick
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = { if (currentIndex > 0) currentIndex-- },
                        enabled = currentIndex > 0,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = stringResource(id = R.string.iterate_previous_button))
                    }
                    Button(
                        onClick = { if (currentIndex < clients.lastIndex) currentIndex++ },
                        enabled = currentIndex < clients.lastIndex,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = stringResource(id = R.string.iterate_next_button),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
