package com.bhardwaj.passkey.data.dao

import androidx.room.*
import com.bhardwaj.passkey.data.Categories
import com.bhardwaj.passkey.data.entity.Details
import com.bhardwaj.passkey.utils.Constants.Companion.DETAILS_TABLE
import kotlinx.coroutines.flow.Flow

@Dao
interface DetailsDao {

    @Query("SELECT * FROM $DETAILS_TABLE ORDER BY priority ASC")
    fun getDetails(): Flow<List<Details>>

    @Query("SELECT * FROM $DETAILS_TABLE")
    fun getDetailsForExport(): Flow<List<Details>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetails(details: Details)

    @Delete
    suspend fun deleteDetail(details: Details)

    @Query("DELETE FROM $DETAILS_TABLE WHERE categoryName=:categoryName AND headingName=:headingName")
    suspend fun deleteDetailWithConditions(headingName: String, categoryName: Categories)
}