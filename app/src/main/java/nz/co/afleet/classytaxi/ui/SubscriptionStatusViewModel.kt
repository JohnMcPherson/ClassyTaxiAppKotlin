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

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import nz.co.afleet.classytaxi.SubApp
import nz.co.afleet.classytaxi.data.SubscriptionStatus

class SubscriptionStatusViewModel(application: Application) : AndroidViewModel(application) {

    /**
     * Data repository.
     */
    private val repository = (application as SubApp).repository

    /**
     * Live data is true when there are pending network requests.
     */
    val loading: LiveData<Boolean>
        get() = repository.loading

    /**
     * Subscriptions LiveData.
     */
    val subscriptions: LiveData<List<SubscriptionStatus>> = repository.subscriptions

    /**
     * Live Data with the basic content.
     */
    val basicContent = repository.basicContent

    /**
     * Live Data with the premium content.
     */
    val premiumContent = repository.premiumContent

    fun unregisterInstanceId() {
        // Unregister current Instance ID before the user signs out.
        // This is an authenticated call, so you cannot do this after the sign-out has completed.
        instanceIdToken?.let {
            repository.unregisterInstanceId(it)
        }
    }

    fun userChanged() {
        repository.deleteLocalUserData()
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }
            val token = task.result
            if (token != null) {
                registerInstanceId(token)
            }
        })
        repository.fetchSubscriptions()
    }

    fun manualRefresh() {
        repository.fetchSubscriptions()
    }

    /**
     * Keep track of the last Instance ID to be registered, so that it
     * can be unregistered when the user signs out.
     */
    private var instanceIdToken: String? = null

    /**
     * Register Instance ID.
     */
    private fun registerInstanceId(token: String) {
        repository.registerInstanceId(token)
        // Keep track of the Instance ID so that it can be unregistered.
        instanceIdToken = token
    }

    /**
     * Register a new subscription.
     */
    fun registerSubscription(sku: String, purchaseToken: String) =
        repository.registerSubscription(sku, purchaseToken)

    /**
     * Transfer the subscription to this account.
     */
    fun transferSubscriptions() {
        Log.d(TAG, "transferSubscriptions")
        subscriptions.value?.let {
            for (subscription in it) {
                val sku = subscription.sku
                val purchaseToken = subscription.purchaseToken
                if (sku != null && purchaseToken != null) {
                    repository.transferSubscription(sku = sku, purchaseToken = purchaseToken)
                }
            }
        }
    }

    companion object {
        private const val TAG = "SubViewModel"
    }

}
