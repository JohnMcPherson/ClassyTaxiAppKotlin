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

package com.example.subscriptions

class Constants {

    companion object {
        // Use the fake local server data or real remote server.
        @Volatile
        var USE_FAKE_SERVER = false

        const val BASIC_SKU = "basic_subscription"
        const val PREMIUM_SKU = "premium_subscription"
        const val PLAY_STORE_SUBSCRIPTION_URL
                = "https://play.google.com/store/account/subscriptions"
        const val PLAY_STORE_SUBSCRIPTION_DEEPLINK_URL
                = "https://play.google.com/store/account/subscriptions?sku=%s&package=%s"
    }

}
