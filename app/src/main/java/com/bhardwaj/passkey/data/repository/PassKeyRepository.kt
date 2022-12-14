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
    suspend fun updatePreview(preview: Preview) = previewDao.updatePreview(previews = preview)
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
        isPreview: Boolean,
        initialPosition: Int,
        finalPosition: Int,
        headingName: String,
        categoryName: Categories,
        question: String,
        answer: String
    ) {
        if (isPreview) {
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
        } else {
            detailsDao.incrementPriority(
                initialPosition = initialPosition,
                finalPosition = finalPosition,
                headingName = headingName,
                categoryName = categoryName
            )
            detailsDao.changePriority(
                initialPosition = initialPosition + 1,
                finalPosition = finalPosition,
                headingName = headingName,
                categoryName = categoryName,
                question = question,
                answer = answer
            )
        }
    }

    suspend fun decrementPriority(
        isPreview: Boolean,
        initialPosition: Int,
        finalPosition: Int,
        headingName: String,
        categoryName: Categories,
        question: String,
        answer: String
    ) {
        if (isPreview) {
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
        } else {
            detailsDao.decrementPriority(
                initialPosition = initialPosition,
                finalPosition = finalPosition,
                headingName = headingName,
                categoryName = categoryName
            )
            detailsDao.changePriority(
                initialPosition = initialPosition - 1,
                finalPosition = finalPosition,
                headingName = headingName,
                categoryName = categoryName,
                question = question,
                answer = answer
            )
        }
    }

    val allDetails = detailsDao.getDetails()
    val allDetailsForExport = detailsDao.getDetailsForExport()
    suspend fun insertDetails(details: Details) = detailsDao.insertDetails(details = details)
    suspend fun updateDetails(details: Details) = detailsDao.updateDetails(details = details)
    suspend fun deleteDetails(
        details: Details,
        headingName: String,
        categoryName: Categories,
        initialPosition: Int,
        finalPosition: Int
    ) {
        detailsDao.deleteDetail(details = details)
        detailsDao.decrementPriority(
            initialPosition = initialPosition,
            finalPosition = finalPosition,
            headingName = headingName,
            categoryName = categoryName
        )
    }
}