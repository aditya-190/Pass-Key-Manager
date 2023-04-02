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
    suspend fun getDetailsForExport(): List<Details>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetails(details: Details)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllDetails(allDetails: List<Details>)

    @Update
    suspend fun updateDetails(details: Details)

    @Delete
    suspend fun deleteDetail(details: Details)

    @Query("DELETE FROM $DETAILS_TABLE WHERE categoryName=:categoryName AND headingName=:headingName")
    suspend fun deleteDetailWithConditions(headingName: String, categoryName: Categories)

    @Query("UPDATE $DETAILS_TABLE SET priority = priority + 1 WHERE categoryName=:categoryName AND headingName =:headingName AND priority BETWEEN :finalPosition AND :initialPosition")
    suspend fun incrementPriority(
        initialPosition: Int,
        finalPosition: Int,
        headingName: String,
        categoryName: Categories
    )

    @Query("UPDATE $DETAILS_TABLE SET priority = priority - 1 WHERE categoryName=:categoryName AND headingName =:headingName AND priority BETWEEN :initialPosition AND :finalPosition")
    suspend fun decrementPriority(
        initialPosition: Int,
        finalPosition: Int,
        headingName: String,
        categoryName: Categories
    )

    @Query("UPDATE $DETAILS_TABLE SET priority = :finalPosition WHERE categoryName = :categoryName AND priority = :initialPosition AND headingName =:headingName AND question =:question AND answer =:answer")
    suspend fun changePriority(
        initialPosition: Int,
        finalPosition: Int,
        headingName: String,
        categoryName: Categories,
        question: String,
        answer: String
    )
}