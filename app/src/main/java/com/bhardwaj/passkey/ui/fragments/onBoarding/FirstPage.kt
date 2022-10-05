package com.bhardwaj.passkey.ui.fragments.onBoarding

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.bhardwaj.passkey.R
import com.bhardwaj.passkey.databinding.FragmentFirstBinding
import com.bhardwaj.passkey.utils.Constants.Companion.FINISHED
import com.bhardwaj.passkey.utils.Constants.Companion.ON_BOARDING

class FirstPage : Fragment() {
    private var binding: FragmentFirstBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentFirstBinding.inflate(layoutInflater)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.tvSkip?.setOnClickListener {
            findNavController().navigate(R.id.onBoardingFragment_to_authFragment)
            onBoardingFinished()
        }

        binding?.tvNext?.setOnClickListener {
            activity?.findViewById<ViewPager2>(R.id.pager)?.currentItem = 1
        }
    }

    private fun onBoardingFinished() {
        val sharedPref = requireActivity().getSharedPreferences(ON_BOARDING, Context.MODE_PRIVATE)
        sharedPref.edit().putBoolean(FINISHED, true).apply()
    }
}