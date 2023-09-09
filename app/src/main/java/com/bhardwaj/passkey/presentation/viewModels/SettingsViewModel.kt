package com.bhardwaj.passkey.presentation.viewModels

import android.app.Application
import android.app.LocaleManager
import android.content.Intent
import android.net.Uri
import android.os.Build
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
import com.bhardwaj.passkey.data.repository.DataStoreRepository
import com.bhardwaj.passkey.data.repository.PasskeyRepository
import com.bhardwaj.passkey.presentation.views.events.SettingsEvents
import com.bhardwaj.passkey.utils.AlertBy.ABOUT
import com.bhardwaj.passkey.utils.AlertBy.PRIVACY
import com.bhardwaj.passkey.utils.AlertBy.TERMS_N_CONDITIONS
import com.bhardwaj.passkey.utils.Constants.Companion.FILE_NAME
import com.bhardwaj.passkey.utils.Constants.Companion.FILE_TYPE
import com.bhardwaj.passkey.utils.UiEvents
import com.bhardwaj.passkey.utils.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
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

            SettingsEvents.OnImportDataClick -> {
                viewModelScope.launch {
                    if (importDateFromStorage()) {
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

    private fun exportDataToStorage(fileName: String): Boolean {
        return false
    }

    private fun importDateFromStorage(): Boolean {
        // TODO: ADITYA (IMPLEMENT THIS)
        return false
    }
}