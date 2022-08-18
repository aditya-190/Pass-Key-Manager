package com.bhardwaj.passkey.ui.adapter

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.bhardwaj.passkey.R
import com.bhardwaj.passkey.data.entity.Details
import com.ernestoyaquello.dragdropswiperecyclerview.DragDropSwipeAdapter

class DetailsAdapter(
    details: ArrayList<Details>,
    private val categoryName: String
) : DragDropSwipeAdapter<Details, DetailsAdapter.PreviewViewHolder>(details) {

    class PreviewViewHolder(itemView: View) : DragDropSwipeAdapter.ViewHolder(itemView) {
        val ivSingleDrag: ImageView = itemView.findViewById(R.id.ivSingleDrag)
        val tvAnswer: TextView = itemView.findViewById(R.id.tvAnswer)
        val tvQuestion: TextView = itemView.findViewById(R.id.tvQuestion)
    }

    override fun getViewHolder(itemView: View) = PreviewViewHolder(itemView)

    override fun onBindViewHolder(item: Details, viewHolder: PreviewViewHolder, position: Int) {
        viewHolder.tvQuestion.text = item.question
        viewHolder.tvAnswer.text = item.answer
    }

    override fun getViewToTouchToStartDraggingItem(
        item: Details,
        viewHolder: PreviewViewHolder,
        position: Int
    ): View {
        return viewHolder.ivSingleDrag
    }
}