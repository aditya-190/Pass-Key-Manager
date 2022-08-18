package com.bhardwaj.passkey.data.dao

import androidx.room.*
import com.bhardwaj.passkey.data.entity.Details
import kotlinx.coroutines.flow.Flow

@Dao
interface DetailsDao {

    @Query("SELECT * FROM details_table")
    fun getDetails(): Flow<List<Details>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDetails(details: Details)

    @Delete
    suspend fun deleteDetail(details: Details)
}