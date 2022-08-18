package com.bhardwaj.passkey.data.repository

import com.bhardwaj.passkey.data.dao.DetailsDao
import com.bhardwaj.passkey.data.entity.Details
import javax.inject.Inject

class DetailsRepository @Inject constructor(
    private val detailsDao: DetailsDao
) {
    val allDetails = detailsDao.getDetails()
    suspend fun insertDetails(details: Details) = detailsDao.insertDetails(details = details)
    suspend fun deleteDetails(details: Details) = detailsDao.deleteDetail(details = details)
}