package com.bhardwaj.passkey.viewModels

import android.app.Application
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
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
import com.opencsv.CSVReaderBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
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

    fun importData(fileUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resolver = getApplication<PassKeyApplication>().contentResolver
                val csvReader =
                    CSVReaderBuilder(InputStreamReader(resolver.openInputStream(fileUri)))
                        .withSkipLines(1)
                        .build()

                val allPreviews = mutableListOf<Preview>()
                val allDetails = mutableListOf<Details>()
                var current: Array<String>?
                while (csvReader.readNext().also { current = it } != null) {
                    Log.d("ADITYA", "importData: ${current!![0]}")
                }
                csvReader.close()
                passKeyRepository.insertAllPreview(allPreviews = allPreviews)
                passKeyRepository.insertAllDetails(allDetails = allDetails)

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        getApplication(),
                        getApplication<PassKeyApplication>().getString(R.string.import_success),
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        getApplication(),
                        getApplication<PassKeyApplication>().getString(R.string.import_failed),
                        Toast.LENGTH_LONG
                    ).show()
                }
                e.printStackTrace()
            }
        }
    }

    fun exportData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val fileName =
                    "${Constants.FILE_NAME}_${System.currentTimeMillis()}.${Constants.FILE_TYPE}"
                var dataToExport = Constants.FILE_HEADER

                passKeyRepository.getDetailsForExport().forEach { single ->
                    dataToExport += "${single.categoryName},${single.headingName},${single.question},${single.answer}\n"
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
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
                        fileName
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
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        getApplication(),
                        getApplication<PassKeyApplication>().getString(R.string.something_went_wrong),
                        Toast.LENGTH_LONG
                    ).show()
                }
                e.printStackTrace()
            }
        }
    }
}