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
    suspend fun deletePreview(
        preview: Preview,
        heading: String,
        categoryName: Categories,
        initialPosition: Int,
        finalPosition: Int
    ) {
        previewDao.deletePreview(previews = preview)
        previewDao.decrementPriority(
            initialPosition = initialPosition,
            finalPosition = finalPosition,
            categoryName = categoryName
        )
        detailsDao.deleteDetailWithConditions(
            headingName = heading,
            categoryName = categoryName
        )
    }

    suspend fun incrementPriority(
        initialPosition: Int,
        finalPosition: Int,
        headingName: String,
        categoryName: Categories
    ) {
        previewDao.incrementPriority(
            initialPosition = initialPosition,
            finalPosition = finalPosition,
            categoryName = categoryName
        )
        previewDao.changePriority(
            initialPosition = initialPosition + 1,
            finalPosition = finalPosition,
            headingName = headingName,
            categoryName = categoryName
        )
    }

    suspend fun decrementPriority(
        initialPosition: Int,
        finalPosition: Int,
        headingName: String,
        categoryName: Categories
    ) {
        previewDao.decrementPriority(
            initialPosition = initialPosition,
            finalPosition = finalPosition,
            categoryName = categoryName
        )
        previewDao.changePriority(
            initialPosition = initialPosition - 1,
            finalPosition = finalPosition,
            headingName = headingName,
            categoryName = categoryName
        )
    }

    val allDetails = detailsDao.getDetails()
    val allDetailsForExport = detailsDao.getDetailsForExport()
    suspend fun insertDetails(details: Details) = detailsDao.insertDetails(details = details)
    suspend fun deleteDetails(details: Details) = detailsDao.deleteDetail(details = details)
}