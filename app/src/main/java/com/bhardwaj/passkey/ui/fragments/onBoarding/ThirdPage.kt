package com.bhardwaj.passkey.ui.fragments.onBoarding

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.bhardwaj.passkey.R
import com.bhardwaj.passkey.databinding.FragmentThirdBinding

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
            findNavController().navigate(R.id.onBoardingFragment_to_homeFragment)
            onBoardingFinished()
        }
    }

    private fun onBoardingFinished() {
        val sharedPref = requireActivity().getSharedPreferences("onBoarding", Context.MODE_PRIVATE)
        sharedPref.edit().putBoolean("Finished", true).apply()
    }
}