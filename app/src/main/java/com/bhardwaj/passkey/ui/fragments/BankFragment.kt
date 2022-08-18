package com.bhardwaj.passkey.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bhardwaj.passkey.R
import com.bhardwaj.passkey.data.entity.Preview
import com.bhardwaj.passkey.databinding.FragmentBankBinding
import com.bhardwaj.passkey.ui.adapter.PreviewAdapter
import com.bhardwaj.passkey.viewModels.MainViewModel
import com.ernestoyaquello.dragdropswiperecyclerview.DragDropSwipeRecyclerView
import com.ernestoyaquello.dragdropswiperecyclerview.listener.OnItemDragListener
import com.ernestoyaquello.dragdropswiperecyclerview.listener.OnItemSwipeListener
import com.ernestoyaquello.dragdropswiperecyclerview.listener.OnListScrollListener
import com.google.android.material.bottomsheet.BottomSheetDialog

class BankFragment : Fragment() {
    private var binding: FragmentBankBinding? = null
    private val mainViewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentBankBinding.inflate(layoutInflater)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bottomNavigationControls()
        setUpRecyclerView()
        clickListeners()
    }

    private fun clickListeners() {
        binding?.fabBanks?.setOnClickListener {
            showBottomSheetDialog()
        }
    }

    private fun showBottomSheetDialog() {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        bottomSheetDialog.setContentView(R.layout.bottom_sheet_preview)

        val tvSave = bottomSheetDialog.findViewById<TextView>(R.id.tvSave)
        val etHeading = bottomSheetDialog.findViewById<EditText>(R.id.etHeading)

        tvSave?.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }

    private fun setUpRecyclerView() {
        var previewsList = arrayListOf<Preview>()

        val previewAdapter = PreviewAdapter(previewsList, false, findNavController())

        val onItemSwipeListener = object : OnItemSwipeListener<Preview> {
            override fun onItemSwiped(
                position: Int,
                direction: OnItemSwipeListener.SwipeDirection,
                item: Preview
            ): Boolean {
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

        binding?.rvBanks.also {
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
            previewsList = previews as ArrayList<Preview>
            checkForNoResults(previewsList)
            previewAdapter.updateInList(previews)
        }
    }

    private fun checkForNoResults(previewsList: ArrayList<Preview>) {
        if (previewsList.isNotEmpty()) {
            binding?.rvBanks?.visibility = View.VISIBLE
            binding?.ivNoResults?.visibility = View.GONE
        } else {
            binding?.rvBanks?.visibility = View.GONE
            binding?.ivNoResults?.visibility = View.VISIBLE
        }
    }

    private fun bottomNavigationControls() {
        binding?.ivEmail?.setOnClickListener {
            findNavController().navigate(R.id.bankFragment_to_mailFragment)
        }

        binding?.ivApps?.setOnClickListener {
            findNavController().navigate(R.id.bankFragment_to_appsFragment)
        }

        binding?.ivOthers?.setOnClickListener {
            findNavController().navigate(R.id.bankFragment_to_othersFragment)
        }
    }
}