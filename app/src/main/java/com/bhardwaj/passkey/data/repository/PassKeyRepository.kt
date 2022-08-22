package com.bhardwaj.passkey.data.repository

import com.bhardwaj.passkey.data.Categories
import com.bhardwaj.passkey.data.dao.DetailsDao
import com.bhardwaj.passkey.data.dao.PreviewDao
import com.bhardwaj.passkey.data.entity.Details
import com.bhardwaj.passkey.data.entity.Preview
import javax.inject.Inject

class PassKeyRepository @Inject constructor(
    private val previewDao: PreviewDao,
    private val detailsDao: DetailsDao
) {
    val allPreviews = previewDao.getPreviews()
    suspend fun insertPreview(preview: Preview) = previewDao.insertPreview(previews = preview)
    suspend fun deletePreview(preview: Preview) = previewDao.deletePreview(previews = preview)

    val allDetails = detailsDao.getDetails()
    suspend fun insertDetails(details: Details) = detailsDao.insertDetails(details = details)
    suspend fun deleteDetails(details: Details) = detailsDao.deleteDetail(details = details)
    suspend fun deleteDetailWithConditions(headingName: String, categoryName: Categories) =
        detailsDao.deleteDetailWithConditions(
            headingName = headingName,
            categoryName = categoryName
        )
}