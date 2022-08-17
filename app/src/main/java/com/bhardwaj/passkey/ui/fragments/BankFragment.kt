package com.bhardwaj.passkey.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bhardwaj.passkey.R
import com.bhardwaj.passkey.databinding.FragmentBankBinding
import com.bhardwaj.passkey.ui.adapter.PreviewAdapter
import com.ernestoyaquello.dragdropswiperecyclerview.DragDropSwipeRecyclerView
import com.ernestoyaquello.dragdropswiperecyclerview.listener.OnItemDragListener
import com.ernestoyaquello.dragdropswiperecyclerview.listener.OnItemSwipeListener
import com.ernestoyaquello.dragdropswiperecyclerview.listener.OnListScrollListener

class BankFragment : Fragment() {
    private var binding: FragmentBankBinding? = null

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
    }

    private fun setUpRecyclerView() {
        val previewsList = arrayListOf("Axis", "BOB", "SBI", "PPF", "Others")
        val previewAdapter = PreviewAdapter(previewsList, false, findNavController(), "banks")

        val onItemSwipeListener = object : OnItemSwipeListener<String> {
            override fun onItemSwiped(
                position: Int,
                direction: OnItemSwipeListener.SwipeDirection,
                item: String
            ): Boolean {
                return false
            }
        }
        val onItemDragListener = object : OnItemDragListener<String> {
            override fun onItemDragged(previousPosition: Int, newPosition: Int, item: String) {
            }

            override fun onItemDropped(initialPosition: Int, finalPosition: Int, item: String) {
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

        checkForNoResults(previewsList)

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
    }

    private fun checkForNoResults(previewsList: ArrayList<String>) {
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