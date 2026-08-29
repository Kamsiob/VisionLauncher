package io.github.kamsiob.launcher.today

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Calendar

/**
 * A corruption handler, because without one a truncated file throws out of the
 * flow before any decode runs. Losing this file resets it to defaults, which is
 * recoverable; a launcher that will not open is not.
 */
private val Context.todayData by preferencesDataStore(
    name = "today",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

/**
 * One thing happening today, written by the person or their helper.
 *
 * Deliberately free text. This app contains no medical content, no dosing
 * advice, and no drug information, and it never will. A card saying "Blood
 * pressure pill" is a reminder somebody wrote for themselves; a card the app
 * generated would be medical advice. See MASTER_SPEC 5.9.
 */
@Serializable
data class TodayCard(
    val id: Int,
    val hour: Int,
    val minute: Int,
    val what: String,
    /**
     * Minutes past midnight when it was marked done, or null. Stored as a time
     * of day plus the day it belongs to so a card resets tomorrow rather than
     * staying green forever.
     */
    val doneAtMinutes: Int? = null,
    val doneOnDay: Int? = null,
) {
    fun isDoneToday(today: Int): Boolean = doneAtMinutes != null && doneOnDay == today
}

/** The cards, kept on the device like everything else. */
class TodayStore(private val context: Context) {

    private val key = stringPreferencesKey("cards")
    private val json = Json { ignoreUnknownKeys = true }

    val cards: Flow<List<TodayCard>> = context.todayData.data.map { prefs ->
        prefs[key]?.let { stored ->
            runCatching { json.decodeFromString<List<TodayCard>>(stored) }.getOrDefault(emptyList())
        }?.sortedWith(compareBy({ it.hour }, { it.minute })) ?: emptyList()
    }

    suspend fun current(): List<TodayCard> = cards.first()

    private suspend fun write(list: List<TodayCard>) {
        context.todayData.edit { it[key] = json.encodeToString(list) }
    }

    suspend fun save(card: TodayCard) {
        val list = current().toMutableList()
        val index = list.indexOfFirst { it.id == card.id }
        if (index >= 0) list[index] = card else list.add(card)
        write(list)
    }

    suspend fun delete(id: Int) = write(current().filterNot { it.id == id })

    suspend fun nextId(): Int = (current().maxOfOrNull { it.id } ?: 0) + 1

    /**
     * Marks a card done, or undoes it. Nothing in this app is one way, and a
     * mis-tapped Done on a medication reminder is exactly the kind of mistake
     * somebody needs to be able to take back.
     */
    suspend fun setDone(id: Int, done: Boolean, now: Calendar = Calendar.getInstance()) {
        val card = current().firstOrNull { it.id == id } ?: return
        save(
            if (done) {
                card.copy(
                    doneAtMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE),
                    doneOnDay = dayKey(now),
                )
            } else {
                card.copy(doneAtMinutes = null, doneOnDay = null)
            }
        )
    }

    companion object {
        /**
         * The day a completion belongs to, as one number.
         *
         * Year times 1000 plus day of year, so it survives a year boundary. A
         * plain day of year would make January 1 look like the same day as the
         * previous January 1, and every card would still read as done.
         */
        fun dayKey(now: Calendar = Calendar.getInstance()): Int =
            now.get(Calendar.YEAR) * 1000 + now.get(Calendar.DAY_OF_YEAR)
    }
}
