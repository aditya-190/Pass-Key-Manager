package com.bhardwaj.passkey.data.local.entity

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.bhardwaj.passkey.utils.Categories
import com.bhardwaj.passkey.utils.Constants

@Keep
@Entity(tableName = Constants.PREVIEW_TABLE)
data class Preview(
    @PrimaryKey(autoGenerate = true)
    val previewId: Long? = null,
    var heading: String,
    val categoryName: Categories,
    val sequence: Long = 0
)