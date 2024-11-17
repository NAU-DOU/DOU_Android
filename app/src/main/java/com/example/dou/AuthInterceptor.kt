package com.example.dou

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = App.prefs.token
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()

        // Authorization 헤더가 이미 포함되어 있는지 확인
        if (!token.isNullOrEmpty() && originalRequest.header("Authorization") == null) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        val modifiedRequest = requestBuilder.build()
        Log.d("AuthInterceptor", "Request: $modifiedRequest")

        return chain.proceed(modifiedRequest)
    }
}
