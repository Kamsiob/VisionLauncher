package io.github.kamsiob.launcher.messages

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/**
 * One message the listener saw.
 *
 * Everything here stays on the phone. There is no network permission and a
 * build gate enforces that, so there is nowhere for this to go even by mistake.
 * See MASTER_SPEC section 2 and DECISIONS D4.
 */
@Entity(tableName = "messages")
data class StoredMessage(
    @PrimaryKey val id: String,
    val sender: String,
    val body: String,
    val appLabel: String,
    val packageName: String,
    val postedAt: Long,
    /** True when the platform hid the content from us, not when it was empty. */
    val redacted: Boolean,
    /** The notification key, so a reply can find the live notification again. */
    val notificationKey: String?,
    val canReply: Boolean,
    val readAt: Long? = null,
)

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages ORDER BY postedAt DESC LIMIT 500")
    fun recent(): Flow<List<StoredMessage>>

    @Query("SELECT COUNT(*) FROM messages WHERE readAt IS NULL AND postedAt >= :since")
    fun unreadSince(since: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(message: StoredMessage)

    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun byId(id: String): StoredMessage?

    @Query("UPDATE messages SET readAt = :at WHERE id = :id")
    suspend fun markRead(id: String, at: Long)

    /**
     * A reply action is only valid while its notification is alive. When one is
     * dismissed the row stays, because an inbox that forgets is the trap D4 was
     * written about, but it stops claiming it can reply.
     */
    @Query("UPDATE messages SET canReply = 0, notificationKey = NULL WHERE notificationKey = :key")
    suspend fun forgetReplyAction(key: String)

    @Query("DELETE FROM messages WHERE postedAt < :before")
    suspend fun trim(before: Long)
}

@Database(entities = [StoredMessage::class], version = 1, exportSchema = true)
abstract class MessageDatabase : RoomDatabase() {
    abstract fun messages(): MessageDao

    companion object {
        @Volatile private var instance: MessageDatabase? = null

        fun get(context: Context): MessageDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                MessageDatabase::class.java,
                "messages",
            ).build().also { instance = it }
        }
    }
}
