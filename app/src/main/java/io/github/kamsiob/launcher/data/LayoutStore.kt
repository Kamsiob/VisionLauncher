package io.github.kamsiob.launcher.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.layoutData by preferencesDataStore(name = "layout")

/** The built in features that can hold a home tile. */
enum class BuiltIn(val id: String) {
    CALL("call"),
    MESSAGES("messages"),
    MAGNIFIER("magnifier"),
    CAMERA("camera"),
    PHOTOS("photos"),
    ALARMS("alarms");

    companion object {
        fun fromId(id: String?): BuiltIn? = entries.firstOrNull { it.id == id }
    }
}

/**
 * One tile on the home screen: a built in feature, a third party app, or an
 * empty spot that keeps its place so the layout the hands learned never
 * shifts underneath them.
 */
@Serializable
data class SavedTile(
    val builtIn: String? = null,
    val packageName: String? = null,
    val activity: String? = null,
) {
    val isEmpty: Boolean get() = builtIn == null && packageName == null

    companion object {
        val EMPTY = SavedTile()
        fun of(feature: BuiltIn) = SavedTile(builtIn = feature.id)
        fun ofApp(packageName: String, activity: String) =
            SavedTile(packageName = packageName, activity = activity)
    }
}

/**
 * The home layout and its last kept snapshot. Every Keep snapshots the
 * layout; Settings carries the standing "Put my screen back" that restores it.
 */
class LayoutStore(private val context: Context) {

    private object Keys {
        val current = stringPreferencesKey("current")
        val snapshot = stringPreferencesKey("snapshot")
    }

    private val json = Json { ignoreUnknownKeys = true }

    val defaultLayout: List<SavedTile> = listOf(
        SavedTile.of(BuiltIn.CALL),
        SavedTile.of(BuiltIn.MESSAGES),
        SavedTile.of(BuiltIn.MAGNIFIER),
        SavedTile.of(BuiltIn.CAMERA),
        SavedTile.of(BuiltIn.PHOTOS),
        SavedTile.EMPTY,
    )

    private fun decode(stored: String?): List<SavedTile>? = stored?.let {
        runCatching { json.decodeFromString<List<SavedTile>>(it) }.getOrNull()
    }

    val layout: Flow<List<SavedTile>> = context.layoutData.data.map { prefs ->
        decode(prefs[Keys.current]) ?: defaultLayout
    }

    val snapshot: Flow<List<SavedTile>> = context.layoutData.data.map { prefs ->
        decode(prefs[Keys.snapshot]) ?: defaultLayout
    }

    suspend fun setLayout(tiles: List<SavedTile>) {
        context.layoutData.edit { it[Keys.current] = json.encodeToString(tiles) }
    }

    /**
     * Called on every Keep. The snapshot holds what the screen looked like
     * BEFORE this change, which is what makes "Put my screen back" able to undo
     * the most recent arranging session. Snapshotting the new layout instead
     * would leave that key with nothing to restore while still claiming it had
     * put the screen back.
     */
    suspend fun keep(tiles: List<SavedTile>) {
        context.layoutData.edit { prefs ->
            prefs[Keys.snapshot] = prefs[Keys.current] ?: json.encodeToString(defaultLayout)
            prefs[Keys.current] = json.encodeToString(tiles)
        }
    }

    /**
     * The standing "Put my screen back". Returns true only when it genuinely
     * changed something, so the screen can say what actually happened rather
     * than confirming an action it did not take.
     */
    suspend fun restoreSnapshot(): Boolean {
        var changed = false
        context.layoutData.edit { prefs ->
            val snap = prefs[Keys.snapshot]
            if (snap != null && snap != prefs[Keys.current]) {
                prefs[Keys.current] = snap
                changed = true
            }
        }
        return changed
    }
}
