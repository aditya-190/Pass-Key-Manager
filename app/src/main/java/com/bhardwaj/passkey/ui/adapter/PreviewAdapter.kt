package com.bhardwaj.passkey.ui.adapter

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.navigation.NavController
import com.bhardwaj.passkey.R
import com.ernestoyaquello.dragdropswiperecyclerview.DragDropSwipeAdapter

class PreviewAdapter(
    previews: ArrayList<String>,
    private val smallerText: Boolean,
    private val navController: NavController,
    private val fragmentName: String
) : DragDropSwipeAdapter<String, PreviewAdapter.PreviewViewHolder>(previews) {

    class PreviewViewHolder(itemView: View) : DragDropSwipeAdapter.ViewHolder(itemView) {
        val clSingleItem: ConstraintLayout = itemView.findViewById(R.id.clSingleItem)
        val tvSingleText: TextView = itemView.findViewById(R.id.tvSingleText)
        val ivSingleButton: ImageView = itemView.findViewById(R.id.ivSingleButton)
        val ivSingleDrag: ImageView = itemView.findViewById(R.id.ivSingleDrag)
    }

    override fun getViewHolder(itemView: View) = PreviewViewHolder(itemView)

    override fun onBindViewHolder(item: String, viewHolder: PreviewViewHolder, position: Int) {
        if (smallerText) viewHolder.tvSingleText.textSize = 12F
        viewHolder.tvSingleText.text = item

        viewHolder.clSingleItem.setOnClickListener {
            when (fragmentName) {
                "banks" -> navController.navigate(R.id.bankFragment_to_detailsFragment)
                "mails" -> navController.navigate(R.id.mailFragment_to_detailsFragment)
                "apps" -> navController.navigate(R.id.appsFragment_to_detailsFragment)
                "others" -> navController.navigate(R.id.othersFragment_to_detailsFragment)
            }
        }

        viewHolder.ivSingleButton.setOnClickListener {
            when (fragmentName) {
                "banks" -> navController.navigate(R.id.bankFragment_to_detailsFragment)
                "mails" -> navController.navigate(R.id.mailFragment_to_detailsFragment)
                "apps" -> navController.navigate(R.id.appsFragment_to_detailsFragment)
                "others" -> navController.navigate(R.id.othersFragment_to_detailsFragment)
            }
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