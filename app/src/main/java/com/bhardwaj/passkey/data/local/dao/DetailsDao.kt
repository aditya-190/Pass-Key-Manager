package com.bhardwaj.passkey.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.bhardwaj.passkey.data.local.entity.Details
import com.bhardwaj.passkey.utils.Constants.Companion.DETAILS_TABLE
import kotlinx.coroutines.flow.Flow

@Dao
interface DetailsDao {
    @Query("SELECT * FROM $DETAILS_TABLE ORDER BY sequence ASC")
    fun getDetails(): Flow<List<Details>>

    @Query("SELECT * FROM $DETAILS_TABLE WHERE previewId=:previewId ORDER BY sequence ASC")
    fun getDetailsByPreviewId(previewId: Long): Flow<List<Details>>

    @Query("SELECT * FROM $DETAILS_TABLE WHERE detailsId=:detailId")
    suspend fun getDetailById(detailId: Long): Details?

    @Query("SELECT * FROM $DETAILS_TABLE WHERE previewId=:previewId AND question=:question AND answer=:answer")
    suspend fun getDetailByContent(previewId: Long, question: String, answer: String): Details?

    @Upsert
    suspend fun upsertDetails(details: Details): Long

    @Delete
    suspend fun deleteDetail(details: Details)

    @Query("DELETE FROM $DETAILS_TABLE WHERE previewId=:previewId")
    suspend fun deleteDetailByPreviewId(previewId: Long)

    @Query("UPDATE $DETAILS_TABLE SET sequence=:sequence WHERE detailsId=:detailsId")
    suspend fun updateDetailSequence(detailsId: Long, sequence: Long)
}