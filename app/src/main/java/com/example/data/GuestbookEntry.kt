package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "guestbook_entries")
data class GuestbookEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val guestName: String,
    val story: String,
    val relationshipType: String, // "Family", "Friend", "Someone Special", "Myself"
    val cakeBaseId: String,
    val frostingFlavorId: String,
    val dateMs: Long,
    val imagePath: String,
    val note: String
)
