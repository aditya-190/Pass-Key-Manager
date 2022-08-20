package com.bhardwaj.passkey.ui.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bhardwaj.passkey.R
import com.bhardwaj.passkey.data.entity.Preview
import com.bhardwaj.passkey.databinding.FragmentOthersBinding
import com.bhardwaj.passkey.ui.adapter.PreviewAdapter
import com.bhardwaj.passkey.viewModels.MainViewModel
import com.ernestoyaquello.dragdropswiperecyclerview.DragDropSwipeRecyclerView
import com.ernestoyaquello.dragdropswiperecyclerview.listener.OnItemDragListener
import com.ernestoyaquello.dragdropswiperecyclerview.listener.OnItemSwipeListener
import com.ernestoyaquello.dragdropswiperecyclerview.listener.OnListScrollListener
import com.google.android.material.bottomsheet.BottomSheetDialog

class OthersFragment : Fragment() {
    private var binding: FragmentOthersBinding? = null
    private val mainViewModel: MainViewModel by activityViewModels()
    private var previewsList: ArrayList<Preview> = arrayListOf()
    private lateinit var previewAdapter: PreviewAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentOthersBinding.inflate(layoutInflater)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bottomNavigationControls()
        setUpRecyclerView()
        clickListeners()
    }

    private fun clickListeners() {
        binding?.fabOthers?.setOnClickListener {
            showBottomSheetDialog()
        }
    }

    private fun showBottomSheetDialog() {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        bottomSheetDialog.setContentView(R.layout.bottom_sheet_preview)

        val tvSave = bottomSheetDialog.findViewById<TextView>(R.id.tvSave)
        val etHeading = bottomSheetDialog.findViewById<EditText>(R.id.etHeading)

        tvSave?.setOnClickListener {
            if (etHeading?.text.toString().trim().isNotEmpty()) {
                val preview = Preview(
                    previewId = 0,
                    priority = previewsList.size + 1,
                    heading = etHeading?.text.toString().trim(),
                    categoryName = "others"
                )

                mainViewModel.insertPreview(preview)

            } else {
                Toast.makeText(requireContext(), "Enter a Valid Heading.", Toast.LENGTH_SHORT)
                    .show()
            }
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }

    private fun setUpRecyclerView() {
        val onItemSwipeListener = object : OnItemSwipeListener<Preview> {
            override fun onItemSwiped(
                position: Int,
                direction: OnItemSwipeListener.SwipeDirection,
                item: Preview
            ): Boolean {
                mainViewModel.deletePreview(item)
                return false
            }
        }
        val onItemDragListener = object : OnItemDragListener<Preview> {
            override fun onItemDragged(previousPosition: Int, newPosition: Int, item: Preview) {
            }

            override fun onItemDropped(initialPosition: Int, finalPosition: Int, item: Preview) {
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

        previewAdapter = PreviewAdapter(previewsList, false, findNavController())

        binding?.rvOthers.also {
            it?.layoutManager = LinearLayoutManager(activity)
            it?.adapter = previewAdapter
            it?.orientation =
                DragDropSwipeRecyclerView.ListOrientation.VERTICAL_LIST_WITH_VERTICAL_DRAGGING
            it?.swipeListener = onItemSwipeListener
            it?.dragListener = onItemDragListener
            it?.scrollListener = onListScrollListener
            it?.behindSwipedItemLayoutId = R.layout.custom_preview_swiped
            it?.disableSwipeDirection(DragDropSwipeRecyclerView.ListOrientation.DirectionFlag.RIGHT)
        }

        mainViewModel.allPreviews.observe(viewLifecycleOwner) { previews ->
            previewsList =
                (previews).filter { s -> s.categoryName == "others" } as ArrayList<Preview>
            checkForNoResults(previewsList)
            previewAdapter.dataSet = previewsList
        }
    }

    private fun checkForNoResults(previewsList: ArrayList<Preview>) {
        if (previewsList.isNotEmpty()) {
            binding?.rvOthers?.visibility = View.VISIBLE
            binding?.ivNoResults?.visibility = View.GONE
        } else {
            binding?.rvOthers?.visibility = View.GONE
            binding?.ivNoResults?.visibility = View.VISIBLE
        }
    }

    private fun bottomNavigationControls() {
        binding?.ivBank?.setOnClickListener {
            findNavController().navigate(R.id.othersFragment_to_bankFragment)
        }

        binding?.ivEmail?.setOnClickListener {
            findNavController().navigate(R.id.othersFragment_to_mailFragment)
        }

        binding?.ivApps?.setOnClickListener {
            findNavController().navigate(R.id.othersFragment_to_appsFragment)
        }
    }
}