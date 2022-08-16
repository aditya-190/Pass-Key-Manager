package com.bhardwaj.passkey.ui.adapter

import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.bhardwaj.passkey.R
import com.ernestoyaquello.dragdropswiperecyclerview.DragDropSwipeAdapter

class PreviewAdapter(
    previews: ArrayList<String>,
    private val smallerText: Boolean
) : DragDropSwipeAdapter<String, PreviewAdapter.PreviewViewHolder>(previews) {

    class PreviewViewHolder(itemView: View) : DragDropSwipeAdapter.ViewHolder(itemView) {
        val clSingleItem: ConstraintLayout = itemView.findViewById(R.id.clSingleItem)
        val tvSingleText: TextView = itemView.findViewById(R.id.tvSingleText)
        val ivSingleButton: ImageView = itemView.findViewById(R.id.ivSingleButton)
        val ivSingleDrag: ImageView = itemView.findViewById(R.id.ivSingleDrag)
    }

    override fun getViewHolder(itemView: View) = PreviewViewHolder(itemView)

    override fun onBindViewHolder(item: String, viewHolder: PreviewViewHolder, position: Int) {
        if (smallerText) viewHolder.tvSingleText.textSize = 14F
        viewHolder.tvSingleText.text = item

        viewHolder.clSingleItem.setOnClickListener {
            Log.d("ADITYA", "Full Button Clicked -> $item")
        }

        viewHolder.ivSingleButton.setOnClickListener {
            Log.d("ADITYA", "Next Button Clicked -> $item")
        }
    }

    override fun getViewToTouchToStartDraggingItem(
        item: String,
        viewHolder: PreviewViewHolder,
        position: Int
    ): View {
        return viewHolder.ivSingleDrag
    }
}