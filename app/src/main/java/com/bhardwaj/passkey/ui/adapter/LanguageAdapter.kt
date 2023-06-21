package com.bhardwaj.passkey.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.bhardwaj.passkey.R
import com.bhardwaj.passkey.data.entity.Language

class LanguageAdapter(
    private val language: ArrayList<Language>,
    private var onItemClicked: ((languageId: String, comingSoon: Boolean) -> Unit)
) : RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder>() {

    inner class LanguageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val clSingleLanguage: ConstraintLayout = itemView.findViewById(R.id.clSingleLanguage)
        val tvLanguage: TextView = itemView.findViewById(R.id.tvLanguage)
        val tvLanguageInEnglish: TextView = itemView.findViewById(R.id.tvLanguageInEnglish)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): LanguageAdapter.LanguageViewHolder {
        return LanguageViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.single_language, parent, false)
        )
    }

    override fun onBindViewHolder(holder: LanguageAdapter.LanguageViewHolder, position: Int) {
        val singleLanguage = language[position]
        holder.tvLanguage.text = singleLanguage.languageName
        holder.tvLanguageInEnglish.text = singleLanguage.languageNameInEnglish
        holder.clSingleLanguage.setOnClickListener {
            onItemClicked(singleLanguage.languageId, singleLanguage.comingSoon)
        }
    }

    override fun getItemCount() = language.size
}