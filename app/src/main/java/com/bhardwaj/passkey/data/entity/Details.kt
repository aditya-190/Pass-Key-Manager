package com.bhardwaj.passkey.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.bhardwaj.passkey.data.Categories
import com.bhardwaj.passkey.utils.Constants.Companion.DETAILS_TABLE

@Entity(tableName = DETAILS_TABLE)
data class Details(
    @PrimaryKey(autoGenerate = true)
    val detailsId: Long,
    val question: String,
    val answer: String,
    val priority: Int,
    val headingName: String,
    val categoryName: Categories
)