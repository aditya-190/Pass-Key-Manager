package com.bhardwaj.passkey.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.bhardwaj.passkey.data.Categories

@Entity(tableName = "preview_table")
data class Preview(
    @PrimaryKey(autoGenerate = true)
    val previewId: Long,
    val heading: String,
    val categoryName: Categories,
    val priority: Int
)