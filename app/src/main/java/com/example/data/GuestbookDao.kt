package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GuestbookDao {
    @Query("SELECT * FROM guestbook_entries ORDER BY dateMs DESC")
    fun getAllEntries(): Flow<List<GuestbookEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: GuestbookEntry): Long

    @Delete
    suspend fun deleteEntry(entry: GuestbookEntry)

    @Query("SELECT COUNT(*) FROM guestbook_entries")
    suspend fun getCount(): Int

    @Query("SELECT * FROM guestbook_entries WHERE guestName = :name ORDER BY dateMs DESC LIMIT 1")
    suspend fun getLastEntryForGuest(name: String): GuestbookEntry?
}
