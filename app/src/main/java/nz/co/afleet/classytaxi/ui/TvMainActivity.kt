/*
 * Copyright 2019 Google LLC. All rights reserved.
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

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProviders
import com.android.billingclient.api.Purchase
import nz.co.afleet.classytaxi.R
import com.firebase.ui.auth.AuthUI
import nz.co.afleet.classytaxi.Constants
import nz.co.afleet.classytaxi.SubApp

/**
 * TvMainActivity contains a TvMainFragment that leverages Leanback UI to build an optimized
 * Android TV experience for Classy Taxi.
 *
 * This Activity follows a nearly identical pattern to its sibling class MainActivity, subscribing to the same
 * ViewModels and providing similar business logic.
 */
class TvMainActivity : FragmentActivity() {

    companion object {
        private const val TAG = "TvMainActivity"
        private const val RC_SIGN_IN = 0
    }

    private lateinit var billingClientLifecycle: nz.co.afleet.classytaxi.billing.BillingClientLifecycle

    private lateinit var authenticationViewModel: FirebaseUserViewModel
    private lateinit var billingViewModel: BillingViewModel
    private lateinit var subscriptionViewModel: SubscriptionStatusViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tv_main)

        authenticationViewModel = ViewModelProviders.of(this).get(FirebaseUserViewModel::class.java)
        billingViewModel = ViewModelProviders.of(this).get(BillingViewModel::class.java)
        subscriptionViewModel =
            ViewModelProviders.of(this).get(SubscriptionStatusViewModel::class.java)

        // Billing APIs are all handled in the this lifecycle observer.
        billingClientLifecycle = (application as SubApp).billingClientLifecycle
        lifecycle.addObserver(billingClientLifecycle)

        // Register purchases when they change.
        billingClientLifecycle.purchaseUpdateEvent.observe(this, {
            it?.let {
                registerPurchases(it)
            }
        })

        // Launch the billing flow when the user clicks a button to buy something.
        billingViewModel.buyEvent.observe(this, {
            it?.let {
                billingClientLifecycle.launchBillingFlow(this, it)
            }
        })

        // Open the Play Store when this event is triggered.
        billingViewModel.openPlayStoreSubscriptionsEvent.observe(this, {
            Log.i(TAG, "Viewing subscriptions on the Google Play Store")
            val sku = it
            val url = if (sku == null) {
                // If the SKU is not specified, just open the Google Play subscriptions URL.
                Constants.PLAY_STORE_SUBSCRIPTION_URL
            } else {
                // If the SKU is specified, open the deeplink for this SKU on Google Play.
                String.format(Constants.PLAY_STORE_SUBSCRIPTION_DEEPLINK_URL, sku, packageName)
            }
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            startActivity(intent)
        })

        // Update authentication UI.
        authenticationViewModel.firebaseUser.observe(this, {
            invalidateOptionsMenu()
            if (it == null) {
                triggerSignIn()
            } else {
                Log.d(TAG, "CURRENT user: " + it.email + " " + it.displayName)
            }
        })

        // Update subscription information when user changes.
        authenticationViewModel.userChangeEvent.observe(this, {
            subscriptionViewModel.userChanged()
            billingClientLifecycle.purchaseUpdateEvent.value?.let {
                registerPurchases(it)
            }
        })

    }

    /**
     * Register SKUs and purchase tokens with the server.
     */
    private fun registerPurchases(purchaseList: List<Purchase>) {
        for (purchase in purchaseList) {
            val sku = purchase.skus[0]
            val purchaseToken = purchase.purchaseToken
            Log.d(TAG, "Register purchase with sku: $sku, token: $purchaseToken")
            subscriptionViewModel.registerSubscription(
                sku = sku,
                purchaseToken = purchaseToken
            )
        }
    }

    /**
     * Sign in with FirebaseUI Auth.
     */
    fun triggerSignIn() {
        Log.d(TAG, "Attempting SIGN-IN!")
        val providers = listOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )
        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build(),
            RC_SIGN_IN
        )
    }

    /**
     * Sign out with FirebaseUI Auth.
     */
    fun triggerSignOut() {
        subscriptionViewModel.unregisterInstanceId()
        AuthUI.getInstance().signOut(this).addOnCompleteListener {
            Log.d(TAG, "User SIGNED OUT!")
            authenticationViewModel.updateFirebaseUser()
        }
    }

    /**
     * Receive Activity result, including sign-in result.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            RC_SIGN_IN -> {
                // If sign-in is successful, update ViewModel.
                if (resultCode == RESULT_OK) {
                    Log.d(TAG, "Sign-in SUCCESS!")
                    authenticationViewModel.updateFirebaseUser()
                } else {
                    Log.d(TAG, "Sign-in FAILED!")
                }
            }
            else -> {
                Log.e(TAG, "Unrecognized request code: $requestCode")
            }
        }
    }
}