package com.example.data

import kotlinx.coroutines.flow.Flow

class GuestbookRepository(private val guestbookDao: GuestbookDao) {
    val allEntries: Flow<List<GuestbookEntry>> = guestbookDao.getAllEntries()

    suspend fun insertEntry(entry: GuestbookEntry): Long {
        return guestbookDao.insertEntry(entry)
    }

    suspend fun deleteEntry(entry: GuestbookEntry) {
        guestbookDao.deleteEntry(entry)
    }

    suspend fun getCount(): Int {
        return guestbookDao.getCount()
    }

    suspend fun getLastEntryForGuest(name: String): GuestbookEntry? {
        return guestbookDao.getLastEntryForGuest(name)
    }
}
