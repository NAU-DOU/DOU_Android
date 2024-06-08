package com.example.dou

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface NaverSpeechService {
    @Multipart
    @POST("recognizer/upload")
    fun recognizeSpeech(
        @Part media: MultipartBody.Part,
        @Part("params") params: RequestBody,
        @Header("X-CLOVASPEECH-API-KEY") apiKey: String
    ): Call<SpeechResponse>
}

data class SpeechResponse(
    val text: String
)