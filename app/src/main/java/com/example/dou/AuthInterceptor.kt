package com.example.dou

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = App.prefs.token
        val requestBuilder = chain.request().newBuilder()

        if (!token.isNullOrEmpty()) {
            requestBuilder.addHeader("Authorization", token)
        }

        Log.d("AuthInterceptor", "Request: ${requestBuilder.build()}")
        return chain.proceed(requestBuilder.build())
    }
}
