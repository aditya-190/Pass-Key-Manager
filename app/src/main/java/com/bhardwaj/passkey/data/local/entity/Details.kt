package com.bhardwaj.passkey.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.bhardwaj.passkey.utils.Constants.Companion.DETAILS_TABLE

@Entity(tableName = DETAILS_TABLE)
data class Details(
    @PrimaryKey(autoGenerate = true)
    val detailsId: Long? = null,
    val previewId: Long,
    val question: String,
    val answer: String,
    val sequence: Long = 0
)