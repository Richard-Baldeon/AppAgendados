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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.agendados.addclient.AddClientActivity
import com.example.agendados.ui.theme.AgendadosTheme
import java.util.Locale

private const val HOME_TITLE = "App Agendados"
private const val HOME_SUBTITLE = "Selecciona una opción para continuar"
private const val HOME_OPTION_ADD = "Añadir"
private const val HOME_OPTION_ADD_DESCRIPTION = "Registrar un nuevo cliente"
private const val HOME_OPTION_DELETE = "Eliminar"
private const val HOME_OPTION_ITERATE = "Iterar"
private const val HOME_OPTION_TICKET = "Ticket"
private const val HOME_OPTION_PENDING = "Disponible próximamente"
private const val HOME_OPTION_COMING_SOON_TEMPLATE = "La opción %s estará disponible próximamente."

class HomeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AgendadosTheme {
                HomeScreen(
                    title = HOME_TITLE,
                    subtitle = HOME_SUBTITLE,
                    addTitle = HOME_OPTION_ADD,
                    addDescription = HOME_OPTION_ADD_DESCRIPTION,
                    deleteTitle = HOME_OPTION_DELETE,
                    iterateTitle = HOME_OPTION_ITERATE,
                    ticketTitle = HOME_OPTION_TICKET,
                    pendingDescription = HOME_OPTION_PENDING,
                    onAddClick = { openAddClient() },
                    onEliminarClick = { showComingSoonToast(HOME_OPTION_DELETE) },
                    onIterarClick = { showComingSoonToast(HOME_OPTION_ITERATE) },
                    onTicketClick = { showComingSoonToast(HOME_OPTION_TICKET) }
                )
            }
        }
    }

    private fun openAddClient() {
        startActivity(Intent(this, AddClientActivity::class.java))
    }

    private fun showComingSoonToast(optionName: String) {
        val message = String.format(Locale.getDefault(), HOME_OPTION_COMING_SOON_TEMPLATE, optionName)
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun HomeScreen(
    title: String,
    subtitle: String,
    addTitle: String,
    addDescription: String,
    deleteTitle: String,
    iterateTitle: String,
    ticketTitle: String,
    pendingDescription: String,
    onAddClick: () -> Unit,
    onEliminarClick: () -> Unit,
    onIterarClick: () -> Unit,
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
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }

            MenuOption(
                title = addTitle,
                description = addDescription,
                onClick = onAddClick
            )
            MenuOption(
                title = deleteTitle,
                description = pendingDescription,
                onClick = onEliminarClick
            )
            MenuOption(
                title = iterateTitle,
                description = pendingDescription,
                onClick = onIterarClick
            )
            MenuOption(
                title = ticketTitle,
                description = pendingDescription,
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
