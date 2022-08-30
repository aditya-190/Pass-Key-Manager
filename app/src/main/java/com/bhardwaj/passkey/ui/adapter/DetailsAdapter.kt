package com.bhardwaj.passkey.ui.adapter

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.bhardwaj.passkey.R
import com.bhardwaj.passkey.data.entity.Details
import com.ernestoyaquello.dragdropswiperecyclerview.DragDropSwipeAdapter

class DetailsAdapter(
    details: ArrayList<Details>,
    private var onEditClicked: ((item: Details) -> Unit),
    private var onItemLongPressed: ((answer: String) -> Unit)
) : DragDropSwipeAdapter<Details, DetailsAdapter.PreviewViewHolder>(details) {

    class PreviewViewHolder(itemView: View) : DragDropSwipeAdapter.ViewHolder(itemView) {
        val clSingleItem: ConstraintLayout = itemView.findViewById(R.id.clSingleItem)
        val ivSingleDrag: ImageView = itemView.findViewById(R.id.ivSingleDrag)
        val ivEdit: ImageView = itemView.findViewById(R.id.ivEdit)
        val tvAnswer: TextView = itemView.findViewById(R.id.tvAnswer)
        val tvQuestion: TextView = itemView.findViewById(R.id.tvQuestion)
    }

    override fun getViewHolder(itemView: View) = PreviewViewHolder(itemView)

    override fun onBindViewHolder(item: Details, viewHolder: PreviewViewHolder, position: Int) {
        viewHolder.tvQuestion.text = item.question
        viewHolder.tvAnswer.text = item.answer
        viewHolder.ivEdit.setOnClickListener { onEditClicked(item) }
        viewHolder.clSingleItem.setOnLongClickListener {
            onItemLongPressed(item.answer)
            return@setOnLongClickListener true
        }
    }

    override fun getViewToTouchToStartDraggingItem(
        item: Details,
        viewHolder: PreviewViewHolder,
        position: Int
    ): View {
        return viewHolder.ivSingleDrag
    }
}