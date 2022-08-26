package com.bhardwaj.passkey.ui.fragments.onBoarding

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bhardwaj.passkey.R
import com.bhardwaj.passkey.databinding.FragmentSplashBinding

class SplashFragment : Fragment() {
    private var binding: FragmentSplashBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSplashBinding.inflate(layoutInflater)

        if (onBoardingFinished()) {
            findNavController().navigate(R.id.splashFragment_to_authFragment)
        } else {
            findNavController().navigate(R.id.splashFragment_to_onBoardingFragment)
        }

        return binding?.root
    }

    private fun onBoardingFinished(): Boolean {
        val sharedPref = requireActivity().getSharedPreferences("onBoarding", Context.MODE_PRIVATE)
        return sharedPref.getBoolean("Finished", false)
    }
}