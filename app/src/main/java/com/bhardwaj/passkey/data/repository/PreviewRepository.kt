package com.bhardwaj.passkey.data.repository

import com.bhardwaj.passkey.data.dao.PreviewDao
import com.bhardwaj.passkey.data.entity.Preview
import javax.inject.Inject

class PreviewRepository @Inject constructor(
    private val previewDao: PreviewDao
) {
    val allPreviews = previewDao.getPreviews()
    suspend fun insertPreview(preview: Preview) = previewDao.insertPreview(previews = preview)
    suspend fun deletePreview(preview: Preview) = previewDao.deletePreview(previews = preview)
}