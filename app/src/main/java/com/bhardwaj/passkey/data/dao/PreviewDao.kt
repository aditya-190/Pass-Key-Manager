package com.bhardwaj.passkey.data.dao

import androidx.room.*
import com.bhardwaj.passkey.data.entity.Preview
import com.bhardwaj.passkey.utils.Constants.Companion.PREVIEW_TABLE
import kotlinx.coroutines.flow.Flow

@Dao
interface PreviewDao {

    @Query("SELECT * FROM $PREVIEW_TABLE ORDER BY priority ASC")
    fun getPreviews(): Flow<List<Preview>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreview(previews: Preview)

    @Delete
    suspend fun deletePreview(previews: Preview)
}