package com.bhardwaj.passkey.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.bhardwaj.passkey.data.entity.Details
import com.bhardwaj.passkey.data.entity.Preview
import com.bhardwaj.passkey.data.repository.DetailsRepository
import com.bhardwaj.passkey.data.repository.PreviewRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val previewRepository: PreviewRepository,
    private val detailsRepository: DetailsRepository
) : ViewModel() {
    val allPreviews = previewRepository.allPreviews.asLiveData()

    fun insertPreview(preview: Preview) {
        viewModelScope.launch(Dispatchers.IO) {
            previewRepository.insertPreview(preview = preview)
        }
    }

    fun deletePreview(preview: Preview) {
        viewModelScope.launch(Dispatchers.IO) {
            previewRepository.deletePreview(preview = preview)
        }
    }

    val allDetails = detailsRepository.allDetails.asLiveData()

    fun insertDetails(details: Details) {
        viewModelScope.launch(Dispatchers.IO) {
            detailsRepository.insertDetails(details = details)
        }
    }

    fun deleteDetails(details: Details) {
        viewModelScope.launch(Dispatchers.IO) {
            detailsRepository.deleteDetails(details = details)
        }
    }
}