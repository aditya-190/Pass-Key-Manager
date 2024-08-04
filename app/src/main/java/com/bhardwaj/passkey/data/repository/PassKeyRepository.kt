package com.bhardwaj.passkey.data.repository

import com.bhardwaj.passkey.data.local.entity.Details
import com.bhardwaj.passkey.data.local.entity.Preview
import kotlinx.coroutines.flow.Flow

interface PasskeyRepository {
    // Details
    fun getDetails(): Flow<List<Details>>

    fun getDetailsByPreviewId(previewId: Long): Flow<List<Details>>

    suspend fun getDetailById(detailId: Long): Details?

    suspend fun getDetailByContent(previewId: Long, question: String, answer: String): Details?

    suspend fun upsertDetails(details: Details): Long

    suspend fun deleteDetail(details: Details)

    suspend fun deleteDetailByPreviewId(previewId: Long)

    // Preview
    fun getPreviews(): Flow<List<Preview>>

    suspend fun getPreviewById(previewId: Long): Preview?

    suspend fun getPreviewByHeading(previewHeading: String): Preview?

    suspend fun upsertPreview(previews: Preview): Long

    suspend fun deletePreview(previews: Preview)
}