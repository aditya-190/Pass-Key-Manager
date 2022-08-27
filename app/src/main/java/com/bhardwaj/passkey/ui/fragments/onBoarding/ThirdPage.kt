package com.bhardwaj.passkey.ui.fragments.onBoarding

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bhardwaj.passkey.R
import com.bhardwaj.passkey.databinding.FragmentThirdBinding
import com.bhardwaj.passkey.utils.Constants.Companion.FINISHED
import com.bhardwaj.passkey.utils.Constants.Companion.ON_BOARDING

class ThirdPage : Fragment() {
    private var binding: FragmentThirdBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentThirdBinding.inflate(layoutInflater)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.tvNext?.setOnClickListener {
            findNavController().navigate(R.id.onBoardingFragment_to_authFragment)
            onBoardingFinished()
        }
    }

    private fun onBoardingFinished() {
        val sharedPref = requireActivity().getSharedPreferences(ON_BOARDING, Context.MODE_PRIVATE)
        sharedPref.edit().putBoolean(FINISHED, true).apply()
    }
}