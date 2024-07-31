package com.bhardwaj.passkey.domain.viewModels

import android.app.Application
import android.app.LocaleManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bhardwaj.passkey.R
import com.bhardwaj.passkey.data.local.entity.Details
import com.bhardwaj.passkey.data.local.entity.Preview
import com.bhardwaj.passkey.data.repository.DataStoreRepository
import com.bhardwaj.passkey.data.repository.PasskeyRepository
import com.bhardwaj.passkey.domain.events.SettingsEvents
import com.bhardwaj.passkey.utils.AlertBy.ABOUT
import com.bhardwaj.passkey.utils.AlertBy.PRIVACY
import com.bhardwaj.passkey.utils.AlertBy.TERMS_N_CONDITIONS
import com.bhardwaj.passkey.utils.Categories
import com.bhardwaj.passkey.utils.Constants.Companion.FILE_HEADER
import com.bhardwaj.passkey.utils.Constants.Companion.FILE_NAME
import com.bhardwaj.passkey.utils.Constants.Companion.FILE_TYPE
import com.bhardwaj.passkey.utils.UiEvents
import com.bhardwaj.passkey.utils.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.StringReader
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: PasskeyRepository,
    private val dataStoreRepository: DataStoreRepository,
    private val appContext: Application,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val _uiEvents = Channel<UiEvents>()
    val uiEvents = _uiEvents.receiveAsFlow()

    var appVersion: String by mutableStateOf(
        appContext.packageManager.getPackageInfo(
            appContext.packageName,
            0
        ).versionName ?: "0.0.1"
    )
        private set

    var bottomSheetOpenedBy by mutableStateOf(PRIVACY)
        private set

    var isSheetOpen by mutableStateOf(false)
        private set

    fun onEvent(event: SettingsEvents) {
        when (event) {
            SettingsEvents.OnLanguageClick -> {
                isSheetOpen = true
            }

            SettingsEvents.OnDismissBottomSheet -> {
                isSheetOpen = false
            }

            SettingsEvents.OnRateAppClick -> {
                val appPackageName = appContext.packageName
                val intent =
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")
                    )
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                appContext.startActivity(intent)
            }

            SettingsEvents.OnPrivacyClick -> {
                bottomSheetOpenedBy = PRIVACY
            }

            SettingsEvents.OnTermsAndConditionClick -> {
                bottomSheetOpenedBy = TERMS_N_CONDITIONS
            }

            SettingsEvents.OnAboutClick -> {
                bottomSheetOpenedBy = ABOUT
            }

            is SettingsEvents.OnLanguageChange -> {
                isSheetOpen = false
                if (event.newLanguage.comingSoon) {
                    sendUiEvents(
                        UiEvents.ShowSnackBar(
                            message = UiText.StringResource(R.string.coming_soon)
                                .asString(context = appContext)
                        )
                    )
                } else {
                    viewModelScope.launch {
                        dataStoreRepository.savePreference(
                            key = DataStoreRepository.currentLanguageKey,
                            value = event.newLanguage.languageId
                        )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            appContext.getSystemService(LocaleManager::class.java).applicationLocales =
                                LocaleList.forLanguageTags(event.newLanguage.languageId)
                        } else {
                            AppCompatDelegate.setApplicationLocales(
                                LocaleListCompat.forLanguageTags(
                                    event.newLanguage.languageId
                                )
                            )
                        }
                    }
                }
            }

            SettingsEvents.OnExportDataClick -> {
                viewModelScope.launch {
                    val fileName = "${FILE_NAME}_${System.currentTimeMillis()}.${FILE_TYPE}"

                    println("TESTING NAME -> $fileName")

                    if (exportDataToStorage(fileName)) {
                        sendUiEvents(
                            UiEvents.ShowSnackBar(
                                message = UiText.StringResource(
                                    R.string.export_download
                                ).asString(appContext)
                            )
                        )
                    } else {
                        sendUiEvents(
                            UiEvents.ShowSnackBar(
                                message = UiText.StringResource(
                                    R.string.something_went_wrong
                                ).asString(appContext)
                            )
                        )
                    }
                }
            }

            is SettingsEvents.OnImportDataClick -> {
                viewModelScope.launch {
                    if (importDateFromStorage(event.content)) {
                        sendUiEvents(
                            UiEvents.ShowSnackBar(
                                message = UiText.StringResource(
                                    R.string.import_success
                                ).asString(appContext)
                            )
                        )
                    } else {
                        sendUiEvents(
                            UiEvents.ShowSnackBar(
                                message = UiText.StringResource(
                                    R.string.import_failed
                                ).asString(appContext)
                            )
                        )
                    }
                }
            }
        }
    }

    private fun sendUiEvents(events: UiEvents) {
        viewModelScope.launch {
            _uiEvents.send(events)
        }
    }

    private suspend fun exportDataToStorage(fileName: String): Boolean {
        return try {
            val allPreviews = repository.getPreviews().first()

            println("TESTING -> $allPreviews")

            val csvBuilder = StringBuilder()
            csvBuilder.append(FILE_HEADER)
            allPreviews.forEach { preview ->
                val previewId = preview.previewId!!
                val details = repository.getDetailsByPreviewId(previewId).first()
                details.forEach {
                    csvBuilder.append("${preview.categoryName},${preview.heading},${it.question},${it.answer}\n")
                }
            }

            val downloadsDirectory =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

            val file = File(downloadsDirectory, "${fileName}.${FILE_TYPE}")
            file.writeText(csvBuilder.toString())
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private suspend fun importDateFromStorage(content: String): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                val reader = BufferedReader(StringReader(content))
                reader.readLine()

                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val values = line!!.split(",")
                    if (values.size != 4) {
                        continue
                    }

                    val category = values[0].trim()
                    val heading = values[1].trim()
                    val question = values[2].trim()
                    val answer = values[3].trim()

                    val previewId = repository.upsertPreview(
                        Preview(
                            heading = heading,
                            categoryName = Categories.valueOf(category)
                        )
                    )

                    repository.upsertDetails(
                        Details(
                            previewId = previewId,
                            question = question,
                            answer = answer
                        )
                    )
                }
                reader.close()
                return@withContext true
            }
        } catch (e: Exception) {
            return false
        }
    }
}