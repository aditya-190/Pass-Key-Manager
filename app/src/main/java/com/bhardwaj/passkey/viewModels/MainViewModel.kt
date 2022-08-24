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

    fun deletePreview(preview: Preview) {
        viewModelScope.launch(Dispatchers.IO) {
            passKeyRepository.deletePreview(preview = preview)
        }
    }

    val allDetails = passKeyRepository.allDetails.asLiveData()
    val allDetailsForExport = passKeyRepository.allDetailsForExport.asLiveData()

    fun insertDetails(details: Details) {
        viewModelScope.launch(Dispatchers.IO) {
            passKeyRepository.insertDetails(details = details)
        }
    }

    fun deleteDetails(details: Details) {
        viewModelScope.launch(Dispatchers.IO) {
            passKeyRepository.deleteDetails(details = details)
        }
    }

    fun deleteDetailWithConditions(headingName: String, categoryName: Categories) {
        viewModelScope.launch(Dispatchers.IO) {
            passKeyRepository.deleteDetailWithConditions(
                headingName = headingName,
                categoryName = categoryName
            )
        }
    }
}