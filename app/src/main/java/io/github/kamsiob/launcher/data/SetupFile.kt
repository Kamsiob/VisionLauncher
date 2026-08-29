package io.github.kamsiob.launcher.data

import io.github.kamsiob.launcher.today.TodayCard
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The whole setup, as one file a helper can save and load.
 *
 * The point is a helper who set the phone up once and does not want to do it
 * again after a factory reset, or who is setting up a second phone the same
 * way. Written as readable JSON on purpose: somebody should be able to open it
 * and see exactly what is in it, because a backup format nobody can inspect is
 * asking to be trusted blindly.
 *
 * Never transmitted. The file goes wherever the person's own file picker puts
 * it, and the app has no network to send it anywhere else.
 */
@Serializable
data class Setup(
    val version: Int = CURRENT_VERSION,
    val favorites: List<Favorite> = emptyList(),
    val emergencyContact: EmergencyContact? = null,
    val replyPhrases: List<String> = emptyList(),
    val todayCards: List<TodayCard> = emptyList(),
    val look: String? = null,
    val outlined: Boolean? = null,
    val textStep: Int? = null,
    val homeLayout: List<SavedTile> = emptyList(),
) {
    companion object {
        /**
         * Raised whenever a field is removed or changes meaning, never for an
         * added one. Unknown fields are ignored on read, so a file written by a
         * newer version still loads what this version understands.
         */
        const val CURRENT_VERSION = 1

        private val json = Json {
            ignoreUnknownKeys = true
            prettyPrint = true
            encodeDefaults = true
        }

        fun write(setup: Setup): String = json.encodeToString(
            // Completion state never travels. A setup file describes what
            // happens today, not what somebody did today, and carrying it means
            // a restored phone opens showing a pill already taken that nobody
            // took. On a second phone it would be somebody else's morning.
            setup.copy(
                todayCards = setup.todayCards.map {
                    it.copy(doneAtMinutes = null, doneOnDay = null)
                }
            )
        )

        /**
         * Returns null for anything that is not a setup file this version can
         * read, so the screen can say so rather than half applying a file and
         * leaving the phone in a state nobody chose.
         */
        fun read(text: String): Setup? {
            val setup = runCatching { json.decodeFromString<Setup>(text) }.getOrNull()
                ?: return null
            if (setup.version > CURRENT_VERSION) return null
            return setup
        }
    }
}
