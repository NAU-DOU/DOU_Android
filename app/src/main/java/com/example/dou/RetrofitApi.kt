package com.example.dou

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitApi {

    private val okHttpClient = OkHttpClient.Builder().addInterceptor(AuthInterceptor()).build()

    private const val BASE_URL = "https://dev.nau-dou.shop/"
    private val getRetrofit by lazy{
        Retrofit.Builder()
            .client(okHttpClient) //토큰 인터셉터
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava3CallAdapterFactory.create()) // 이 부분 추가
            .build()
    }
    val getRetrofitService:RetrofitService by lazy{getRetrofit.create(RetrofitService::class.java)}


    private const val KAKAO_URL = "https://kapi.kakao.com/v1/"
    private val getKaKaoRetrofit by lazy {
        Retrofit.Builder()
            .baseUrl(KAKAO_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    val getKaKaoRetrofitService:RetrofitService by lazy{getKaKaoRetrofit.create(RetrofitService::class.java)}
}