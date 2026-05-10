package com.ctis487.smartwardrobe.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface ApiService {

    @Multipart
    @POST("api/items")
    fun uploadImage(
        @Part image: MultipartBody.Part,
        @Part("options") options: RequestBody
    ): Call<ResponseBody>

    @GET("api/items")
    fun getItems(): Call<ItemsResponse>

    @DELETE("api/items/{id}")
    fun deleteItem(@Path("id") id: String): Call<ResponseBody>

    @PUT("api/items/{id}/status")
    fun updateStatus(
        @Path("id") id: String,
        @Body status: Map<String, String>
    ): Call<ResponseBody>

    @POST("api/items/{id}/worn")
    fun markItemWorn(@Path("id") id: String): Call<ResponseBody>

    @POST("api/search")
    fun searchOutfit(@Body request: OutfitSearchRequest): Call<OutfitSearchResponse>

    // Weather
    @GET("api/weather")
    fun getWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("city") city: String? = null,
        @Query("date") date: String? = null,
        @Query("hourly") hourly: Boolean = false
    ): Call<WeatherResponse>

    // Reverse geocoding
    @GET("api/weather/reverse-geocode")
    fun reverseGeocode(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double
    ): Call<GeoResponse>

    @GET("api/profile")
    fun getProfile(): Call<ProfileResponse>

    @POST("api/profile")
    fun saveProfile(@Body profile: UserProfile): Call<ProfileResponse>

    @GET("api/analytics")
    fun getAnalytics(): Call<AnalyticsResponse>

    @GET("api/health")
    fun getHealth(): Call<ResponseBody>
}