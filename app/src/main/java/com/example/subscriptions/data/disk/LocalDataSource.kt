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

package com.example.subscriptions.data.disk

import com.example.subscriptions.AppExecutors
import com.example.subscriptions.data.SubscriptionStatus
import com.example.subscriptions.data.disk.db.AppDatabase
import java.util.concurrent.Executor

class LocalDataSource private constructor(
        private val executor: Executor,
        private val appDatabase: AppDatabase
) {

    /**
     * Get the list of subscriptions from the localDataSource and get notified when the data changes.
     */
    val subscriptions = appDatabase.subscriptionStatusDao().getAll()

    fun updateSubscriptions(subscriptions: List<SubscriptionStatus>) {
        executor.execute {
            appDatabase.runInTransaction {
                // Delete existing subscriptions.
                appDatabase.subscriptionStatusDao().deleteAll()
                // Put new subscriptions data into localDataSource.
                appDatabase.subscriptionStatusDao().insertAll(subscriptions)
            }
        }
    }

    /**
     * Delete local user data when the user signs out.
     */
    fun deleteLocalUserData() = updateSubscriptions(listOf())

    companion object {

        @Volatile
        private var INSTANCE: LocalDataSource? = null

        fun getInstance(executors: AppExecutors, database: AppDatabase): LocalDataSource =
                INSTANCE ?: synchronized(this) {
                    INSTANCE ?: LocalDataSource(executors.diskIO, database).also { INSTANCE = it }
                }
    }

}