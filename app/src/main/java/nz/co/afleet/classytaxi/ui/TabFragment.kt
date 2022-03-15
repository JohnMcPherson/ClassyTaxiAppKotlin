/*
 * Copyright 2018 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nz.co.afleet.classytaxi.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import nz.co.afleet.classytaxi.R
import nz.co.afleet.classytaxi.databinding.FragmentHomeBinding
import nz.co.afleet.classytaxi.databinding.FragmentPremiumBinding
import nz.co.afleet.classytaxi.databinding.FragmentSettingsBinding

/**
 * [Fragment] for Home, Premium, and Settings tabs.
 */
class TabFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val billingViewModel =
            ViewModelProviders.of(requireActivity()).get(BillingViewModel::class.java)
        val subscriptionViewModel =
            ViewModelProviders.of(requireActivity()).get(SubscriptionStatusViewModel::class.java)

        return when (arguments?.getInt(ARG_SECTION_NUMBER)) {
            MainActivity.HOME_PAGER_INDEX -> createHomeView(
                inflater, container, billingViewModel, subscriptionViewModel
            )
            MainActivity.PREMIUM_PAGER_INDEX -> createPremiumView(
                inflater, container, billingViewModel, subscriptionViewModel
            )
            MainActivity.SETTINGS_PAGER_INDEX -> createSettingsView(
                inflater, container, billingViewModel, subscriptionViewModel
            )
            else -> {
                Log.e(TAG, "Unrecognized fragment index")
                createHomeView(inflater, container, billingViewModel, subscriptionViewModel)
            }
        }
    }

    /**
     * Inflate the UI view for the Home tab and bind to the ViewModel.
     */
    private fun createHomeView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        billingViewModel: BillingViewModel,
        subscriptionViewModel: SubscriptionStatusViewModel
    ): View {
        // Data binding with a ViewModel.
        val fragmentBinding: FragmentHomeBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_home, container, false)
        fragmentBinding.lifecycleOwner = this
        fragmentBinding.billingViewModel = billingViewModel
        fragmentBinding.subscriptionViewModel = subscriptionViewModel
        return fragmentBinding.root
    }

    /**
     * Inflate the UI view for the Premium tab and bind to the ViewModel.
     */
    private fun createPremiumView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        billingViewModel: BillingViewModel,
        subscriptionViewModel: SubscriptionStatusViewModel
    ): View {
        // Data binding with a ViewModel.
        val fragmentBinding: FragmentPremiumBinding =
            DataBindingUtil.inflate(
                inflater, R.layout.fragment_premium, container, false
            )
        fragmentBinding.lifecycleOwner = this
        fragmentBinding.billingViewModel = billingViewModel
        fragmentBinding.subscriptionViewModel = subscriptionViewModel
        return fragmentBinding.root
    }

    /**
     * Inflate the UI view for the Settings tab and bind to the ViewModel.
     */
    private fun createSettingsView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        billingViewModel: BillingViewModel,
        subscriptionViewModel: SubscriptionStatusViewModel
    ): View {
        // Data binding with a ViewModel.
        val fragmentBinding: FragmentSettingsBinding =
            DataBindingUtil.inflate(
                inflater, R.layout.fragment_settings, container, false
            )
        fragmentBinding.lifecycleOwner = this
        fragmentBinding.billingViewModel = billingViewModel
        fragmentBinding.subscriptionViewModel = subscriptionViewModel
        return fragmentBinding.root
    }

    companion object {
        private const val TAG = "TabFragment"

        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private const val ARG_SECTION_NUMBER = "section_number"

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        fun newInstance(sectionNumber: Int): TabFragment {
            val args = Bundle().apply {
                putInt(ARG_SECTION_NUMBER, sectionNumber)
            }
            return TabFragment().apply { arguments = args }
        }
    }

}
