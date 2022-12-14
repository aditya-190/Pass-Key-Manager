package com.bhardwaj.passkey.ui.fragments

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context.CLIPBOARD_SERVICE
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bhardwaj.passkey.R
import com.bhardwaj.passkey.data.Categories
import com.bhardwaj.passkey.data.entity.Preview
import com.bhardwaj.passkey.databinding.FragmentHomeBinding
import com.bhardwaj.passkey.ui.adapter.PreviewAdapter
import com.bhardwaj.passkey.utils.Constants.Companion.CATEGORY_NAME
import com.bhardwaj.passkey.utils.Constants.Companion.HEADING_NAME
import com.bhardwaj.passkey.viewModels.MainViewModel
import com.ernestoyaquello.dragdropswiperecyclerview.DragDropSwipeRecyclerView
import com.ernestoyaquello.dragdropswiperecyclerview.listener.OnItemDragListener
import com.ernestoyaquello.dragdropswiperecyclerview.listener.OnItemSwipeListener
import com.ernestoyaquello.dragdropswiperecyclerview.listener.OnListScrollListener
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar


class HomeFragment : Fragment() {
    private var binding: FragmentHomeBinding? = null
    private val mainViewModel: MainViewModel by activityViewModels()
    private var previewsList: ArrayList<Preview> = arrayListOf()
    private lateinit var previewAdapter: PreviewAdapter
    private lateinit var categoryName: Categories
    private var readPermissionGranted = false
    private var writePermissionGranted = false
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                readPermissionGranted =
                    permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: readPermissionGranted
                writePermissionGranted = permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE]
                    ?: writePermissionGranted

                if (!readPermissionGranted || !writePermissionGranted) {
                    Snackbar.make(
                        requireView(),
                        getString(R.string.permission_denied),
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(layoutInflater)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        categoryName = arguments?.getSerializable(CATEGORY_NAME) as Categories
        changeHeading(categoryName)
        clickListeners()
        setUpRecyclerView()
    }

    private fun clickListeners() {
        binding?.ivBank?.setOnClickListener {
            changeHeading(changeTo = Categories.BANKS)
        }
        binding?.ivEmail?.setOnClickListener {
            changeHeading(changeTo = Categories.MAILS)
        }
        binding?.ivApps?.setOnClickListener {
            changeHeading(changeTo = Categories.APPS)
        }
        binding?.ivOthers?.setOnClickListener {
            changeHeading(changeTo = Categories.OTHERS)
        }
        binding?.fabAdd?.setOnClickListener {
            showBottomSheetDialog(editMode = false, previews = null)
        }
        binding?.ivImportExport?.setOnClickListener {
            val popupMenu = PopupMenu(requireContext(), binding?.ivImportExport!!)
            popupMenu.menuInflater.inflate(R.menu.popup_menu, popupMenu.menu)
            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_import -> if (updateOrRequestPermission()) mainViewModel.importData()
                    R.id.action_export -> if (updateOrRequestPermission()) mainViewModel.exportData()
                }
                true
            }
            popupMenu.show()
        }
    }

    private fun updateOrRequestPermission(): Boolean {
        val hasReadPermission = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

        val hasWritePermission = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

        val minSdk29 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

        readPermissionGranted = hasReadPermission
        writePermissionGranted = hasWritePermission || minSdk29

        val permissionsToRequest = mutableListOf<String>()

        if (!readPermissionGranted) {
            permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        if (!writePermissionGranted) {
            permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        return when {
            permissionsToRequest.isEmpty() -> true

            shouldShowRequestPermissionRationale(permissionsToRequest[0]) -> {
                val snackBar = Snackbar.make(
                    requireView(),
                    getString(R.string.permission_required),
                    Snackbar.LENGTH_LONG
                )
                snackBar.show()
                snackBar.setAction(getString(R.string.ok)) {
                    permissionLauncher.launch(permissionsToRequest.toTypedArray())
                }
                false
            }
            else -> {
                permissionLauncher.launch(permissionsToRequest.toTypedArray())
                false
            }
        }
    }

    private fun setUpRecyclerView() {
        val onItemSwipeListener = object : OnItemSwipeListener<Preview> {
            override fun onItemSwiped(
                position: Int,
                direction: OnItemSwipeListener.SwipeDirection,
                item: Preview
            ): Boolean {
//                var clickedUndo = false
//
//                val snackBar = Snackbar.make(binding?.root!!, "Deleted.", Snackbar.LENGTH_LONG)
//                snackBar.also {
//                    it.setAction("Undo") {
//                        previewAdapter.insertItem(position, item)
//                        clickedUndo = true
//                    }
//                        .addCallback(object :
//                            BaseTransientBottomBar.BaseCallback<Snackbar>() {
//                            override fun onDismissed(
//                                transientBottomBar: Snackbar?,
//                                event: Int
//                            ) {
//                                if (!clickedUndo) {
//                                    mainViewModel.deletePreview(
//                                        preview = item,
//                                        heading = item.heading,
//                                        categoryName = categoryName,
//                                        initialPosition = position + 1,
//                                        finalPosition = previewAdapter.itemCount + 1
//                                    )
//                                }
//                                super.onDismissed(transientBottomBar, event)
//                            }
//                        })
//                    it.anchorView = binding?.fabAdd
//                    it.show()
//                }

                val builder = AlertDialog.Builder(requireContext())

                val positiveButtonClick = { _: DialogInterface, _: Int ->
                    mainViewModel.deletePreview(
                        preview = item,
                        heading = item.heading,
                        categoryName = categoryName,
                        initialPosition = position + 1,
                        finalPosition = previewAdapter.itemCount + 1
                    )
                }

                val negativeButtonClick = { _: DialogInterface, _: Int ->
                    previewAdapter.insertItem(position, item)
                }

                with(builder)
                {
                    setTitle("Confirm delete")
                    setMessage("Are you sure you want to delete heading - ${item.heading}?")
                    setPositiveButton("Delete", DialogInterface.OnClickListener(function = positiveButtonClick))
                    setNegativeButton("Cancel", negativeButtonClick)
                    setCancelable(false)
                    show()
                }
                return false
            }
        }
        val onItemDragListener = object : OnItemDragListener<Preview> {
            override fun onItemDragged(previousPosition: Int, newPosition: Int, item: Preview) {
            }

            override fun onItemDropped(
                initialPosition: Int,
                finalPosition: Int,
                item: Preview
            ) {
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

        previewAdapter =
            PreviewAdapter(previewsList, { heading, category ->
                val bundle = bundleOf(HEADING_NAME to heading, CATEGORY_NAME to category)
                findNavController().navigate(R.id.homeFragment_to_detailsFragment, bundle)
            }, { preview ->
                showBottomSheetDialog(editMode = true, previews = preview)
            }, { heading ->
                copyToClipBoard(heading)
            })

        binding?.rvAll.also {
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

        observeAllPreviews()
    }

    private fun copyToClipBoard(heading: String) {
        val clipboardManager =
            requireActivity().getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.setPrimaryClip(ClipData.newPlainText("Heading", heading))
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2)
            Toast.makeText(requireContext(), "Copied", Toast.LENGTH_SHORT).show()
    }

    private fun persistChangesInOrdering(
        initialPosition: Int,
        finalPosition: Int,
        item: Preview
    ) {
        if (finalPosition > initialPosition) {
            mainViewModel.decrementPriority(
                isPreview = true,
                initialPosition = initialPosition + 1,
                finalPosition = finalPosition + 1,
                headingName = item.heading,
                categoryName = item.categoryName,
                question = "QUESTION",
                answer = "ANSWER"
            )
        } else {
            mainViewModel.incrementPriority(
                isPreview = true,
                initialPosition = initialPosition + 1,
                finalPosition = finalPosition + 1,
                headingName = item.heading,
                categoryName = item.categoryName,
                question = "QUESTION",
                answer = "ANSWER"
            )
        }
    }

    private fun observeAllPreviews() {
        mainViewModel.allPreviews.observe(viewLifecycleOwner) { previews ->
            previewsList =
                (previews).filter { s -> s.categoryName == categoryName } as ArrayList<Preview>
            checkForNoResults(previewsList)
            previewAdapter.dataSet = previewsList
        }
    }

    private fun checkForNoResults(previewsList: ArrayList<Preview>) {
        if (previewsList.isNotEmpty()) {
            binding?.rvAll?.visibility = View.VISIBLE
            binding?.ivNoResults?.visibility = View.GONE
        } else {
            binding?.rvAll?.visibility = View.GONE
            binding?.ivNoResults?.visibility = View.VISIBLE
        }
    }

    private fun changeHeading(changeTo: Categories) {
        categoryName = changeTo
        observeAllPreviews()

        when (changeTo) {
            Categories.BANKS -> {
                binding?.ivBank?.setColorFilter(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.primary
                    )
                )
                binding?.ivEmail?.setColorFilter(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.secondary
                    )
                )
                binding?.ivApps?.setColorFilter(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.secondary
                    )
                )
                binding?.ivOthers?.setColorFilter(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.secondary
                    )
                )
                binding?.tvHeading?.text = resources.getString(R.string.banks)
            }
            Categories.MAILS -> {
                binding?.ivBank?.setColorFilter(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.secondary
                    )
                )
                binding?.ivEmail?.setColorFilter(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.primary
                    )
                )
                binding?.ivApps?.setColorFilter(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.secondary
                    )
                )
                binding?.ivOthers?.setColorFilter(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.secondary
                    )
                )
                binding?.tvHeading?.text = resources.getString(R.string.mails)
            }
            Categories.APPS -> {
                binding?.ivBank?.setColorFilter(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.secondary
                    )
                )
                binding?.ivEmail?.setColorFilter(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.secondary
                    )
                )
                binding?.ivApps?.setColorFilter(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.primary
                    )
                )
                binding?.ivOthers?.setColorFilter(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.secondary
                    )
                )
                binding?.tvHeading?.text = resources.getString(R.string.apps)
            }
            Categories.OTHERS -> {
                binding?.ivBank?.setColorFilter(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.secondary
                    )
                )
                binding?.ivEmail?.setColorFilter(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.secondary
                    )
                )
                binding?.ivApps?.setColorFilter(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.secondary
                    )
                )
                binding?.ivOthers?.setColorFilter(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.primary
                    )
                )
                binding?.tvHeading?.text = resources.getString(R.string.others)
            }
        }
    }

    private fun showBottomSheetDialog(editMode: Boolean, previews: Preview?) {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        bottomSheetDialog.setContentView(R.layout.bottom_sheet_preview)

        val etHeading =
            bottomSheetDialog.findViewById<EditText>(R.id.etHeading)

        if (editMode) {
            etHeading?.setText(previews?.heading)
        }

        bottomSheetDialog.findViewById<TextView>(R.id.tvSave)?.setOnClickListener {
            val changedHeading = etHeading?.text.toString().trim()

            if (editMode) {
                if (changedHeading.isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.enter_valid_heading),
                        Toast.LENGTH_LONG
                    ).show()
                } else if ((changedHeading != previews?.heading)) {
                    val preview =
                        previews?.copy(heading = changedHeading)
                    if (preview != null) {
                        mainViewModel.updatePreview(preview)
                    }
                }
            } else {
                if (changedHeading.isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.enter_valid_heading),
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    val preview =
                        Preview(
                            previewId = 0, heading = changedHeading, categoryName = categoryName, priority = previewsList.size + 1

                        )
                    mainViewModel.insertPreview(preview)
                }
            }
            bottomSheetDialog.dismiss()
        }
        bottomSheetDialog.show()
    }
}