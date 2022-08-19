package com.bhardwaj.passkey.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "preview_table")
data class Preview(
    @PrimaryKey(autoGenerate = true)
    val previewId: Long,
    val heading: String,
    val categoryName: String,
    val priority: Int
)