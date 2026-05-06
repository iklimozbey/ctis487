package com.ctis487.smartwardrobe.network

import com.ctis487.smartwardrobe.db.ClothingItem
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {

    @Multipart
    @POST("api/items")
    fun uploadImage(
        @Part image: MultipartBody.Part
    ): Call<ResponseBody>

    @GET("api/items")
    fun getItems(): Call<ItemsResponse>

    @retrofit2.http.DELETE("api/items/{id}")
    fun deleteItem(@retrofit2.http.Path("id") id: String): Call<ResponseBody>

    @retrofit2.http.PUT("api/items/{id}/status")
    fun updateStatus(
        @retrofit2.http.Path("id") id: String,
        @retrofit2.http.Body status: Map<String, String>
    ): Call<ResponseBody>

    @POST("api/search")
    fun searchOutfit(@retrofit2.http.Body request: OutfitSearchRequest): Call<OutfitSearchResponse>

    @GET("api/health")
    fun getHealth(): Call<ResponseBody>
}