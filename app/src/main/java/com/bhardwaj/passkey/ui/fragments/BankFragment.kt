package com.bhardwaj.passkey.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.bhardwaj.passkey.R
import com.bhardwaj.passkey.databinding.FragmentBankBinding

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