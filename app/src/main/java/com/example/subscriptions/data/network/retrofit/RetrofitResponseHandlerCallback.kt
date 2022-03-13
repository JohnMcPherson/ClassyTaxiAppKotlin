/*
 * Copyright 2021 Google LLC. All rights reserved.
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

package com.example.subscriptions.data.network.retrofit

import android.util.Log
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException

/**
 * [Callback] that simplifies handling success and failure
 *
 * @param <T> Type to return when successful
 */
abstract class RetrofitResponseHandlerCallback<T>(
    private val methodName: String,
    private val pendingRequestCounter: PendingRequestCounter
) : Callback<T> {

    /**
     * Called when HTTPS call has a response
     *
     * @param call     HTTPS call object
     * @param response HTTPS call's [Response] object
     */
    override fun onResponse(call: Call<T>, response: Response<T>) {
        pendingRequestCounter.decrementRequestCount()
        if (response.isSuccessful) {
            Log.i(TAG, "$methodName returned successfully")
            onSuccess(response.body()!!)
            return
        }
        // Non 2xx response, get the details and call onError
        val errorMessage: String
        val errorBody = response.errorBody()
        errorMessage = if (errorBody == null) {
            "No error body received"
        } else {
            try {
                errorBody.string()
            } catch (e: IOException) {
                e.message ?: "IOException with errorBody"
            }
        }
        onError(response.code(), errorMessage)
    }

    /**
     * Called when HTTPS call fails
     *
     * @param call HTTPS call object
     * @param t    HTTPS call's failure [Throwable] object
     */
    override fun onFailure(call: Call<T>, t: Throwable) {
        pendingRequestCounter.decrementRequestCount()
        onError(NO_HTTP_CODE, t.message ?: "IOException on Failure response")
    }

    /**
     * Called when a successful response returns from the server
     *
     * @param response Successful response object
     */
    protected abstract fun onSuccess(response: T)

    /**
     * Called when any error happens such as a connection failure or a 500 server response
     *
     *
     * The default implementation just calls [.logError]. Override for custom
     * error handling.
     *
     * @param errorCode    HTTP error code or [.NO_HTTP_CODE] if none
     * @param errorMessage Optional error message for logging
     */
    protected open fun onError(errorCode: Int, errorMessage: String) {
        logError(errorCode, errorMessage)
    }

    /**
     * Logs an error to logcat as a warning
     *
     * @param errorCode    HTTP error code or [.NO_HTTP_CODE] if none
     * @param errorMessage Optional error message for logging
     */
    protected fun logError(errorCode: Int, errorMessage: String) {
        val sb = StringBuilder(methodName).append(" failed")
        if (errorCode != NO_HTTP_CODE) {
            sb.append(" (Error code: ").append(errorCode).append(")")
        }
        sb.append(": ")
        sb.append(errorMessage)
        Log.w(TAG, sb.toString())
    }

    companion object {
        /**
         * Used for errors without an HTTP code such as a connection failure
         */
        private const val NO_HTTP_CODE = -1

        private const val TAG = "RetrofitCallback"
    }
}