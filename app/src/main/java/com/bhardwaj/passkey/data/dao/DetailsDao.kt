package com.bhardwaj.passkey.data.dao

import androidx.room.*
import com.bhardwaj.passkey.data.Categories
import com.bhardwaj.passkey.data.entity.Details
import kotlinx.coroutines.flow.Flow

@Dao
interface DetailsDao {

    @Query("SELECT * FROM details_table ORDER BY priority ASC")
    fun getDetails(): Flow<List<Details>>

    @Query("SELECT * FROM details_table")
    fun getDetailsForExport(): Flow<List<Details>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetails(details: Details)

    @Delete
    suspend fun deleteDetail(details: Details)

    @Query("DELETE FROM details_table WHERE categoryName=:categoryName AND headingName=:headingName")
    suspend fun deleteDetailWithConditions(headingName: String, categoryName: Categories)
}