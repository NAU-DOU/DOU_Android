package com.example.dou

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.example.dou.databinding.ActivitySplashBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 애니메이션 적용
        val slideUpAnimation = AnimationUtils.loadAnimation(this, R.anim.dou_slide_up)
        binding.splachDou.startAnimation(slideUpAnimation)

        Handler(Looper.getMainLooper()).postDelayed({
            val accessToken = getSavedAccessToken()
            val refreshToken = getRefreshToken()

            Log.d("SplashActivity", "AccessToken: $accessToken, RefreshToken: $refreshToken")
            if (accessToken != null && refreshToken != null) {
                // 리프레시 토큰을 사용해 새 액세스 토큰 요청
                refreshAccessToken(refreshToken)
            } else {
                // 로그아웃 상태 -> 로그인 화면으로 이동
                navigateToLogin()
            }
        }, 1000) // 애니메이션 길이와 동일하게 설정 (1000ms)
    }

    private fun getSavedAccessToken(): String? {
        val sharedPref = getSharedPreferences("authAccess", Context.MODE_PRIVATE)
        return sharedPref.getString("ACCESS_TOKEN", null)
    }

    private fun getRefreshToken(): String? {
        val sharedPref = getSharedPreferences("authRefresh", Context.MODE_PRIVATE)
        return sharedPref.getString("REFRESH_TOKEN", null)
    }

    private fun moveToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        overridePendingTransition(0, 0)
        finish()
    }

    private fun saveAccessTokens(accessToken: String) {
        val sharedPref = getSharedPreferences("authAccess", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("ACCESS_TOKEN", accessToken)
            commit()
        }
    }

    private fun refreshAccessToken(refreshToken: String) {
        val service = RetrofitApi.getRetrofitService
        val accessToken = getSavedAccessToken()
        val refreshTokenCookie = "eid_refresh_token=$refreshToken"


        Log.d("SplashActivity", "Attempting token refresh with RefreshToken: $refreshTokenCookie")

        service.postRefreshToken("Bearer $accessToken", refreshTokenCookie)
            .enqueue(object : Callback<KaKaoRefreshResponse> {
                override fun onResponse(
                    call: Call<KaKaoRefreshResponse>,
                    response: Response<KaKaoRefreshResponse>
                ) {
                    if (response.isSuccessful) {
                        val data = response.body()?.data
                        val newAccessToken = data?.eid_access_token

                        if (newAccessToken != null) {
                            Log.d("SplashActivity", "New AccessToken received: $newAccessToken")
                            saveAccessTokens(newAccessToken) // 새로운 액세스 토큰 저장
                            App.prefs.token = newAccessToken
                            moveToMainActivity() // 메인 화면으로 이동
                        } else {
                            Log.e("SplashActivity", "Token refresh failed. New AccessToken is null.")
                            navigateToLogin() // 토큰 갱신 실패 시 로그인 화면으로 이동
                        }
                    } else {
                        Log.e("SplashActivity", "Token refresh failed. Response: ${response.errorBody()?.string()}")
                        navigateToLogin() // 실패 시 로그인 화면으로 이동
                    }
                }

                override fun onFailure(call: Call<KaKaoRefreshResponse>, t: Throwable) {
                    Log.e("SplashActivity", "Token refresh failed. Network error: ${t.message}")
                    navigateToLogin() // 네트워크 오류 시 로그인 화면으로 이동
                }
            })
    }
}
