package com.bhardwaj.passkey.ui.adapter

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.bhardwaj.passkey.R
import com.bhardwaj.passkey.data.Categories
import com.bhardwaj.passkey.data.entity.Preview
import com.ernestoyaquello.dragdropswiperecyclerview.DragDropSwipeAdapter

class PreviewAdapter(
    previews: ArrayList<Preview>,
    private var onItemClicked: ((heading: String, category: Categories) -> Unit),
    private var onEditClicked: ((item: Preview) -> Unit),
    private var onItemLongPressed: ((headingName: String) -> Unit)
) : DragDropSwipeAdapter<Preview, PreviewAdapter.PreviewViewHolder>(previews) {

    class PreviewViewHolder(itemView: View) : ViewHolder(itemView) {
        val clSingleItem: ConstraintLayout = itemView.findViewById(R.id.clSingleItem)
        val tvSingleText: TextView = itemView.findViewById(R.id.tvSingleText)
        val ivEdit: ImageView = itemView.findViewById(R.id.ivEdit)
        val ivSingleDrag: ImageView = itemView.findViewById(R.id.ivSingleDrag)
    }

    override fun getViewHolder(itemView: View) = PreviewViewHolder(itemView)

    override fun onBindViewHolder(item: Preview, viewHolder: PreviewViewHolder, position: Int) {
        viewHolder.tvSingleText.text = item.heading

        viewHolder.clSingleItem.setOnClickListener {
            onItemClicked(item.heading, item.categoryName)
        }

        viewHolder.ivEdit.setOnClickListener { onEditClicked(item) }

        viewHolder.clSingleItem.setOnLongClickListener {
            onItemLongPressed(item.heading)
            return@setOnLongClickListener true
        }
    }

    override fun getViewToTouchToStartDraggingItem(
        item: Preview,
        viewHolder: PreviewViewHolder,
        position: Int
    ): View {
        return viewHolder.ivSingleDrag
    }
}