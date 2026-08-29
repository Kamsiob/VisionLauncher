package io.github.kamsiob.launcher.alarm

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.alarmData by preferencesDataStore(name = "alarms")

@Serializable
data class Alarm(
    val id: Int,
    val hour: Int,
    val minute: Int,
    val label: String = "",
    val enabled: Boolean = true,
)

/** The alarms, kept on the device like everything else. */
class AlarmStore(private val context: Context) {

    private val key = stringPreferencesKey("alarms")
    private val json = Json { ignoreUnknownKeys = true }

    val alarms: Flow<List<Alarm>> = context.alarmData.data.map { prefs ->
        prefs[key]?.let { stored ->
            runCatching { json.decodeFromString<List<Alarm>>(stored) }.getOrDefault(emptyList())
        }?.sortedWith(compareBy({ it.hour }, { it.minute })) ?: emptyList()
    }

    suspend fun current(): List<Alarm> = alarms.first()

    private suspend fun write(list: List<Alarm>) {
        context.alarmData.edit { it[key] = json.encodeToString(list) }
    }

    suspend fun save(alarm: Alarm) {
        val list = current().toMutableList()
        val index = list.indexOfFirst { it.id == alarm.id }
        if (index >= 0) list[index] = alarm else list.add(alarm)
        write(list)
    }

    suspend fun delete(id: Int) {
        write(current().filterNot { it.id == id })
    }

    suspend fun nextId(): Int = (current().maxOfOrNull { it.id } ?: 0) + 1
}
