package com.bhardwaj.passkey.viewModels

import android.app.Application
import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.bhardwaj.passkey.PassKeyApplication
import com.bhardwaj.passkey.R
import com.bhardwaj.passkey.data.Categories
import com.bhardwaj.passkey.data.entity.Details
import com.bhardwaj.passkey.data.entity.Preview
import com.bhardwaj.passkey.data.repository.PassKeyRepository
import com.bhardwaj.passkey.utils.Constants
import com.bhardwaj.passkey.utils.Constants.Companion.FILE_HEADER
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val passKeyRepository: PassKeyRepository, application: Application
) : AndroidViewModel(application) {
    val allPreviews = passKeyRepository.allPreviews.asLiveData()

    fun insertPreview(preview: Preview) {
        viewModelScope.launch(Dispatchers.IO) {
            passKeyRepository.insertPreview(preview = preview)
        }
    }

    fun updatePreview(preview: Preview) {
        viewModelScope.launch(Dispatchers.IO) {
            passKeyRepository.updatePreview(preview = preview)
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
    private val allDetailsForExport = passKeyRepository.allDetailsForExport.asLiveData()

    fun insertDetails(details: Details) {
        viewModelScope.launch(Dispatchers.IO) {
            passKeyRepository.insertDetails(details = details)
        }
    }

    fun updateDetails(details: Details) {
        viewModelScope.launch(Dispatchers.IO) {
            passKeyRepository.updateDetails(details = details)
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

    fun importData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        getApplication(),
                        "Coming Soon.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun exportData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                var dataToExport = FILE_HEADER

                allDetailsForExport.value?.forEach { single ->
                    dataToExport += "${single.question},${single.answer},${single.headingName},${single.categoryName}\n"
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, Constants.FILE_NAME)
                        put(MediaStore.MediaColumns.MIME_TYPE, "text/adi")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }

                    val resolver = getApplication<PassKeyApplication>().contentResolver
                    val uri =
                        resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

                    if (uri != null) {
                        resolver.openOutputStream(uri, "wt").use { output ->
                            output?.write(dataToExport.toByteArray())
                        }
                    }

                } else {
                    val target = File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                        Constants.FILE_NAME
                    )

                    FileOutputStream(target).use { output ->
                        output.write(dataToExport.toByteArray())
                    }
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        getApplication(),
                        getApplication<PassKeyApplication>().getString(R.string.export_download),
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}