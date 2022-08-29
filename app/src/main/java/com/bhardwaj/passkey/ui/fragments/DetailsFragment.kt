package com.bhardwaj.passkey.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bhardwaj.passkey.R
import com.bhardwaj.passkey.data.Categories
import com.bhardwaj.passkey.data.entity.Details
import com.bhardwaj.passkey.databinding.FragmentDetailsBinding
import com.bhardwaj.passkey.ui.adapter.DetailsAdapter
import com.bhardwaj.passkey.utils.Constants.Companion.CATEGORY_NAME
import com.bhardwaj.passkey.utils.Constants.Companion.HEADING_NAME
import com.bhardwaj.passkey.viewModels.MainViewModel
import com.ernestoyaquello.dragdropswiperecyclerview.DragDropSwipeRecyclerView
import com.ernestoyaquello.dragdropswiperecyclerview.listener.OnItemDragListener
import com.ernestoyaquello.dragdropswiperecyclerview.listener.OnItemSwipeListener
import com.ernestoyaquello.dragdropswiperecyclerview.listener.OnListScrollListener
import com.google.android.material.bottomsheet.BottomSheetDialog

class DetailsFragment : Fragment() {
    private var binding: FragmentDetailsBinding? = null
    private val mainViewModel: MainViewModel by activityViewModels()
    private var detailsList: ArrayList<Details> = arrayListOf()
    private lateinit var detailsAdapter: DetailsAdapter
    private lateinit var headingName: String
    private lateinit var categoryName: Categories

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDetailsBinding.inflate(layoutInflater)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        headingName = arguments?.getString(HEADING_NAME).toString()
        categoryName = arguments?.getSerializable(CATEGORY_NAME) as Categories
        clickListeners()
        setUpRecyclerView()
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val bundle = bundleOf(CATEGORY_NAME to categoryName)
                    findNavController().navigate(R.id.detailsFragment_to_homeFragment, bundle)
                }
            })
    }

    private fun clickListeners() {
        binding?.fabDetails?.setOnClickListener {
            showBottomSheetDialog()
        }
    }

    private fun setUpRecyclerView() {
        val onItemSwipeListener = object : OnItemSwipeListener<Details> {
            override fun onItemSwiped(
                position: Int,
                direction: OnItemSwipeListener.SwipeDirection,
                item: Details
            ): Boolean {
                mainViewModel.deleteDetails(
                    details = item,
                    headingName = item.headingName,
                    categoryName = categoryName,
                    initialPosition = position + 1,
                    finalPosition = detailsAdapter.itemCount + 1
                )
                return false
            }
        }
        val onItemDragListener = object : OnItemDragListener<Details> {
            override fun onItemDragged(previousPosition: Int, newPosition: Int, item: Details) {
            }

            override fun onItemDropped(initialPosition: Int, finalPosition: Int, item: Details) {
                if (initialPosition != finalPosition) {
                    persistChangesInOrdering(initialPosition, finalPosition, item)
                }
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

        detailsAdapter = DetailsAdapter(detailsList)

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

        observeAllDetails()
    }

    private fun persistChangesInOrdering(initialPosition: Int, finalPosition: Int, item: Details) {
        if (finalPosition > initialPosition) {
            mainViewModel.decrementPriority(
                isPreview = false,
                initialPosition = initialPosition + 1,
                finalPosition = finalPosition + 1,
                headingName = item.headingName,
                categoryName = item.categoryName
            )
        } else {
            mainViewModel.incrementPriority(
                isPreview = false,
                initialPosition = initialPosition + 1,
                finalPosition = finalPosition + 1,
                headingName = item.headingName,
                categoryName = item.categoryName
            )
        }
    }

    private fun observeAllDetails() {
        mainViewModel.allDetails.observe(viewLifecycleOwner) { details ->
            detailsList =
                (details).filter { s -> (s.headingName == headingName && s.categoryName == categoryName) } as ArrayList<Details>
            checkForNoResults(detailsList)
            detailsAdapter.dataSet = detailsList
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

    private fun showBottomSheetDialog() {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        bottomSheetDialog.setContentView(R.layout.bottom_sheet_details)

        val etAnswer = bottomSheetDialog.findViewById<EditText>(R.id.etAnswer)
        val etQuestion = bottomSheetDialog.findViewById<EditText>(R.id.etQuestion)

        bottomSheetDialog.findViewById<TextView>(R.id.tvSave)?.setOnClickListener {
            if (etQuestion?.text.toString().trim().isNotEmpty() and etAnswer?.text.toString().trim()
                    .isNotEmpty()
            ) {
                val detail =
                    Details(
                        detailsId = 0,
                        question = etQuestion?.text.toString().trim(),
                        answer = etAnswer?.text.toString().trim(),
                        priority = detailsList.size + 1,
                        categoryName = categoryName,
                        headingName = headingName
                    )
                mainViewModel.insertDetails(detail)
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.enter_valid_question_n_answer),
                    Toast.LENGTH_LONG
                ).show()
            }
            bottomSheetDialog.dismiss()
        }
        bottomSheetDialog.show()
    }
}