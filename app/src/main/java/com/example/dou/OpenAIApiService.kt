package com.example.dou

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

interface OpenAIApiService {
    @Headers("Content-Type: application/json")
    @POST("v1/completions")
    fun sendMessage(
        @Header("Authorization") apiKey: String,
        @Body body: OpenAIDataClass.Request
    ): Call<OpenAIDataClass.Response>
}