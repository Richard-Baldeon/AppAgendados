package com.example.agendados.addclient

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import android.widget.NumberPicker
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.agendados.R
import com.example.agendados.data.ClientRepository
import com.example.agendados.ui.theme.AgendadosTheme
import com.example.agendados.ui.components.HomeNavigationButton
import java.time.LocalDate
import java.util.Locale

@Composable
fun AddClientActivityContent(onHomeClick: () -> Unit) {
    AgendadosTheme(darkTheme = false, dynamicColor = false) {
        val context = LocalContext.current
        val repository = remember(context.applicationContext) {
            ClientRepository.getInstance(context.applicationContext)
        }
        val viewModel: AddClientViewModel = viewModel(
            factory = AddClientViewModelFactory(repository)
        )
        AddClientRoute(
            viewModel = viewModel,
            onHomeClick = onHomeClick
        )
    }
}

@Composable
fun AddClientRoute(
    viewModel: AddClientViewModel,
    onHomeClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var alertReason by rememberSaveable { mutableStateOf<ScheduleAlertReason?>(null) }

    var isListening by rememberSaveable { mutableStateOf(false) }
    var speechError by remember { mutableStateOf<String?>(null) }
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
                        viewModel.onDictation(spoken)
                    } else {
                        speechError = context.getString(R.string.speech_no_text_captured)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val partial = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                    if (!partial.isNullOrBlank()) {
                        viewModel.onDictation(partial)
                    }
                }

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
            putExtra(RecognizerIntent.EXTRA_PROMPT, context.getString(R.string.add_client_title))
        }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    speechRecognizer?.let { recognizer ->
        DisposableEffect(recognizer, lifecycleOwner) {
            val observer = object : DefaultLifecycleObserver {
                override fun onStop(owner: LifecycleOwner) {
                    if (isListening) {
                        recognizer.stopListening()
                        isListening = false
                    }
                }

                override fun onDestroy(owner: LifecycleOwner) {
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
        val permissionGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (permissionGranted) {
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

    fun commitSchedule(): Boolean {
        return when (val result = viewModel.saveCurrentClient()) {
            is SaveResult.Success -> {
                Toast.makeText(context, R.string.schedule_saved_message, Toast.LENGTH_SHORT).show()
                viewModel.resetForm()
                true
            }

            is SaveResult.ValidationError -> {
                val message = buildValidationMessage(context, result.errors)
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                false
            }
        }
    }

    val isMicEnabled = speechAvailable && speechRecognizer != null
    val listeningHint = if (!isMicEnabled) {
        context.getString(R.string.no_speech_recognizer)
    } else {
        null
    }

    AddClientScreen(
        state = state,
        isMicEnabled = isMicEnabled,
        isListening = isListening,
        listeningMessage = listeningHint,
        onMicClick = { handleMicClick() },
        onCelularChange = viewModel::updateCelular,
        onNombreChange = viewModel::updateNombre,
        onMontoPPChange = viewModel::updateMontoPP,
        onTasaPPChange = viewModel::updateTasaPP,
        onDeudaChange = viewModel::updateDeuda,
        onMontoCDChange = viewModel::updateMontoCD,
        onTasaCDChange = viewModel::updateTasaCD,
        onComentariosChange = viewModel::updateComentarios,
        onDayChange = viewModel::onDateSelected,
        onHourChange = viewModel::onHourSelected,
        onMinuteChange = viewModel::onMinuteSelected,
        onPeriodChange = viewModel::onPeriodSelected,
        onConfirm = {
            val reason = viewModel.evaluateSchedule()
            if (reason == null) {
                commitSchedule()
            } else {
                alertReason = reason
            }
        },
        onHomeClick = onHomeClick
    )

    if (alertReason != null) {
        ScheduleAlertDialog(
            reason = alertReason!!,
            state = state,
            onDayChange = viewModel::onDateSelected,
            onHourChange = viewModel::onHourSelected,
            onMinuteChange = viewModel::onMinuteSelected,
            onPeriodChange = viewModel::onPeriodSelected,
            onConfirm = {
                if (commitSchedule()) {
                    alertReason = null
                }
            },
            onDismiss = { alertReason = null }
        )
    }
}

@Composable
fun AddClientScreen(
    state: AddClientUiState,
    isMicEnabled: Boolean,
    isListening: Boolean,
    listeningMessage: String? = null,
    onMicClick: () -> Unit,
    onCelularChange: (String) -> Unit,
    onNombreChange: (String) -> Unit,
    onMontoPPChange: (String) -> Unit,
    onTasaPPChange: (String) -> Unit,
    onDeudaChange: (String) -> Unit,
    onMontoCDChange: (String) -> Unit,
    onTasaCDChange: (String) -> Unit,
    onComentariosChange: (String) -> Unit,
    onDayChange: (Int) -> Unit,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    onPeriodChange: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onHomeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val displayListeningMessage = listeningMessage ?: stringResource(R.string.listening_message)
    Column(
        modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 24.dp)
            .verticalScroll(scrollState)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        HomeNavigationButton(
            onClick = onHomeClick,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = stringResource(id = R.string.add_client_title),
            style = MaterialTheme.typography.titleLarge.copy(
                color = TextColor,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier
                    .size(96.dp)
                    .shadow(12.dp, CircleShape)
                    .clickable(enabled = isMicEnabled) { onMicClick() },
                shape = CircleShape,
                color = when {
                    !isMicEnabled -> ActionColor.copy(alpha = 0.3f)
                    isListening -> ListeningColor
                    else -> ActionColor
                }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Mic,
                        contentDescription = stringResource(id = R.string.mic_button_content_description),
                        tint = if (isMicEnabled) Color.White else Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = displayListeningMessage,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = if (isListening) ActionColor else TextColor
        )
        Spacer(modifier = Modifier.height(32.dp))
        DictationPreview(text = state.dictationText)
        Spacer(modifier = Modifier.height(24.dp))
        BlockFields(
            state = state,
            onCelularChange = onCelularChange,
            onNombreChange = onNombreChange,
            onMontoPPChange = onMontoPPChange,
            onTasaPPChange = onTasaPPChange,
            onDeudaChange = onDeudaChange,
            onMontoCDChange = onMontoCDChange,
            onTasaCDChange = onTasaCDChange,
            onComentariosChange = onComentariosChange
        )
        Spacer(modifier = Modifier.height(32.dp))
        ScheduleSection(
            state = state,
            onDayChange = onDayChange,
            onHourChange = onHourChange,
            onMinuteChange = onMinuteChange,
            onPeriodChange = onPeriodChange
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onConfirm,
            colors = ButtonDefaults.buttonColors(containerColor = ActionColor),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = stringResource(id = R.string.confirm_button),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun DictationPreview(text: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(id = R.string.dictation_preview_label),
            style = MaterialTheme.typography.bodyMedium.copy(color = TextColor, fontWeight = FontWeight.Medium)
        )
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = FieldBackground,
            tonalElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .shadow(6.dp, RoundedCornerShape(12.dp))
        ) {
            Text(
                text = text.ifBlank { stringResource(id = R.string.dictation_preview_placeholder) },
                style = MaterialTheme.typography.bodyMedium.copy(color = TextColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 96.dp)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            )
        }
    }
}

@Composable
private fun BlockFields(
    state: AddClientUiState,
    onCelularChange: (String) -> Unit,
    onNombreChange: (String) -> Unit,
    onMontoPPChange: (String) -> Unit,
    onTasaPPChange: (String) -> Unit,
    onDeudaChange: (String) -> Unit,
    onMontoCDChange: (String) -> Unit,
    onTasaCDChange: (String) -> Unit,
    onComentariosChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            LabeledField(
                label = stringResource(id = R.string.field_celular),
                value = state.celular,
                onValueChange = onCelularChange,
                modifier = Modifier.weight(1f)
            )
            LabeledField(
                label = stringResource(id = R.string.field_nombre),
                value = state.nombre,
                onValueChange = onNombreChange,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            LabeledField(
                label = stringResource(id = R.string.field_monto_pp),
                value = state.montoPP,
                onValueChange = onMontoPPChange,
                modifier = Modifier.weight(1f)
            )
            LabeledField(
                label = stringResource(id = R.string.field_tasa_pp),
                value = state.tasaPP,
                onValueChange = onTasaPPChange,
                modifier = Modifier.weight(1f)
            )
            LabeledField(
                label = stringResource(id = R.string.field_deuda),
                value = state.deuda,
                onValueChange = onDeudaChange,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            LabeledField(
                label = stringResource(id = R.string.field_monto_cd),
                value = state.montoCD,
                onValueChange = onMontoCDChange,
                modifier = Modifier.weight(1f)
            )
            LabeledField(
                label = stringResource(id = R.string.field_tasa_cd),
                value = state.tasaCD,
                onValueChange = onTasaCDChange,
                modifier = Modifier.weight(1f)
            )
        }
        LabeledField(
            label = stringResource(id = R.string.field_comentarios),
            value = state.comentarios,
            onValueChange = onComentariosChange,
            singleLine = false,
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ScheduleSection(
    state: AddClientUiState,
    onDayChange: (Int) -> Unit,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    onPeriodChange: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = stringResource(id = R.string.agendamiento_title),
            style = MaterialTheme.typography.titleMedium.copy(color = TextColor, fontWeight = FontWeight.Bold)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(id = R.string.day_label),
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextColor, fontWeight = FontWeight.Medium)
                )
                Spacer(modifier = Modifier.height(8.dp))
                PickerContainer {
                    DayPicker(state = state, onDayChange = onDayChange)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(id = R.string.time_label),
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextColor, fontWeight = FontWeight.Medium)
                )
                Spacer(modifier = Modifier.height(8.dp))
                PickerContainer {
                    TimePicker(
                        hour = state.hour,
                        minute = state.minute,
                        isAm = state.isAm,
                        onHourChange = onHourChange,
                        onMinuteChange = onMinuteChange,
                        onPeriodChange = onPeriodChange
                    )
                }
            }
        }
    }
}

@Composable
private fun PickerContainer(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .shadow(6.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = FieldBackground,
        tonalElevation = 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            content()
        }
    }
}

@Composable
private fun DayPicker(state: AddClientUiState, onDayChange: (Int) -> Unit) {
    val options = state.dateOptions
    val today = options.firstOrNull()
    val tomorrow = today?.plusDays(1)
    val context = LocalContext.current
    val labels = remember(options) {
        options.map { date ->
            buildDayLabel(date, today, tomorrow, context)
        }.toTypedArray()
    }
    AndroidNumberPicker(
        minValue = 0,
        maxValue = (labels.size - 1).coerceAtLeast(0),
        value = state.selectedDateIndex.coerceIn(0, labels.lastIndex.coerceAtLeast(0)),
        displayedValues = labels,
        onValueChange = onDayChange,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun TimePicker(
    hour: Int,
    minute: Int,
    isAm: Boolean,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    onPeriodChange: (Boolean) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AndroidNumberPicker(
            minValue = 1,
            maxValue = 12,
            value = hour.coerceIn(1, 12),
            displayedValues = (1..12).map { it.toString() }.toTypedArray(),
            onValueChange = onHourChange,
            modifier = Modifier.weight(1f)
        )
        AndroidNumberPicker(
            minValue = 0,
            maxValue = 59,
            value = minute.coerceIn(0, 59),
            displayedValues = (0..59).map { String.format(Locale.getDefault(), "%02d", it) }.toTypedArray(),
            onValueChange = onMinuteChange,
            modifier = Modifier.weight(1f)
        )
        AndroidNumberPicker(
            minValue = 0,
            maxValue = 1,
            value = if (isAm) 0 else 1,
            displayedValues = arrayOf("AM", "PM"),
            onValueChange = { newVal -> onPeriodChange(newVal == 0) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AndroidNumberPicker(
    minValue: Int,
    maxValue: Int,
    value: Int,
    displayedValues: Array<String>,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidViewNumberPicker(
        minValue = minValue,
        maxValue = maxValue,
        value = value,
        displayedValues = displayedValues,
        onValueChange = onValueChange,
        modifier = modifier
    )
}

@Composable
private fun AndroidViewNumberPicker(
    minValue: Int,
    maxValue: Int,
    value: Int,
    displayedValues: Array<String>,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.ui.viewinterop.AndroidView(
        modifier = modifier,
        factory = { context ->
            NumberPicker(context).apply {
                this.minValue = minValue
                this.maxValue = maxValue.coerceAtLeast(minValue)
                this.displayedValues = displayedValues
                this.value = value.coerceIn(this.minValue, this.maxValue)
                this.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
                this.setOnValueChangedListener { _, _, newVal ->
                    onValueChange(newVal)
                }
            }
        },
        update = { picker ->
            val safeMax = maxValue.coerceAtLeast(minValue)
            if (picker.minValue != minValue || picker.maxValue != safeMax) {
                picker.displayedValues = null
                picker.minValue = minValue
                picker.maxValue = safeMax
                picker.displayedValues = displayedValues
            }
            if (picker.displayedValues?.contentEquals(displayedValues) == false) {
                picker.displayedValues = displayedValues
            }
            val coercedValue = value.coerceIn(minValue, safeMax)
            if (picker.value != coercedValue) {
                picker.value = coercedValue
            }
        }
    )
}

@Composable
private fun LabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1
) {
    Column(modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(color = TextColor, fontWeight = FontWeight.Medium)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = FieldBackground,
            tonalElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .shadow(6.dp, RoundedCornerShape(12.dp))
        ) {
            androidx.compose.foundation.text.BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = singleLine,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextColor),
                cursorBrush = SolidColor(TextColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = if (singleLine) 48.dp else 120.dp)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                decorationBox = { innerTextField -> innerTextField() },
                minLines = minLines
            )
        }
    }
}

@Composable
private fun ScheduleAlertDialog(
    reason: ScheduleAlertReason,
    state: AddClientUiState,
    onDayChange: (Int) -> Unit,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    onPeriodChange: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(id = R.string.schedule_review_title),
                style = MaterialTheme.typography.titleMedium.copy(color = TextColor, fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(text = reason.reasonText, color = TextColor)
                ScheduleSection(
                    state = state,
                    onDayChange = onDayChange,
                    onHourChange = onHourChange,
                    onMinuteChange = onMinuteChange,
                    onPeriodChange = onPeriodChange
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = ActionColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = stringResource(id = R.string.schedule_review_confirm), color = Color.White)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.schedule_review_cancel))
            }
        }
    )
}

private val ActionColor = Color(0xFF2BB673)
private val ListeningColor = Color(0xFF13855C)
private val TextColor = Color(0xFF1E1E1E)
private val FieldBackground = Color(0xFFF3F4F6)

private fun buildDayLabel(
    date: LocalDate,
    today: LocalDate?,
    tomorrow: LocalDate?,
    context: android.content.Context
): String {
    val base = date.dayOfMonth.toString()
    val suffix = when {
        today != null && date == today -> context.getString(R.string.today_label)
        tomorrow != null && date == tomorrow -> context.getString(R.string.tomorrow_label)
        else -> null
    }
    return if (suffix != null) "$base (${suffix})" else base
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

private fun buildValidationMessage(context: Context, errors: List<SaveValidationError>): String {
    val distinctErrors = errors.toSet()
    return when {
        distinctErrors.containsAll(setOf(SaveValidationError.MissingName, SaveValidationError.InvalidPhone)) ->
            context.getString(R.string.validation_missing_name_phone)

        distinctErrors.contains(SaveValidationError.MissingName) ->
            context.getString(R.string.validation_missing_name)

        distinctErrors.contains(SaveValidationError.InvalidPhone) ->
            context.getString(R.string.validation_invalid_phone)

        else -> context.getString(R.string.validation_generic_error)
    }
}
