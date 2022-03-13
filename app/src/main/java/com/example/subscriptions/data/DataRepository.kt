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

package com.example.subscriptions.data

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.android.billingclient.api.Purchase
import com.example.subscriptions.Constants
import com.example.subscriptions.billing.BillingClientLifecycle
import com.example.subscriptions.data.disk.LocalDataSource
import com.example.subscriptions.data.network.WebDataSource

/**
 * Repository handling the work with subscriptions.
 */
class DataRepository private constructor(
    private val localDataSource: LocalDataSource,
    private val webDataSource: WebDataSource,
    private val billingClientLifecycle: BillingClientLifecycle
) {

    /**
     * Live data is true when there are pending network requests.
     */
    val loading: LiveData<Boolean>
        get() = webDataSource.loading

    /**
     * [MediatorLiveData] to coordinate updates from the database and the network.
     *
     * The mediator observes multiple sources. The database source is immediately exposed.
     * The network source is stored in the database, which will eventually be exposed.
     * The mediator provides an easy way for us to use LiveData for both the local data source
     * and the network data source, without implementing a new callback interface.
     */
    val subscriptions = MediatorLiveData<List<SubscriptionStatus>>()

    /**
     * Live Data with the basic content.
     */
    val basicContent = MediatorLiveData<ContentResource>()

    /**
     * Live Data with the premium content.
     */
    val premiumContent = MediatorLiveData<ContentResource>()

    init {
        // Update content from the web.
        // We are using a MediatorLiveData so that we can clear the data immediately
        // when the subscription changes.
        basicContent.addSource(webDataSource.basicContent) {
            basicContent.postValue(it)
        }
        premiumContent.addSource(webDataSource.premiumContent) {
            premiumContent.postValue(it)
        }
        // Database changes are observed by the ViewModel.
        subscriptions.addSource(localDataSource.subscriptions) {
            Log.d("Repository", "Subscriptions updated: ${it?.size}")
            subscriptions.postValue(it)
        }
        // Observed network changes are store in the database.
        // The database changes will propagate to the ViewModel.
        // We could write different logic to ensure that the network call completes when
        // the UI component is inactive.
        subscriptions.addSource(webDataSource.subscriptions) {
            updateSubscriptionsFromNetwork(it)
        }
        // When the list of purchases changes, we need to update the subscription status
        // to indicate whether the subscription is local or not. It is local if the
        // the Google Play Billing APIs return a Purchase record for the SKU. It is not
        // local if there is no record of the subscription on the device.
        subscriptions.addSource(billingClientLifecycle.purchases) { it ->
            // We only need to update the database if the isLocalPurchase field needs to change.
            val purchases = it
            subscriptions.value?.let {
                val subscriptions = it
                val hasChanged = updateLocalPurchaseTokens(subscriptions, purchases)
                if (hasChanged) {
                    localDataSource.updateSubscriptions(subscriptions)
                }
            }
        }
    }

    fun updateSubscriptionsFromNetwork(remoteSubscriptions: List<SubscriptionStatus>?) {
        val oldSubscriptions = subscriptions.value
        val purchases = billingClientLifecycle.purchases.value
        val subscriptions =
            mergeSubscriptionsAndPurchases(oldSubscriptions, remoteSubscriptions, purchases)
        remoteSubscriptions?.let {
            acknowledgeRegisteredPurchaseTokens(remoteSubscriptions)
        }
        // Store the subscription information when it changes.
        localDataSource.updateSubscriptions(subscriptions)

        // Update the content when the subscription changes.
        remoteSubscriptions?.let {
            // Figure out which content we need to fetch.
            var updateBasic = false
            var updatePremium = false
            for (subscription in it) {
                when (subscription.sku) {
                    Constants.BASIC_SKU -> {
                        updateBasic = true
                    }
                    Constants.PREMIUM_SKU -> {
                        updatePremium = true
                        // Premium subscribers get access to basic content as well.
                        updateBasic = true
                    }
                }
            }

            if (updateBasic) {
                // Fetch the basic content.
                webDataSource.updateBasicContent()
            } else {
                // If we no longer own this content, clear it from the UI.
                basicContent.postValue(null)
            }
            if (updatePremium) {
                // Fetch the premium content.
                webDataSource.updatePremiumContent()
            } else {
                // If we no longer own this content, clear it from the UI.
                premiumContent.postValue(null)
            }
        }
    }

    /**
     * Acknowledge subscriptions that have been registered by the server.
     */
    private fun acknowledgeRegisteredPurchaseTokens(
        remoteSubscriptions: List<SubscriptionStatus>
    ) {
        for (remoteSubscription in remoteSubscriptions) {
            remoteSubscription.purchaseToken?.let { purchaseToken ->
                billingClientLifecycle.acknowledgePurchase(purchaseToken)
            }
        }
    }

    /**
     * Merge the previous subscriptions and new subscriptions by looking at on-device purchases.
     *
     * We want to return the list of new subscriptions, possibly with some modifications
     * based on old subscriptions and the on-devices purchases from Google Play Billing.
     * Old subscriptions should be retained if they are owned by someone else (subAlreadyOwned)
     * and the purchase token for the subscription is still on this device.
     */
    private fun mergeSubscriptionsAndPurchases(
        oldSubscriptions: List<SubscriptionStatus>?,
        newSubscriptions: List<SubscriptionStatus>?,
        purchases: List<Purchase>?
    ): List<SubscriptionStatus> {
        return ArrayList<SubscriptionStatus>().apply {
            if (purchases != null) {
                // Record which purchases are local and can be managed on this device.
                updateLocalPurchaseTokens(newSubscriptions, purchases)
            }
            if (newSubscriptions != null) {
                addAll(newSubscriptions)
            }
            // Find old subscriptions that are in purchases but not in new subscriptions.
            if (purchases != null && oldSubscriptions != null) {
                for (oldSubscription in oldSubscriptions) {
                    if (oldSubscription.subAlreadyOwned && oldSubscription.isLocalPurchase) {
                        // This old subscription was previously marked as "already owned" by
                        // another user. It should be included in the output if the SKU
                        // and purchase token match their previous value.
                        for (purchase in purchases) {
                            if (purchase.skus[0] == oldSubscription.sku &&
                                purchase.purchaseToken == oldSubscription.purchaseToken
                            ) {
                                // The old subscription that was already owned subscription should
                                // be added to the new subscriptions.
                                // Look through the new subscriptions to see if it is there.
                                var foundNewSubscription = false
                                newSubscriptions?.let {
                                    for (newSubscription in it) {
                                        if (newSubscription.sku == oldSubscription.sku) {
                                            foundNewSubscription = true
                                        }
                                    }
                                }
                                if (!foundNewSubscription) {
                                    // The old subscription should be added to the output.
                                    // It matches a local purchase.
                                    add(oldSubscription)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Modify the subscriptions isLocalPurchase field based on the list of local purchases.
     * Return true if any of the values changed.
     */
    private fun updateLocalPurchaseTokens(
        subscriptions: List<SubscriptionStatus>?,
        purchases: List<Purchase>?
    ): Boolean {
        var hasChanged = false
        subscriptions?.let { it ->
            for (subscription in it) {
                var isLocalPurchase = false
                var purchaseToken = subscription.purchaseToken
                purchases?.let {
                    for (purchase in it) {
                        if (subscription.sku == purchase.skus[0]) {
                            isLocalPurchase = true
                            purchaseToken = purchase.purchaseToken
                        }
                    }
                }
                if (subscription.isLocalPurchase != isLocalPurchase) {
                    subscription.isLocalPurchase = isLocalPurchase
                    subscription.purchaseToken = purchaseToken
                    hasChanged = true
                }
            }
        }
        return hasChanged
    }

    /**
     * Fetch subscriptions from the server and update local data source.
     */
    fun fetchSubscriptions() {
        webDataSource.updateSubscriptionStatus()
    }

    /**
     * Register subscription to this account and update local data source.
     */
    fun registerSubscription(sku: String, purchaseToken: String) {
        webDataSource.registerSubscription(sku = sku, purchaseToken = purchaseToken)
    }

    /**
     * Transfer subscription to this account and update local data source.
     */
    fun transferSubscription(sku: String, purchaseToken: String) {
        webDataSource.postTransferSubscriptionSync(sku = sku, purchaseToken = purchaseToken)
    }

    /**
     * Register Instance ID.
     */
    fun registerInstanceId(instanceId: String) {
        webDataSource.postRegisterInstanceId(instanceId)
    }

    /**
     * Unregister Instance ID.
     */
    fun unregisterInstanceId(instanceId: String) {
        webDataSource.postUnregisterInstanceId(instanceId)
    }

    /**
     * Delete local user data when the user signs out.
     */
    fun deleteLocalUserData() {
        localDataSource.deleteLocalUserData()
        basicContent.postValue(null)
        premiumContent.postValue(null)
    }

    companion object {

        @Volatile
        private var INSTANCE: DataRepository? = null

        fun getInstance(
            localDataSource: LocalDataSource,
            webDataSource: WebDataSource,
            billingClientLifecycle: BillingClientLifecycle
        ): DataRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: DataRepository(localDataSource, webDataSource, billingClientLifecycle)
                    .also { INSTANCE = it }
            }
    }

}
