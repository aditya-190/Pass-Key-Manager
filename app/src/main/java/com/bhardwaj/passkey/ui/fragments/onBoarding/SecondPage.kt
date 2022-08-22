package com.bhardwaj.passkey.ui.fragments.onBoarding

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.bhardwaj.passkey.R
import com.bhardwaj.passkey.databinding.FragmentSecondBinding

class SecondPage : Fragment() {
    private var binding: FragmentSecondBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSecondBinding.inflate(layoutInflater)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.tvSkip?.setOnClickListener {
            findNavController().navigate(R.id.onBoardingFragment_to_homeFragment)
            onBoardingFinished()
        }

        binding?.tvNext?.setOnClickListener {
            activity?.findViewById<ViewPager2>(R.id.pager)?.currentItem = 2
        }
    }

    private fun onBoardingFinished() {
        val sharedPref = requireActivity().getSharedPreferences("onBoarding", Context.MODE_PRIVATE)
        sharedPref.edit().putBoolean("Finished", true).apply()
    }
}