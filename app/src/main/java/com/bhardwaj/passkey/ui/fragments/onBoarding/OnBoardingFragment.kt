package com.bhardwaj.passkey.ui.fragments.onBoarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bhardwaj.passkey.databinding.FragmentOnBoardingBinding

class OnBoardingFragment : Fragment() {
    private var binding: FragmentOnBoardingBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentOnBoardingBinding.inflate(layoutInflater)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fragmentList = arrayListOf(
            FirstPage(),
            SecondPage(),
            ThirdPage()
        )

        val adapter = ViewPagerAdapter(
            fragmentList,
            requireActivity().supportFragmentManager,
            lifecycle
        )

        binding?.pager?.apply {
            this.adapter = adapter
            this.setPageTransformer(ViewPagerTransformer())
        }
    }
}