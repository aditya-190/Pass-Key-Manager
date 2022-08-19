package com.bhardwaj.passkey.data.dao

import androidx.room.*
import com.bhardwaj.passkey.data.entity.Preview
import kotlinx.coroutines.flow.Flow

@Dao
interface PreviewDao {

    @Query("SELECT * FROM preview_table ORDER BY priority ASC")
    fun getPreviews(): Flow<List<Preview>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreview(previews: Preview)

    @Delete
    suspend fun deletePreview(previews: Preview)
}