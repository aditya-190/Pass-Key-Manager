package com.bhardwaj.passkey.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bhardwaj.passkey.data.entity.Language
import com.bhardwaj.passkey.databinding.LanguageSheetBinding
import com.bhardwaj.passkey.ui.adapter.LanguageAdapter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class LanguageFragment(
    private var onItemClicked: ((languageId: String, comingSoon: Boolean) -> Unit)
) : BottomSheetDialogFragment() {
    private var binding: LanguageSheetBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = LanguageSheetBinding.inflate(layoutInflater)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val languageList = arrayListOf(
            Language(
                languageId = "en",
                languageName = "English",
                languageNameInEnglish = "English",
                comingSoon = false
            ),
            Language(
                languageId = "hi",
                languageName = "हिन्दी",
                languageNameInEnglish = "Hindi",
                comingSoon = false
            ),
            Language(
                languageId = "de",
                languageName = "Deutsch",
                languageNameInEnglish = "German",
                comingSoon = false
            ),
            Language(
                languageId = "mr",
                languageName = "मराठी",
                languageNameInEnglish = "Marathi",
                comingSoon = true
            ),
            Language(
                languageId = "bn",
                languageName = "বাংলা",
                languageNameInEnglish = "Bengali",
                comingSoon = true
            ),
            Language(
                languageId = "te",
                languageName = "తెలుగు",
                languageNameInEnglish = "Telugu",
                comingSoon = true
            ),
            Language(
                languageId = "ta",
                languageName = "தமிழ்",
                languageNameInEnglish = "Tamil",
                comingSoon = true
            ),
            Language(
                languageId = "gu",
                languageName = "ગુજરાતી",
                languageNameInEnglish = "Gujarati",
                comingSoon = true
            ),
            Language(
                languageId = "zh",
                languageName = "中文",
                languageNameInEnglish = "Chinese",
                comingSoon = true
            ),
            Language(
                languageId = "es",
                languageName = "Español",
                languageNameInEnglish = "Spanish",
                comingSoon = true
            ),
            Language(
                languageId = "pt",
                languageName = "Português",
                languageNameInEnglish = "Portuguese",
                comingSoon = true
            ),
            Language(
                languageId = "ru",
                languageName = "Русский",
                languageNameInEnglish = "Russian",
                comingSoon = true
            ),
            Language(
                languageId = "ja",
                languageName = "日本語",
                languageNameInEnglish = "Japanese",
                comingSoon = true
            ),
            Language(
                languageId = "ko",
                languageName = "한국어",
                languageNameInEnglish = "Korean",
                comingSoon = true
            ),
            Language(
                languageId = "fr",
                languageName = "Français",
                languageNameInEnglish = "French",
                comingSoon = true
            ),
            Language(
                languageId = "it",
                languageName = "Italiano",
                languageNameInEnglish = "Italian",
                comingSoon = true
            ),
        )

        binding?.rvLanguages.also {
            it?.layoutManager =
                LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
            it?.adapter = LanguageAdapter(languageList) { languageId, comingSoon ->
                dismiss()
                onItemClicked(languageId, comingSoon)
            }
            it?.addItemDecoration(
                DividerItemDecoration(activity, DividerItemDecoration.VERTICAL)
            )
        }

        binding?.ivCloseButton?.setOnClickListener {
            dismiss()
        }
    }
}