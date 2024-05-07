package com.example.dou

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object OpenAI {
    private const val BASE_URL = "https://api.openai.com/"

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val service: OpenAIApiService = retrofit.create(OpenAIApiService::class.java)
}