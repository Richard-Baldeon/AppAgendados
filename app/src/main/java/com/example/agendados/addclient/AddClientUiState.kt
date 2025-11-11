package com.example.agendados.addclient

import java.time.LocalDate

/**
 * UI state for the Añadir Cliente screen.
 */
data class AddClientUiState(
    val celular: String = "",
    val nombre: String = "",
    val montoPP: String = "",
    val tasaPP: String = "",
    val deuda: String = "",
    val montoCD: String = "",
    val tasaCD: String = "",
    val comentarios: String = "",
    val dictationText: String? = null,
    val dateOptions: List<LocalDate> = emptyList(),
    val selectedDateIndex: Int = 0,
    val hour: Int = DEFAULT_HOUR,
    val minute: Int = DEFAULT_MINUTE,
    val isAm: Boolean = true
) {
    companion object {
        const val DEFAULT_HOUR = 10
        const val DEFAULT_MINUTE = 30
    }
}
