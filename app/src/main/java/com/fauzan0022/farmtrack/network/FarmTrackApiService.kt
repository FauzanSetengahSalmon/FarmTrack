package com.fauzan0022.farmtrack.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface FarmTrackApiService {

    @GET("livestock")
    suspend fun getLivestock(
        @Query("user_email") userEmail: String
    ): List<LivestockDto>

    @Multipart
    @POST("livestock")
    suspend fun storeLivestock(
        @Part("user_email") userEmail: RequestBody,
        @Part("name") name: RequestBody,
        @Part("type") type: RequestBody,
        @Part("age") age: RequestBody,
        @Part("weight") weight: RequestBody,
        @Part photo: MultipartBody.Part
    ): OpStatusDto

    @Headers("Content-Type: application/json")
    @PUT("livestock/{id}")
    suspend fun updateLivestock(
        @Path("id") id: Int,
        @Body body: LivestockUpdateDto
    ): OpStatusDto

    @DELETE("livestock/{id}")
    suspend fun deleteLivestock(
        @Path("id") id: Int
    ): OpStatusDto

    companion object {
        private const val BASE_URL = "https://farmtrack-api-production-51d6.up.railway.app/api/"

        fun create(): FarmTrackApiService {
            val logger = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(logger)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()

            return retrofit.create(FarmTrackApiService::class.java)
        }
    }
}
