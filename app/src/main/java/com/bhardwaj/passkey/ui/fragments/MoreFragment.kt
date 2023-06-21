package com.bhardwaj.passkey.ui.fragments

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.bhardwaj.passkey.BuildConfig
import com.bhardwaj.passkey.R
import com.bhardwaj.passkey.databinding.FragmentMoreBinding
import com.bhardwaj.passkey.ui.activity.MainActivity
import com.bhardwaj.passkey.viewModels.MainViewModel
import com.google.android.material.snackbar.Snackbar

class MoreFragment : Fragment() {
    private var binding: FragmentMoreBinding? = null
    private val mainViewModel: MainViewModel by activityViewModels()
    private var readPermissionGranted = false
    private var writePermissionGranted = false
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var getFileLauncher: ActivityResultLauncher<Intent>

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

        getFileLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    result.data?.data?.let { fileUri -> mainViewModel.importData(fileUri) }
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMoreBinding.inflate(layoutInflater)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initialise()
        clickListeners()
    }

    private fun initialise() {
        binding?.tvAppVersion?.text = getString(R.string.app_version, BuildConfig.VERSION_NAME)
    }

    private fun clickListeners() {
        binding?.tvChangeLanguage?.setOnClickListener {
            LanguageFragment { languageId, comingSoon ->
                if (comingSoon) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.coming_soon),
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    (activity as MainActivity).changeLanguage(languageId)
                    (activity as MainActivity).recreate()
                }
            }.show(parentFragmentManager, "change language")
        }

        binding?.tvRateApp?.setOnClickListener { rateApp() }

        binding?.tvPrivacy?.setOnClickListener {
            showMessage(getString(R.string.privacy), getString(R.string.privacy_message))
        }

        binding?.tvTerms?.setOnClickListener {
            showMessage(
                getString(R.string.terms_n_condition),
                getString(R.string.terms_n_condition_message)
            )
        }

        binding?.tvImportData?.setOnClickListener {
            if (updateOrRequestPermission()) importData()
        }

        binding?.tvExportData?.setOnClickListener {
            if (updateOrRequestPermission()) mainViewModel.exportData()
        }

        binding?.tvAbout?.setOnClickListener {
            showMessage(getString(R.string.about), getString(R.string.about_message))
        }
    }

    private fun rateApp() {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(
                "https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}"
            )
        }
        requireContext().startActivity(intent)
    }

    private fun importData() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        getFileLauncher.launch(intent)
    }

    private fun showMessage(title: String, message: String) {
        val builder = AlertDialog.Builder(requireContext(), R.style.customAlertDialog)
        builder.setTitle(title)
        builder.setMessage(message)
            .show()
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
        val minSdk33 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

        readPermissionGranted = hasReadPermission || minSdk33
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
}