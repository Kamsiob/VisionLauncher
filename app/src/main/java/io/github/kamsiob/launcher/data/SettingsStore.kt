package io.github.kamsiob.launcher.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.kamsiob.launcher.ui.theme.Look
import io.github.kamsiob.launcher.ui.theme.TextStep
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.settingsData by preferencesDataStore(name = "settings")

@Serializable
data class Favorite(
    val name: String,
    val number: String,
    val relationship: String = "",
)

@Serializable
data class EmergencyContact(
    val name: String,
    val number: String,
)

data class Settings(
    val look: Look = Look.LIGHT,
    val outlined: Boolean = false,
    val textStep: TextStep = TextStep.ONE,
    val onboardingDone: Boolean = false,
    val helperPath: Boolean = false,
    val batteryStepSkipped: Boolean = false,
    val dismissedThresholds: Set<String> = emptySet(),
    val favorites: List<Favorite> = emptyList(),
    val emergencyContact: EmergencyContact? = null,
)

/**
 * Every user preference in one place, persisted through DataStore. Nothing
 * here ever leaves the device.
 */
class SettingsStore(private val context: Context) {

    private object Keys {
        val look = stringPreferencesKey("look")
        val outlined = booleanPreferencesKey("outlined")
        val textStep = intPreferencesKey("text_step")
        val onboardingDone = booleanPreferencesKey("onboarding_done")
        val helperPath = booleanPreferencesKey("helper_path")
        val batteryStepSkipped = booleanPreferencesKey("battery_step_skipped")
        val dismissedThresholds = stringSetPreferencesKey("dismissed_thresholds")
        val favorites = stringPreferencesKey("favorites")
        val emergencyContact = stringPreferencesKey("emergency_contact")
    }

    private val json = Json { ignoreUnknownKeys = true }

    val settings: Flow<Settings> = context.settingsData.data.map { prefs ->
        Settings(
            look = prefs[Keys.look]?.let { runCatching { Look.valueOf(it) }.getOrNull() } ?: Look.LIGHT,
            outlined = prefs[Keys.outlined] ?: false,
            textStep = when (prefs[Keys.textStep] ?: 0) {
                1 -> TextStep.TWO
                2 -> TextStep.THREE
                else -> TextStep.ONE
            },
            onboardingDone = prefs[Keys.onboardingDone] ?: false,
            helperPath = prefs[Keys.helperPath] ?: false,
            batteryStepSkipped = prefs[Keys.batteryStepSkipped] ?: false,
            dismissedThresholds = prefs[Keys.dismissedThresholds] ?: emptySet(),
            favorites = prefs[Keys.favorites]?.let { stored ->
                runCatching { json.decodeFromString<List<Favorite>>(stored) }.getOrDefault(emptyList())
            } ?: emptyList(),
            emergencyContact = prefs[Keys.emergencyContact]?.let { stored ->
                runCatching { json.decodeFromString<EmergencyContact>(stored) }.getOrNull()
            },
        )
    }

    suspend fun setLook(look: Look) {
        context.settingsData.edit { it[Keys.look] = look.name }
    }

    suspend fun setOutlined(outlined: Boolean) {
        context.settingsData.edit { it[Keys.outlined] = outlined }
    }

    suspend fun setTextStep(step: TextStep) {
        context.settingsData.edit { it[Keys.textStep] = step.ordinal }
    }

    suspend fun setOnboardingDone(helperPath: Boolean, batterySkipped: Boolean) {
        context.settingsData.edit {
            it[Keys.onboardingDone] = true
            it[Keys.helperPath] = helperPath
            it[Keys.batteryStepSkipped] = batterySkipped
        }
    }

    suspend fun dismissThreshold(destination: String) {
        context.settingsData.edit {
            it[Keys.dismissedThresholds] = (it[Keys.dismissedThresholds] ?: emptySet()) + destination
        }
    }

    suspend fun restoreAllThresholds() {
        context.settingsData.edit { it[Keys.dismissedThresholds] = emptySet() }
    }

    suspend fun setFavorites(favorites: List<Favorite>) {
        context.settingsData.edit { it[Keys.favorites] = json.encodeToString(favorites) }
    }

    suspend fun setEmergencyContact(contact: EmergencyContact?) {
        context.settingsData.edit {
            if (contact == null) it.remove(Keys.emergencyContact)
            else it[Keys.emergencyContact] = json.encodeToString(contact)
        }
    }
}
