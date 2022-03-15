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

package nz.co.afleet.classytaxi.data.network

import androidx.lifecycle.LiveData
import nz.co.afleet.classytaxi.AppExecutors
import nz.co.afleet.classytaxi.data.SubscriptionStatus
import nz.co.afleet.classytaxi.data.network.firebase.ServerFunctions
import java.util.concurrent.Executor

/**
 * Execute network requests on the network thread.
 * Fetch data from a [ServerFunctions] object and expose with [subscriptions].
 */
class WebDataSource private constructor(
    private val executor: Executor,
    private val serverFunctions: ServerFunctions
) {

    /**
     * Live data is true when there are pending network requests.
     */
    val loading: LiveData<Boolean>
        get() = serverFunctions.loading

    /**
     * LiveData with the [SubscriptionStatus] information.
     */
    val subscriptions = serverFunctions.subscriptions

    /**
     * Live Data with the basic content.
     */
    val basicContent = serverFunctions.basicContent

    /**
     * Live Data with the premium content.
     */
    val premiumContent = serverFunctions.premiumContent

    /**
     * GET basic content.
     */
    fun updateBasicContent() = serverFunctions.updateBasicContent()

    /**
     * GET premium content.
     */
    fun updatePremiumContent() = serverFunctions.updatePremiumContent()

    /**
     * GET request for subscription status.
     */
    fun updateSubscriptionStatus() {
        executor.execute {
            synchronized(WebDataSource::class.java) {
                serverFunctions.updateSubscriptionStatus()
            }
        }
    }

    /**
     * POST request to register subscription.
     */
    fun registerSubscription(sku: String, purchaseToken: String) {
        executor.execute {
            synchronized(WebDataSource::class.java) {
                serverFunctions.registerSubscription(sku = sku, purchaseToken = purchaseToken)
            }
        }
    }

    /**
     * POST request to transfer a subscription that is owned by someone else.
     */
    fun postTransferSubscriptionSync(sku: String, purchaseToken: String) {
        executor.execute {
            synchronized(WebDataSource::class.java) {
                serverFunctions.transferSubscription(sku = sku, purchaseToken = purchaseToken)
            }
        }
    }

    /**
     * POST request to register an Instance ID.
     */
    fun postRegisterInstanceId(instanceId: String) {
        executor.execute {
            synchronized(WebDataSource::class.java) {
                serverFunctions.registerInstanceId(instanceId)
            }
        }
    }

    /**
     * POST request to unregister an Instance ID.
     */
    fun postUnregisterInstanceId(instanceId: String) {
        executor.execute {
            synchronized(WebDataSource::class.java) {
                serverFunctions.unregisterInstanceId(instanceId)
            }
        }
    }

    companion object {

        @Volatile
        private var INSTANCE: WebDataSource? = null

        fun getInstance(
            executors: AppExecutors,
            callableFunctions: ServerFunctions
        ): WebDataSource =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: WebDataSource(
                    executors.networkIO,
                    callableFunctions
                ).also { INSTANCE = it }
            }
    }

}
