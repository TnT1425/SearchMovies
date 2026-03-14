package com.example.searchmovies

import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface ApiService {
    @POST("identify-and-save")
    fun searchByName(@Query("query_name") name: String): Call<MovieResponse>

    @Multipart
    @POST("identify-by-image")
    fun indentifyByImage(@Part file: MultipartBody.Part): Call<MovieResponse>

    @GET("history")
    fun getHistory(): Call<Map<String,List<MovieInfo>>>
}

