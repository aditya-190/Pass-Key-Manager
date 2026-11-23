package com.bhardwaj.passkey.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bhardwaj.passkey.data.local.entity.AutofillTrustEntity
import com.bhardwaj.passkey.utils.Constants.Companion.AUTOFILL_TRUST_TABLE

@Dao
interface AutofillTrustDao {
    @Query("SELECT signer FROM $AUTOFILL_TRUST_TABLE WHERE packageName=:packageName")
    suspend fun signerOf(packageName: String): String?

    /** IGNORE: the first signer wins; a later one never overwrites it. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entry: AutofillTrustEntity)
}
