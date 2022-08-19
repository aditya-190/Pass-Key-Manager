package com.bhardwaj.passkey.data.dao

import androidx.room.*
import com.bhardwaj.passkey.data.entity.Details
import kotlinx.coroutines.flow.Flow

@Dao
interface DetailsDao {

    @Query("SELECT * FROM details_table ORDER BY priority ASC")
    fun getDetails(): Flow<List<Details>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetails(details: Details)

    @Delete
    suspend fun deleteDetail(details: Details)
}