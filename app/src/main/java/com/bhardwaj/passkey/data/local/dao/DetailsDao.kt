package com.bhardwaj.passkey.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.bhardwaj.passkey.data.local.entity.Details
import com.bhardwaj.passkey.utils.Constants.Companion.DETAILS_TABLE
import kotlinx.coroutines.flow.Flow

@Dao
interface DetailsDao {
    @Query("SELECT * FROM $DETAILS_TABLE ORDER BY detailsId ASC")
    fun getDetails(): Flow<List<Details>>

    @Query("SELECT * FROM $DETAILS_TABLE WHERE previewId=:previewId ORDER BY detailsId ASC")
    fun getDetailsByPreviewId(previewId: Long): Flow<List<Details>>

    @Query("SELECT * FROM $DETAILS_TABLE WHERE detailsId=:detailId")
    suspend fun getDetailById(detailId: Long): Details?

    @Upsert
    suspend fun upsertDetails(details: Details)

    @Delete
    suspend fun deleteDetail(details: Details)

    @Query("DELETE FROM $DETAILS_TABLE WHERE previewId=:previewId")
    suspend fun deleteDetailByPreviewId(previewId: Long)
}