package com.bhardwaj.passkey.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.bhardwaj.passkey.data.local.entity.Preview
import com.bhardwaj.passkey.utils.Constants.Companion.PREVIEW_TABLE
import kotlinx.coroutines.flow.Flow

@Dao
interface PreviewDao {
    @Query("SELECT * FROM $PREVIEW_TABLE ORDER BY previewId ASC")
    fun getPreviews(): Flow<List<Preview>>

    @Query("SELECT * FROM $PREVIEW_TABLE WHERE previewId=:previewId")
    suspend fun getPreviewById(previewId: Long): Preview?

    @Upsert
    suspend fun upsertPreview(previews: Preview)

    @Delete
    suspend fun deletePreview(previews: Preview)
}