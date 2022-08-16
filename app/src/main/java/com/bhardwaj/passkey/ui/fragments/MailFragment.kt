package com.bhardwaj.passkey.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bhardwaj.passkey.R
import com.bhardwaj.passkey.databinding.FragmentMailBinding
import com.bhardwaj.passkey.ui.adapter.PreviewAdapter
import com.ernestoyaquello.dragdropswiperecyclerview.DragDropSwipeRecyclerView
import com.ernestoyaquello.dragdropswiperecyclerview.listener.OnItemDragListener
import com.ernestoyaquello.dragdropswiperecyclerview.listener.OnItemSwipeListener
import com.ernestoyaquello.dragdropswiperecyclerview.listener.OnListScrollListener

class MailFragment : Fragment() {
    private var binding: FragmentMailBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMailBinding.inflate(layoutInflater)
        return binding?.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bottomNavigationControls()
        setUpRecyclerView()
    }

    private fun setUpRecyclerView() {
        val previewsList = arrayListOf("aadi.bbhardwaj@gmail.com", "yrkkh.cclub@gmail.com", "aditirdr05@gmail.com", "ab2225@srmist.edu.in", "arvind.bhardwaj@timetechnoplast.com")
        val previewAdapter = PreviewAdapter(previewsList, true)

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

        binding?.rvMails.also {
            it?.layoutManager = LinearLayoutManager(activity)
            it?.adapter = previewAdapter
            it?.orientation =
                DragDropSwipeRecyclerView.ListOrientation.VERTICAL_LIST_WITH_VERTICAL_DRAGGING
            it?.swipeListener = onItemSwipeListener
            it?.dragListener = onItemDragListener
            it?.scrollListener = onListScrollListener
            it?.behindSwipedItemLayoutId = R.layout.custom_swiped
            it?.disableSwipeDirection(DragDropSwipeRecyclerView.ListOrientation.DirectionFlag.RIGHT)
        }
    }

    private fun checkForNoResults(previewsList: ArrayList<String>) {
        if (previewsList.isNotEmpty()) {
            binding?.rvMails?.visibility = View.VISIBLE
            binding?.ivNoResults?.visibility = View.GONE
        } else {
            binding?.rvMails?.visibility = View.GONE
            binding?.ivNoResults?.visibility = View.VISIBLE
        }
    }

    private fun bottomNavigationControls() {
        binding?.ivBank?.setOnClickListener {
            findNavController().navigate(R.id.mailFragment_to_bankFragment)
        }

        binding?.ivApps?.setOnClickListener {
            findNavController().navigate(R.id.mailFragment_to_appsFragment)
        }

        binding?.ivOthers?.setOnClickListener {
            findNavController().navigate(R.id.mailFragment_to_othersFragment)
        }
    }
}