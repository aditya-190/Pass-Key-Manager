package com.bhardwaj.passkey.data.dao

import androidx.room.*
import com.bhardwaj.passkey.data.Categories
import com.bhardwaj.passkey.data.entity.Preview
import com.bhardwaj.passkey.utils.Constants.Companion.PREVIEW_TABLE
import kotlinx.coroutines.flow.Flow

@Dao
interface PreviewDao {

    @Query("SELECT * FROM $PREVIEW_TABLE ORDER BY priority ASC")
    fun getPreviews(): Flow<List<Preview>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreview(previews: Preview)

    @Update
    suspend fun updatePreview(previews: Preview)

    @Delete
    suspend fun deletePreview(previews: Preview)

    @Query("UPDATE $PREVIEW_TABLE SET priority = priority + 1 WHERE categoryName=:categoryName AND priority BETWEEN :finalPosition AND :initialPosition")
    suspend fun incrementPriority(
        initialPosition: Int,
        finalPosition: Int,
        categoryName: Categories
    )

    @Query("UPDATE $PREVIEW_TABLE SET priority = priority - 1 WHERE categoryName=:categoryName AND priority BETWEEN :initialPosition AND :finalPosition")
    suspend fun decrementPriority(
        initialPosition: Int,
        finalPosition: Int,
        categoryName: Categories
    )

    @Query("UPDATE $PREVIEW_TABLE SET priority = :finalPosition WHERE categoryName = :categoryName AND priority = :initialPosition AND heading =:headingName")
    suspend fun changePriority(
        initialPosition: Int,
        finalPosition: Int,
        headingName: String,
        categoryName: Categories
    )
}