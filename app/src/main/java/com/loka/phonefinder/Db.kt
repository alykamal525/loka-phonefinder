package com.loka.phonefinder

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "history")
data class HistoryEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val input: String,
    val e164: String,
    val region: String,
    val mode: String,
    val createdAt: Long
)

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY createdAt DESC")
    fun all(): Flow<List<HistoryEntry>>

    @Insert
    suspend fun insert(e: HistoryEntry)

    @Query("DELETE FROM history")
    suspend fun clear()

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun delete(id: Long)
}

@Database(entities = [HistoryEntry::class], version = 1, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun dao(): HistoryDao

    companion object {
        @Volatile
        private var inst: AppDb? = null

        fun get(c: Context): AppDb = inst ?: synchronized(this) {
            inst ?: Room.databaseBuilder(c.applicationContext, AppDb::class.java, "loka.db")
                .build().also { inst = it }
        }
    }
}
