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

package nz.co.afleet.taxisubscription.data

import android.os.Parcel
import android.os.Parcelable

/**
 * SubscriptionContent is an immutable object that holds the various metadata associated with a Subscription.
 */
class SubscriptionContent : Parcelable {
    val title: String?
    val subtitle: String?
    val description: String?

    private constructor(
        title: String?,
        subtitle: String?,
        desc: String?
    ) {
        this.title = title
        this.subtitle = subtitle
        this.description = desc
    }

    private constructor(input: Parcel) {
        title = input.readString()
        subtitle = input.readString()
        description = input.readString()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(title)
        dest.writeString(subtitle)
        dest.writeString(description)
    }

    override fun toString() =
        "SubscriptionContent{title='$title', subtitle='$subtitle', description='$description'}"

    // Builder for Subscription object.
    class Builder {
        private var title: String? = null
        private var subtitle: String? = null
        private var desc: String? = null

        fun title(title: String): Builder {
            this.title = title
            return this
        }

        fun subtitle(subtitle: String): Builder {
            this.subtitle = subtitle
            return this
        }

        fun description(desc: String?): Builder {
            this.desc = desc
            return this
        }

        fun build(): SubscriptionContent {
            return SubscriptionContent(
                title,
                subtitle,
                desc
            )
        }
    }

    companion object CREATOR : Parcelable.Creator<SubscriptionContent> {
        override fun createFromParcel(parcel: Parcel): SubscriptionContent {
            return SubscriptionContent(parcel)
        }

        override fun newArray(size: Int): Array<SubscriptionContent?> {
            return arrayOfNulls(size)
        }
    }
}
