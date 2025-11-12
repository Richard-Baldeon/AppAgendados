package com.example.agendados.addclient

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.agendados.data.ClientRecord
import com.example.agendados.data.ClientRepository
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AddClientViewModel(
    private val repository: ClientRepository,
    private val clock: Clock = Clock.system(LimaZone),
    private val holidayCalendar: HolidayCalendar = PeruHolidayCalendar
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddClientUiState())
    val uiState: StateFlow<AddClientUiState> = _uiState.asStateFlow()

    init {
        val options = generateDateOptions(clock)
        val defaultDate = computeDefaultScheduleDate(clock, holidayCalendar)
        val defaultIndex = options.indexOf(defaultDate).takeIf { it >= 0 } ?: 0
        _uiState.update {
            it.copy(
                dateOptions = options,
                selectedDateIndex = defaultIndex,
                hour = AddClientUiState.DEFAULT_HOUR,
                minute = AddClientUiState.DEFAULT_MINUTE,
                isAm = true
            )
        }
    }

    fun updateCelular(value: String) {
        _uiState.update { it.copy(celular = value) }
    }

    fun updateNombre(value: String) {
        _uiState.update { it.copy(nombre = value.uppercase(Locale.getDefault())) }
    }

    fun updateMontoPP(value: String) {
        _uiState.update { it.copy(montoPP = value) }
    }

    fun updateTasaPP(value: String) {
        _uiState.update { it.copy(tasaPP = value) }
    }

    fun updateDeuda(value: String) {
        _uiState.update { it.copy(deuda = value) }
    }

    fun updateMontoCD(value: String) {
        _uiState.update { it.copy(montoCD = value) }
    }

    fun updateTasaCD(value: String) {
        _uiState.update { it.copy(tasaCD = value) }
    }

    fun updateComentarios(value: String) {
        _uiState.update { it.copy(comentarios = value) }
    }

    fun onDictation(text: String) {
        val result = parseDictation(text)
        _uiState.update { current ->
            val updatedDateIndex = result.scheduledTime?.let {
                val defaultDate = computeDefaultScheduleDate(clock, holidayCalendar)
                current.dateOptions.indexOf(defaultDate).takeIf { it >= 0 } ?: current.selectedDateIndex
            } ?: current.selectedDateIndex

            val normalizedNombre = result.nombre?.uppercase(Locale.getDefault()) ?: current.nombre

            current.copy(
                celular = result.celular ?: current.celular,
                nombre = normalizedNombre,
                montoPP = result.montoPP ?: current.montoPP,
                tasaPP = result.tasaPP ?: current.tasaPP,
                deuda = result.deuda ?: current.deuda,
                montoCD = result.montoCD ?: current.montoCD,
                tasaCD = result.tasaCD ?: current.tasaCD,
                comentarios = result.comentarios ?: current.comentarios,
                dictationText = text,
                selectedDateIndex = updatedDateIndex
            ).withTimeFromDictation(result.scheduledTime)
        }
    }

    fun saveCurrentClient(): SaveResult {
        val state = _uiState.value
        val errors = mutableListOf<SaveValidationError>()
        val sanitizedPhone = state.celular.filter { it.isDigit() }
        if (state.nombre.isBlank()) {
            errors += SaveValidationError.MissingName
        }
        if (sanitizedPhone.length != 9) {
            errors += SaveValidationError.InvalidPhone
        }

        if (errors.isNotEmpty()) {
            return SaveResult.ValidationError(errors)
        }

        val date = selectedDate(state)
        val time = toLocalTime(state)
        val existing = repository.findByPhone(sanitizedPhone)
        val recordId = existing?.id ?: UUID.randomUUID().toString()

        val record = ClientRecord(
            id = recordId,
            createdAtMillis = existing?.createdAtMillis ?: System.currentTimeMillis(),
            nombre = state.nombre,
            celular = sanitizedPhone,
            montoPP = state.montoPP,
            tasaPP = state.tasaPP,
            deuda = state.deuda,
            montoCD = state.montoCD,
            tasaCD = state.tasaCD,
            comentarios = state.comentarios,
            scheduledDate = date,
            scheduledTime = time
        )
        repository.addOrUpdate(record)

        return SaveResult.Success(recordId)
    }

    fun onDateSelected(index: Int) {
        _uiState.update { state ->
            val clamped = index.coerceIn(0, state.dateOptions.lastIndex)
            state.copy(selectedDateIndex = clamped)
        }
    }

    fun onHourSelected(hour: Int) {
        val sanitized = hour.coerceIn(1, 12)
        _uiState.update { it.copy(hour = sanitized) }
    }

    fun onMinuteSelected(minute: Int) {
        val sanitized = minute.coerceIn(0, 59)
        _uiState.update { it.copy(minute = sanitized) }
    }

    fun onPeriodSelected(isAm: Boolean) {
        _uiState.update { it.copy(isAm = isAm) }
    }

    fun evaluateSchedule(): ScheduleAlertReason? {
        val state = _uiState.value
        val selectedDate = state.dateOptions.getOrNull(state.selectedDateIndex) ?: return null
        val selectedTime = toLocalTime(state)
        return when {
            selectedDate.dayOfWeek == java.time.DayOfWeek.SUNDAY -> ScheduleAlertReason.CaeDomingo
            !isBusinessDay(selectedDate, holidayCalendar) -> ScheduleAlertReason.CaeFeriado
            selectedTime.isBefore(LocalTime.of(9, 0)) || selectedTime.isAfter(LocalTime.of(20, 0)) ->
                ScheduleAlertReason.FueraHorario

            else -> null
        }
    }

    fun toLocalTime(state: AddClientUiState = _uiState.value): LocalTime {
        val hour24 = when {
            state.isAm -> state.hour % 12
            else -> (state.hour % 12) + 12
        }
        return LocalTime.of(hour24, state.minute)
    }

    private fun selectedDate(state: AddClientUiState): LocalDate {
        return state.dateOptions.getOrNull(state.selectedDateIndex)
            ?: computeDefaultScheduleDate(clock, holidayCalendar)
    }

    private fun AddClientUiState.withTimeFromDictation(time: LocalTime?): AddClientUiState {
        time ?: return this
        val hour = when (time.hour) {
            0 -> 12
            in 1..12 -> time.hour
            else -> time.hour - 12
        }
        val isAm = time.hour < 12
        return copy(
            hour = hour,
            minute = time.minute,
            isAm = isAm
        )
    }
}

enum class ScheduleAlertReason(val reasonText: String) {
    CaeDomingo("Cae domingo"),
    CaeFeriado("Cae feriado"),
    FueraHorario("Fuera del horario (09:00–20:00)")
}

class AddClientViewModelFactory(
    private val repository: ClientRepository,
    private val clock: Clock = Clock.system(LimaZone),
    private val holidayCalendar: HolidayCalendar = PeruHolidayCalendar
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddClientViewModel::class.java)) {
            return AddClientViewModel(repository, clock, holidayCalendar) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

sealed class SaveResult {
    data class Success(val recordId: String) : SaveResult()
    data class ValidationError(val errors: List<SaveValidationError>) : SaveResult()
}

enum class SaveValidationError {
    MissingName,
    InvalidPhone
}
