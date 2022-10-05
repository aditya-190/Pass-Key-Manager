package com.bhardwaj.passkey.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.bhardwaj.passkey.data.Categories
import com.bhardwaj.passkey.utils.Constants.Companion.PREVIEW_TABLE

@Entity(tableName = PREVIEW_TABLE)
data class Preview(
    @PrimaryKey(autoGenerate = true)
    val previewId: Long,
    val heading: String,
    val categoryName: Categories,
    val priority: Int
)