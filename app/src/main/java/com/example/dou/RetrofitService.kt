package com.example.dou

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface RetrofitService {

    @POST("/auth/register")
    fun signup(@Body request: SignupRequest): Call<SignupResponse>

    @POST("/auth/login")
    fun login(@Body request: LoginRequset): Call<LoginResponse>

    @GET("/user/{userId}")
    fun user(@Path("userId") memberId: Int?) : Call<UserResponse>

    @POST("/sentiment")
    fun emotion(@Body request: EmotionRequest): Call<EmotionResponse>

//    @POST("/record/register")
//    fun recordSave(@Body request: EmotionRequest): Call<RecordResponse>

    @GET("/record/{identifyId}")
    fun recordCheck(@Path("identifyId") identifyId: Int?) : Call<RecordCheckResponse>

    @GET("record/date")
    fun recordDate(@Query("date") date: String): Call<DateResponse>
}