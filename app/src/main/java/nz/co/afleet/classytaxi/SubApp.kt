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

package nz.co.afleet.classytaxi

import android.app.Application
import nz.co.afleet.classytaxi.Constants.Companion.USE_FAKE_SERVER
import nz.co.afleet.classytaxi.billing.BillingClientLifecycle
import nz.co.afleet.classytaxi.data.DataRepository
import nz.co.afleet.classytaxi.data.disk.LocalDataSource
import nz.co.afleet.classytaxi.data.disk.db.AppDatabase
import nz.co.afleet.classytaxi.data.network.WebDataSource
import nz.co.afleet.classytaxi.data.network.firebase.FakeServerFunctions
import nz.co.afleet.classytaxi.data.network.firebase.ServerFunctions

/**
 * Android Application class. Used for accessing singletons.
 */
class SubApp : Application() {

    private val executors = AppExecutors()

    private val database: AppDatabase
        get() = AppDatabase.getInstance(this)

    private val localDataSource: LocalDataSource
        get() = LocalDataSource.getInstance(executors, database)

    private val serverFunctions: ServerFunctions
        get() {
            return if (USE_FAKE_SERVER) {
                FakeServerFunctions.getInstance()
            } else {
                ServerFunctions.getInstance()
            }
        }

    private val webDataSource: WebDataSource
        get() = WebDataSource.getInstance(executors, serverFunctions)

    val billingClientLifecycle: BillingClientLifecycle
        get() = BillingClientLifecycle.getInstance(this)

    val repository: DataRepository
        get() = DataRepository.getInstance(localDataSource, webDataSource, billingClientLifecycle)

}
