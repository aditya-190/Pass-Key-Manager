package com.bhardwaj.passkey.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.bhardwaj.passkey.R
import com.bhardwaj.passkey.data.entity.Details
import com.bhardwaj.passkey.databinding.FragmentDetailsBinding
import com.bhardwaj.passkey.ui.adapter.DetailsAdapter
import com.bhardwaj.passkey.viewModels.MainViewModel
import com.ernestoyaquello.dragdropswiperecyclerview.DragDropSwipeRecyclerView
import com.ernestoyaquello.dragdropswiperecyclerview.listener.OnItemDragListener
import com.ernestoyaquello.dragdropswiperecyclerview.listener.OnItemSwipeListener
import com.ernestoyaquello.dragdropswiperecyclerview.listener.OnListScrollListener
import com.google.android.material.bottomsheet.BottomSheetDialog

class DetailsFragment : Fragment() {
    private var binding: FragmentDetailsBinding? = null
    private val mainViewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDetailsBinding.inflate(layoutInflater)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpRecyclerView()
        clickListeners()
    }

    private fun clickListeners() {
        binding?.fabDetails?.setOnClickListener {
            showBottomSheetDialog()
        }
    }

    private fun showBottomSheetDialog() {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        bottomSheetDialog.setContentView(R.layout.bottom_sheet_details)

        val tvSave = bottomSheetDialog.findViewById<TextView>(R.id.tvSave)
        val etAnswer = bottomSheetDialog.findViewById<EditText>(R.id.etAnswer)
        val etQuestion = bottomSheetDialog.findViewById<EditText>(R.id.etQuestion)

        tvSave?.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }

    private fun setUpRecyclerView() {
        var detailsList = arrayListOf<Details>()

        val detailsAdapter = DetailsAdapter(
            detailsList,
            arguments?.getString("categoryName").toString()
        )

        val onItemSwipeListener = object : OnItemSwipeListener<Details> {
            override fun onItemSwiped(
                position: Int,
                direction: OnItemSwipeListener.SwipeDirection,
                item: Details
            ): Boolean {
                return false
            }
        }
        val onItemDragListener = object : OnItemDragListener<Details> {
            override fun onItemDragged(previousPosition: Int, newPosition: Int, item: Details) {
            }

            override fun onItemDropped(initialPosition: Int, finalPosition: Int, item: Details) {
            }
        }
        val onListScrollListener = object : OnListScrollListener {
            override fun onListScrollStateChanged(scrollState: OnListScrollListener.ScrollState) {
            }

            override fun onListScrolled(
                scrollDirection: OnListScrollListener.ScrollDirection,
                distance: Int
            ) {
            }
        }

        binding?.rvDetails.also {
            it?.layoutManager = LinearLayoutManager(activity)
            it?.adapter = detailsAdapter
            it?.orientation =
                DragDropSwipeRecyclerView.ListOrientation.VERTICAL_LIST_WITH_VERTICAL_DRAGGING
            it?.swipeListener = onItemSwipeListener
            it?.dragListener = onItemDragListener
            it?.scrollListener = onListScrollListener
            it?.behindSwipedItemLayoutId = R.layout.custom_details_swiped
            it?.disableSwipeDirection(DragDropSwipeRecyclerView.ListOrientation.DirectionFlag.RIGHT)
        }

        mainViewModel.allDetails.observe(viewLifecycleOwner) { details ->
            detailsList = details as ArrayList<Details>
            checkForNoResults(detailsList)
            detailsAdapter.updateInList(details)
        }
    }

    private fun checkForNoResults(detailsList: ArrayList<Details>) {
        if (detailsList.isNotEmpty()) {
            binding?.rvDetails?.visibility = View.VISIBLE
            binding?.ivNoResults?.visibility = View.GONE
        } else {
            binding?.rvDetails?.visibility = View.GONE
            binding?.ivNoResults?.visibility = View.VISIBLE
        }
    }
}