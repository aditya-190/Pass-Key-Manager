package com.bhardwaj.passkey.ui.adapter

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import com.bhardwaj.passkey.R
import com.bhardwaj.passkey.data.entity.Preview
import com.ernestoyaquello.dragdropswiperecyclerview.DragDropSwipeAdapter

class PreviewAdapter(
    previews: ArrayList<Preview>,
    private val smallerText: Boolean,
    private val navController: NavController,
) : DragDropSwipeAdapter<Preview, PreviewAdapter.PreviewViewHolder>(previews) {

    class PreviewViewHolder(itemView: View) : DragDropSwipeAdapter.ViewHolder(itemView) {
        val clSingleItem: ConstraintLayout = itemView.findViewById(R.id.clSingleItem)
        val tvSingleText: TextView = itemView.findViewById(R.id.tvSingleText)
        val ivSingleButton: ImageView = itemView.findViewById(R.id.ivSingleButton)
        val ivSingleDrag: ImageView = itemView.findViewById(R.id.ivSingleDrag)
    }

    override fun getViewHolder(itemView: View) = PreviewViewHolder(itemView)

    override fun onBindViewHolder(item: Preview, viewHolder: PreviewViewHolder, position: Int) {
        if (smallerText) viewHolder.tvSingleText.textSize = 12F
        viewHolder.tvSingleText.text = item.heading
        val bundle = bundleOf("headingName" to item.heading, "categoryName" to item.categoryName)

        viewHolder.clSingleItem.setOnClickListener {
            navController.navigate(R.id.homeFragment_to_detailsFragment, bundle)
        }

        viewHolder.ivSingleButton.setOnClickListener {
            navController.navigate(R.id.homeFragment_to_detailsFragment, bundle)
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