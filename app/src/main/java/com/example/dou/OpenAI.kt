package com.example.dou

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object OpenAI {
    private const val API_KEY = "Bearer sk-proj-vBIrBb9eqQREw0QCjLYsT3BlbkFJmbFC4e7mXfdWtJQ7XVXA" // 여기에 본인의 API 키를 입력하세요

    // API 키를 반환하는 함수
    fun getApiKey(): String {
        return API_KEY
    }

    private const val BASE_URL = "https://api.openai.com/"

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val service: OpenAIApiService = retrofit.create(OpenAIApiService::class.java)
}