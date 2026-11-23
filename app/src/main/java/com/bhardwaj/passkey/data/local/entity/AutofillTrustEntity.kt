package com.bhardwaj.passkey.data.local.entity

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.bhardwaj.passkey.utils.Constants.Companion.AUTOFILL_TRUST_TABLE

/**
 * The signer an app had the first time PassKey filled it (trust on first use, see AppTrust). Kept inside the
 * encrypted vault: which apps a user fills is itself private.
 */
@Keep
@Entity(tableName = AUTOFILL_TRUST_TABLE)
data class AutofillTrustEntity(
    @PrimaryKey
    val packageName: String,
    val signer: String,
    val firstSeen: Long
)
