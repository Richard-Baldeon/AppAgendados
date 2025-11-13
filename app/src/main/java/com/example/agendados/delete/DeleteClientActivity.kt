package com.example.agendados.delete

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.agendados.R
import com.example.agendados.addclient.extractPhoneDigits
import com.example.agendados.data.ClientRecord
import com.example.agendados.data.ClientRepository
import com.example.agendados.home.HomeActivity
import com.example.agendados.iterate.ClientDetailActivity
import com.example.agendados.ui.components.HomeNavigationButton
import com.example.agendados.ui.theme.AgendadosTheme
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope

class DeleteClientActivity : ComponentActivity() {

    private val repository by lazy { ClientRepository.getInstance(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AgendadosTheme {
                val viewModel: DeleteClientViewModel = viewModel(
                    factory = DeleteClientViewModelFactory(repository)
                )
                DeleteClientRoute(
                    viewModel = viewModel,
                    onHomeClick = { openHome() },
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

    private fun openDetail(recordId: String) {
        val intent = Intent(this, ClientDetailActivity::class.java)
            .putExtra(ClientDetailActivity.EXTRA_CLIENT_ID, recordId)
        startActivity(intent)
    }
}

@Composable
fun DeleteClientRoute(
    viewModel: DeleteClientViewModel,
    onHomeClick: () -> Unit,
    onOpenDetail: (String) -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()

    var isListening by rememberSaveable { mutableStateOf(false) }
    var speechError by remember { mutableStateOf<String?>(null) }
    var showConfirmDialog by rememberSaveable { mutableStateOf(false) }

    val speechAvailable = remember { SpeechRecognizer.isRecognitionAvailable(context) }
    val speechRecognizer = remember(speechAvailable) {
        if (speechAvailable) {
            SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isListening = true
                    }

                    override fun onBeginningOfSpeech() {
                        isListening = true
                    }

                    override fun onRmsChanged(rmsdB: Float) = Unit

                    override fun onBufferReceived(buffer: ByteArray?) = Unit

                    override fun onEndOfSpeech() {
                        isListening = false
                    }

                    override fun onError(error: Int) {
                        isListening = false
                        speechError = mapSpeechError(context, error)
                    }

                    override fun onResults(results: Bundle?) {
                        isListening = false
                        val spoken = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                        if (!spoken.isNullOrBlank()) {
                            val digits = extractPhoneDigits(spoken)
                            if (digits == null) {
                                speechError = context.getString(R.string.delete_client_no_digits)
                            }
                            viewModel.onDictation(spoken, digits)
                        } else {
                            speechError = context.getString(R.string.speech_no_text_captured)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) = Unit

                    override fun onEvent(eventType: Int, params: Bundle?) = Unit
                })
            }
        } else {
            null
        }
    }

    val recognitionIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, context.getString(R.string.delete_client_title))
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    speechRecognizer?.let { recognizer ->
        DisposableEffect(recognizer, lifecycleOwner) {
            val observer = object : androidx.lifecycle.DefaultLifecycleObserver {
                override fun onStop(owner: androidx.lifecycle.LifecycleOwner) {
                    if (isListening) {
                        recognizer.stopListening()
                        isListening = false
                    }
                }

                override fun onDestroy(owner: androidx.lifecycle.LifecycleOwner) {
                    recognizer.cancel()
                    recognizer.destroy()
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                recognizer.cancel()
                recognizer.destroy()
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val recognizer = speechRecognizer
            if (recognizer != null) {
                startListening(context, recognizer, recognitionIntent) { message ->
                    speechError = message
                }
            } else {
                speechError = context.getString(R.string.no_speech_recognizer)
            }
        } else {
            Toast.makeText(context, R.string.audio_permission_required, Toast.LENGTH_SHORT).show()
        }
    }

    fun handleMicClick() {
        if (!speechAvailable || speechRecognizer == null) {
            speechError = context.getString(R.string.no_speech_recognizer)
            return
        }
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (granted) {
            if (isListening) {
                speechRecognizer.stopListening()
                isListening = false
            } else {
                startListening(context, speechRecognizer, recognitionIntent) { message ->
                    speechError = message
                }
            }
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(speechError) {
        speechError?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            speechError = null
        }
    }

    fun confirmDeletion() {
        val removed = viewModel.deleteSelected()
        if (removed > 0) {
            Toast.makeText(context, context.getString(R.string.delete_client_success, removed), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, R.string.delete_client_no_selection, Toast.LENGTH_SHORT).show()
        }
        showConfirmDialog = false
    }

    DeleteClientScreen(
        state = state,
        isMicEnabled = speechAvailable && speechRecognizer != null,
        isListening = isListening,
        onMicClick = { handleMicClick() },
        onHomeClick = onHomeClick,
        onSelectionToggle = viewModel::toggleSelection,
        onConfirmRequest = { showConfirmDialog = true },
        onOpenDetail = onOpenDetail
    )

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = {
                Text(text = stringResource(id = R.string.delete_client_confirmation_title))
            },
            text = {
                Text(text = stringResource(id = R.string.delete_client_confirmation_message))
            },
            confirmButton = {
                Button(onClick = { confirmDeletion() }) {
                    Text(text = stringResource(id = R.string.delete_client_confirm_button))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirmDialog = false }) {
                    Text(text = stringResource(id = R.string.delete_client_cancel_button))
                }
            }
        )
    }
}

@Composable
private fun DeleteClientScreen(
    state: DeleteClientUiState,
    isMicEnabled: Boolean,
    isListening: Boolean,
    onMicClick: () -> Unit,
    onHomeClick: () -> Unit,
    onSelectionToggle: (String) -> Unit,
    onConfirmRequest: () -> Unit,
    onOpenDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    Column(
        modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 24.dp, vertical = 32.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        HomeNavigationButton(onClick = onHomeClick)
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(id = R.string.delete_client_title),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(id = R.string.delete_client_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier
                    .size(96.dp)
                    .shadow(12.dp, CircleShape)
                    .clickable(enabled = isMicEnabled) { onMicClick() },
                shape = CircleShape,
                color = when {
                    !isMicEnabled -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    isListening -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.primary
                }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Mic,
                        contentDescription = stringResource(id = R.string.mic_button_content_description),
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }
        DictationPreview(text = state.dictationText)
        if (state.phoneDigits.isNotBlank()) {
            Text(
                text = stringResource(id = R.string.delete_client_detected_number, state.phoneDigits),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
        when {
            state.phoneDigits.length == 9 && state.matches.isEmpty() -> {
                Text(
                    text = stringResource(id = R.string.delete_client_no_matches),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            state.matches.isNotEmpty() -> {
                Text(
                    text = stringResource(id = R.string.delete_client_select_prompt),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    state.matches.forEach { record ->
                        DeleteRecordItem(
                            record = record,
                            checked = state.selectedIds.contains(record.id),
                            onToggle = { onSelectionToggle(record.id) },
                            onOpenDetail = { onOpenDetail(record.id) }
                        )
                    }
                }
            }
        }
        Button(
            onClick = onConfirmRequest,
            enabled = state.selectedIds.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.delete_client_confirm_action),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun DictationPreview(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 4.dp,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text(
                text = stringResource(id = R.string.dictation_preview_label),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (text.isBlank()) stringResource(id = R.string.dictation_preview_placeholder) else text,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun DeleteRecordItem(
    record: ClientRecord,
    checked: Boolean,
    onToggle: () -> Unit,
    onOpenDetail: () -> Unit
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault()) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault()) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Checkbox(checked = checked, onCheckedChange = { onToggle() })
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
            }
            OutlinedButton(onClick = onOpenDetail) {
                Text(text = stringResource(id = R.string.delete_client_view_detail))
            }
        }
    }
}

private fun startListening(
    context: Context,
    speechRecognizer: SpeechRecognizer,
    intent: Intent,
    onStartFailure: (String) -> Unit
) {
    if (!SpeechRecognizer.isRecognitionAvailable(context)) {
        onStartFailure(context.getString(R.string.no_speech_recognizer))
        return
    }
    runCatching {
        speechRecognizer.startListening(intent)
    }.onFailure {
        onStartFailure(context.getString(R.string.speech_error_client))
    }
}

private fun mapSpeechError(context: Context, errorCode: Int): String {
    return when (errorCode) {
        SpeechRecognizer.ERROR_AUDIO -> context.getString(R.string.speech_error_audio)
        SpeechRecognizer.ERROR_CLIENT -> context.getString(R.string.speech_error_client)
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> context.getString(R.string.audio_permission_required)
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> context.getString(R.string.speech_error_network)
        SpeechRecognizer.ERROR_NO_MATCH -> context.getString(R.string.speech_no_text_captured)
        SpeechRecognizer.ERROR_SERVER -> context.getString(R.string.speech_error_server)
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> context.getString(R.string.speech_error_busy)
        else -> context.getString(R.string.speech_error_unknown)
    }
}

private fun formatPhoneNumber(raw: String): String {
    val digits = raw.filter { it.isDigit() }
    return if (digits.length == 9) digits.chunked(3).joinToString(" ") else raw
}

data class DeleteClientUiState(
    val dictationText: String = "",
    val phoneDigits: String = "",
    val matches: List<ClientRecord> = emptyList(),
    val selectedIds: Set<String> = emptySet()
)

class DeleteClientViewModel(
    private val repository: ClientRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeleteClientUiState())
    val uiState: StateFlow<DeleteClientUiState> = _uiState.asStateFlow()

    private var clients: List<ClientRecord> = emptyList()

    init {
        viewModelScope.launch {
            repository.clients.collect { items ->
                clients = items
                _uiState.update { state ->
                    val matches = findMatches(state.phoneDigits)
                    val allowedSelections = state.selectedIds.filter { id -> matches.any { it.id == id } }.toSet()
                    state.copy(matches = matches, selectedIds = allowedSelections)
                }
            }
        }
    }

    fun onDictation(text: String, digitsOverride: String? = null) {
        val digits = digitsOverride ?: extractPhoneDigits(text) ?: fallbackDigits(text)
        val sanitized = digits?.takeIf { it.length == 9 && it.startsWith('9') } ?: ""
        val matches = findMatches(sanitized)
        _uiState.update { state ->
            val allowedSelections = state.selectedIds.filter { id -> matches.any { it.id == id } }.toSet()
            state.copy(
                dictationText = text,
                phoneDigits = sanitized,
                matches = matches,
                selectedIds = allowedSelections
            )
        }
    }

    fun toggleSelection(id: String) {
        _uiState.update { state ->
            if (state.matches.none { it.id == id }) {
                state
            } else {
                val updated = state.selectedIds.toMutableSet()
                if (updated.contains(id)) {
                    updated.remove(id)
                } else {
                    updated.add(id)
                }
                state.copy(selectedIds = updated)
            }
        }
    }

    fun deleteSelected(): Int {
        val ids = _uiState.value.selectedIds
        if (ids.isEmpty()) return 0
        val existing = clients.filter { it.id in ids }.map { it.id }.toSet()
        if (existing.isEmpty()) {
            _uiState.update { it.copy(selectedIds = emptySet()) }
            return 0
        }
        repository.deleteRecords(existing)
        _uiState.update { it.copy(selectedIds = emptySet()) }
        return existing.size
    }

    private fun findMatches(digits: String): List<ClientRecord> {
        if (digits.length != 9) return emptyList()
        return clients.filter { it.celular == digits }
    }

    private fun fallbackDigits(text: String): String? {
        val digits = text.filter { it.isDigit() }
        val match = Regex("9\\d{8}").find(digits) ?: return null
        return match.value
    }
}

class DeleteClientViewModelFactory(
    private val repository: ClientRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DeleteClientViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DeleteClientViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
