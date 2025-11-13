package com.example.agendados.data

import android.content.Context
import android.content.SharedPreferences
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject

/**
 * Represents a client that was captured from the "Añadir Cliente" screen.
 */
data class ClientRecord(
    val id: String,
    val createdAtMillis: Long,
    val nombre: String,
    val celular: String,
    val montoPP: String,
    val tasaPP: String,
    val deuda: String,
    val montoCD: String,
    val tasaCD: String,
    val comentarios: String,
    val scheduledDate: LocalDate,
    val scheduledTime: LocalTime,
    val alarmActive: Boolean
)

/**
 * Repository in charge of persisting and exposing saved [ClientRecord] items.
 */
class ClientRepository private constructor(context: Context) {

    private val preferences: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private val _clients = MutableStateFlow(loadStoredRecords())
    val clients: StateFlow<List<ClientRecord>> = _clients.asStateFlow()

    /**
     * Saves a [ClientRecord]. If another record shares the same phone number the previous entry is
     * replaced to keep the contact unique.
     */
    fun addOrUpdate(record: ClientRecord) {
        val normalizedPhone = record.celular.filter { it.isDigit() }
        _clients.update { current ->
            val withoutPhoneDuplicates = current.filterNot { it.celular.filter { c -> c.isDigit() } == normalizedPhone }
            val updated = withoutPhoneDuplicates + record.copy(celular = normalizedPhone)
            val sorted = updated.sortedWith(compareBy<ClientRecord> { it.scheduledDate }.thenBy { it.scheduledTime })
            persist(sorted)
            sorted
        }
    }

    fun updateAlarmStatus(id: String, enabled: Boolean) {
        _clients.update { current ->
            val updated = current.map { record ->
                if (record.id == id) record.copy(alarmActive = enabled) else record
            }
            val sorted = updated.sortedWith(compareBy<ClientRecord> { it.scheduledDate }.thenBy { it.scheduledTime })
            persist(sorted)
            sorted
        }
    }

    fun deleteRecords(ids: Set<String>) {
        if (ids.isEmpty()) return
        _clients.update { current ->
            val updated = current.filterNot { it.id in ids }
            val sorted = updated.sortedWith(compareBy<ClientRecord> { it.scheduledDate }.thenBy { it.scheduledTime })
            persist(sorted)
            sorted
        }
    }

    fun findById(id: String): ClientRecord? = _clients.value.firstOrNull { it.id == id }

    fun findByPhone(phoneDigits: String): ClientRecord? =
        _clients.value.firstOrNull { it.celular == phoneDigits }

    private fun loadStoredRecords(): List<ClientRecord> {
        val json = preferences.getString(KEY_CLIENTS, null) ?: return emptyList()
        val result = mutableListOf<ClientRecord>()
        runCatching { JSONArray(json) }.getOrNull()?.let { array ->
            for (index in 0 until array.length()) {
                val obj = array.optJSONObject(index) ?: continue
                val id = obj.optString("id").ifBlank { UUID.randomUUID().toString() }
                val createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                val nombre = obj.optString("nombre", "")
                val celular = obj.optString("celular", "")
                val montoPP = obj.optString("montoPP", "")
                val tasaPP = obj.optString("tasaPP", "")
                val deuda = obj.optString("deuda", "")
                val montoCD = obj.optString("montoCD", "")
                val tasaCD = obj.optString("tasaCD", "")
                val comentarios = obj.optString("comentarios", "")
                val scheduledDate = obj.optString("scheduledDate").takeIf { it.isNotBlank() }?.let {
                    runCatching { LocalDate.parse(it) }.getOrNull()
                } ?: continue
                val scheduledTime = obj.optString("scheduledTime").takeIf { it.isNotBlank() }?.let {
                    runCatching { LocalTime.parse(it) }.getOrNull()
                } ?: continue
                val alarmActive = obj.optBoolean("alarmActive", true)
                result.add(
                    ClientRecord(
                        id = id,
                        createdAtMillis = createdAt,
                        nombre = nombre,
                        celular = celular,
                        montoPP = montoPP,
                        tasaPP = tasaPP,
                        deuda = deuda,
                        montoCD = montoCD,
                        tasaCD = tasaCD,
                        comentarios = comentarios,
                        scheduledDate = scheduledDate,
                        scheduledTime = scheduledTime,
                        alarmActive = alarmActive
                    )
                )
            }
        }
        return result.sortedWith(compareBy<ClientRecord> { it.scheduledDate }.thenBy { it.scheduledTime })
    }

    private fun persist(records: List<ClientRecord>) {
        val array = JSONArray()
        records.forEach { record ->
            val obj = JSONObject().apply {
                put("id", record.id)
                put("createdAt", record.createdAtMillis)
                put("nombre", record.nombre)
                put("celular", record.celular)
                put("montoPP", record.montoPP)
                put("tasaPP", record.tasaPP)
                put("deuda", record.deuda)
                put("montoCD", record.montoCD)
                put("tasaCD", record.tasaCD)
                put("comentarios", record.comentarios)
                put("scheduledDate", record.scheduledDate.toString())
                put("scheduledTime", record.scheduledTime.toString())
                put("alarmActive", record.alarmActive)
            }
            array.put(obj)
        }
        preferences.edit().putString(KEY_CLIENTS, array.toString()).apply()
    }

    companion object {
        private const val PREFERENCES_NAME = "client_repository"
        private const val KEY_CLIENTS = "clients"

        @Volatile
        private var INSTANCE: ClientRepository? = null

        fun getInstance(context: Context): ClientRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ClientRepository(context).also { INSTANCE = it }
            }
        }
    }
}

