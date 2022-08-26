package com.bhardwaj.passkey.ui.fragments

import android.content.Intent
import android.os.*
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.*
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bhardwaj.passkey.R
import com.bhardwaj.passkey.data.Categories
import com.bhardwaj.passkey.data.entity.Preview
import com.bhardwaj.passkey.databinding.FragmentHomeBinding
import com.bhardwaj.passkey.ui.adapter.PreviewAdapter
import com.bhardwaj.passkey.viewModels.MainViewModel
import com.ernestoyaquello.dragdropswiperecyclerview.DragDropSwipeRecyclerView
import com.ernestoyaquello.dragdropswiperecyclerview.listener.OnItemDragListener
import com.ernestoyaquello.dragdropswiperecyclerview.listener.OnItemSwipeListener
import com.ernestoyaquello.dragdropswiperecyclerview.listener.OnListScrollListener
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.io.File
import java.util.concurrent.Executor

class HomeFragment : Fragment() {
    private var binding: FragmentHomeBinding? = null
    private val mainViewModel: MainViewModel by activityViewModels()
    private var previewsList: ArrayList<Preview> = arrayListOf()
    private lateinit var previewAdapter: PreviewAdapter
    private lateinit var categoryName: Categories
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private val biometricManager by lazy {
        BiometricManager.from(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(layoutInflater)
        return binding?.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        executor = ContextCompat.getMainExecutor(requireContext())
        biometricPrompt = BiometricPrompt(
            this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(
                    errorCode: Int,
                    errorString: CharSequence
                ) {
                    super.onAuthenticationError(errorCode, errorString)
                    val toast = Toast.makeText(
                        requireContext(),
                        getString(R.string.something_went_wrong),
                        Toast.LENGTH_LONG
                    )
                    toast.show()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        toast.addCallback(object : Toast.Callback() {
                            override fun onToastHidden() {
                                super.onToastHidden()
                                requireActivity().finish()
                            }
                        })
                    } else {
                        delayTillToastShown()
                    }
                }

                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    super.onAuthenticationSucceeded(result)
                    binding?.root?.visibility = View.VISIBLE
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    val toast = Toast.makeText(
                        requireContext(),
                        getString(R.string.authentication_failed),
                        Toast.LENGTH_LONG
                    )
                    toast.show()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        toast.addCallback(object : Toast.Callback() {
                            override fun onToastHidden() {
                                super.onToastHidden()
                                requireActivity().finish()
                            }
                        })
                    } else {
                        delayTillToastShown()
                    }
                }
            }
        )

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.app_name))
            .setSubtitle(getString(R.string.welcome_to_pass_key))
            .setAllowedAuthenticators(DEVICE_CREDENTIAL or BIOMETRIC_WEAK or BIOMETRIC_STRONG)
            .build()

        when (biometricManager.canAuthenticate(DEVICE_CREDENTIAL or BIOMETRIC_WEAK or BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                biometricPrompt.authenticate(promptInfo)
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE,
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val getResult =
                        registerForActivityResult(
                            ActivityResultContracts.StartActivityForResult()
                        ) {}

                    val intent = Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                        putExtra(
                            Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                            BIOMETRIC_STRONG or DEVICE_CREDENTIAL
                        )
                    }
                    getResult.launch(intent)
                } else {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.set_password),
                        Toast.LENGTH_LONG
                    ).show()

                    delayTillToastShown()
                }
            }
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> {}
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> {}
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> {}
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        categoryName = arguments?.getSerializable("categoryName") as Categories
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
            showBottomSheetDialog()
        }
        binding?.ivExport?.setOnClickListener {
            exportDatabase()
        }
    }

    private fun exportDatabase() {
        try {
            val file = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "pass_key_manager.csv"
            )
            if (file.exists()) file.delete()
            file.createNewFile()

            val header = "question,answer,heading,category\n"
            file.appendText(header)

            mainViewModel.allDetailsForExport.observe(viewLifecycleOwner) { details ->
                details.forEach { single ->
                    file.appendText("${single.question},${single.answer},${single.headingName},${single.categoryName}\n")
                }
            }
            Toast.makeText(context, getString(R.string.export_download), Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setUpRecyclerView() {
        val onItemSwipeListener = object : OnItemSwipeListener<Preview> {
            override fun onItemSwiped(
                position: Int,
                direction: OnItemSwipeListener.SwipeDirection,
                item: Preview
            ): Boolean {
                mainViewModel.deletePreview(item)
                mainViewModel.deleteDetailWithConditions(item.heading, categoryName = categoryName)
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

        previewAdapter =
            PreviewAdapter(previewsList, categoryName == Categories.MAILS, findNavController())

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

    private fun showBottomSheetDialog() {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        bottomSheetDialog.setContentView(R.layout.bottom_sheet_preview)

        val etHeading =
            bottomSheetDialog.findViewById<EditText>(R.id.etHeading)

        bottomSheetDialog.findViewById<TextView>(R.id.tvSave)?.setOnClickListener {
            if (etHeading?.text.toString().trim().isNotEmpty()) {
                val preview = Preview(
                    previewId = 0,
                    priority = previewsList.size + 1,
                    heading = etHeading?.text.toString().trim(),
                    categoryName = categoryName
                )
                mainViewModel.insertPreview(preview)
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.enter_valid_heading),
                    Toast.LENGTH_LONG
                )
                    .show()
            }
            bottomSheetDialog.dismiss()
        }
        bottomSheetDialog.show()
    }

    private fun delayTillToastShown() {
        Handler(
            Looper.myLooper()!!
        ).postDelayed({ requireActivity().finish() }, 3500L)
    }
}