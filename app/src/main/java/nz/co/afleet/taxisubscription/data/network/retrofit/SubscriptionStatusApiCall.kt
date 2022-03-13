package nz.co.afleet.taxisubscription.data.network.retrofit

import nz.co.afleet.taxisubscription.data.ContentResource
import nz.co.afleet.taxisubscription.data.SubscriptionStatus
import nz.co.afleet.taxisubscription.data.SubscriptionStatusList

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

/**
 * [SubscriptionStatusApiCall] defines the API endpoints that are called in [ServerFunctionsImpl].
 */
interface SubscriptionStatusApiCall {

    // Fetch Basic content.
    @GET("content_basic_v2")
    fun fetchBasicContent(): Call<ContentResource>

    // Fetch Premium content.
    @GET("content_premium_v2")
    fun fetchPremiumContent(): Call<ContentResource>

    // Fetch Subscription Status.
    @GET("subscription_status_v2")
    fun fetchSubscriptionStatus(): Call<SubscriptionStatusList>

    // Registers Instance ID for Firebase Cloud Messaging.
    @PUT("subscription_register_v2")
    fun registerInstanceID(@Body instanceId: Map<String, String>): Call<String>

    // Unregisters Instance ID for Firebase Cloud Messaging.
    @PUT("instanceId_unregister_v2")
    fun unregisterInstanceID(@Body instanceId: Map<String, String>): Call<String>

    // Registers subscription status to the server.
    @PUT("subscription_register_v2")
    fun registerSubscription(@Body registerStatus: SubscriptionStatus): Call<SubscriptionStatusList>

    // Transfers subscription status to another account.
    @PUT("subscription_transfer_v2")
    fun transferSubscription(@Body transferStatus: SubscriptionStatus): Call<SubscriptionStatusList>
}