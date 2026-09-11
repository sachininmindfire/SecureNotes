package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val content: String,
    val category: String = "General",
    val colorKey: String = "default",
    val isPinned: Boolean = false,
    val isLocked: Boolean = false,
    val imagePaths: String = "", // Comma-separated internal image filenames
    val createdTimestamp: Long = System.currentTimeMillis(),
    val modifiedTimestamp: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false
) {
    fun getImageList(): List<String> {
        if (imagePaths.isBlank()) return emptyList()
        return imagePaths.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
}
