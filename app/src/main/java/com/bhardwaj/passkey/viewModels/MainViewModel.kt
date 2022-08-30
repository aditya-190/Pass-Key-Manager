package com.bhardwaj.passkey.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.bhardwaj.passkey.data.Categories
import com.bhardwaj.passkey.data.entity.Details
import com.bhardwaj.passkey.data.entity.Preview
import com.bhardwaj.passkey.data.repository.PassKeyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val passKeyRepository: PassKeyRepository
) : ViewModel() {
    val allPreviews = passKeyRepository.allPreviews.asLiveData()

    fun insertPreview(preview: Preview) {
        viewModelScope.launch(Dispatchers.IO) {
            passKeyRepository.insertPreview(preview = preview)
        }
    }

    fun deletePreview(
        preview: Preview,
        heading: String,
        categoryName: Categories,
        initialPosition: Int,
        finalPosition: Int
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            passKeyRepository.deletePreview(
                preview = preview,
                heading = heading,
                categoryName = categoryName,
                initialPosition = initialPosition,
                finalPosition = finalPosition
            )
        }
    }

    fun incrementPriority(
        isPreview: Boolean,
        initialPosition: Int,
        finalPosition: Int,
        headingName: String,
        categoryName: Categories,
        question: String,
        answer: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            passKeyRepository.incrementPriority(
                isPreview = isPreview,
                initialPosition = initialPosition,
                finalPosition = finalPosition,
                headingName = headingName,
                categoryName = categoryName,
                question = question,
                answer = answer
            )
        }
    }

    fun decrementPriority(
        isPreview: Boolean,
        initialPosition: Int,
        finalPosition: Int,
        headingName: String,
        categoryName: Categories,
        question: String,
        answer: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            passKeyRepository.decrementPriority(
                isPreview = isPreview,
                initialPosition = initialPosition,
                finalPosition = finalPosition,
                headingName = headingName,
                categoryName = categoryName,
                question = question,
                answer = answer
            )
        }
    }

    val allDetails = passKeyRepository.allDetails.asLiveData()
    val allDetailsForExport = passKeyRepository.allDetailsForExport.asLiveData()

    fun insertDetails(details: Details) {
        viewModelScope.launch(Dispatchers.IO) {
            passKeyRepository.insertDetails(details = details)
        }
    }

    fun deleteDetails(
        details: Details,
        headingName: String,
        categoryName: Categories,
        initialPosition: Int,
        finalPosition: Int
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            passKeyRepository.deleteDetails(
                details = details,
                headingName = headingName,
                categoryName = categoryName,
                initialPosition = initialPosition,
                finalPosition = finalPosition
            )
        }
    }
}