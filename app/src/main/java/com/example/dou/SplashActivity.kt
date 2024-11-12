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
import okhttp3.ResponseBody
import org.json.JSONObject
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

            Log.d("AccessToken", "액세스토큰 스플래시: ${accessToken}")
            if (accessToken != null) {
                validateAccessToken(accessToken)
            } else {
                navigateToLogin()
            }
        }, 1000) // 애니메이션 길이와 동일하게 설정 (1000ms)
    }

    private fun validateAccessToken(token: String) {
        val service = RetrofitApi.getKaKaoRetrofitService
        val accessToken = "Bearer $token"

        Log.d("validateAccessToken", "검증할 액세스 토큰: $token")

        val call = service.getkakaoInfo(accessToken)
        call.enqueue(object : Callback<KaKaoData> {
            override fun onResponse(call: Call<KaKaoData>, response: Response<KaKaoData>) {
                if (response.isSuccessful) {
                    val expiresIn = response.body()?.expires_in ?: 0
                    Log.d("validateAccessToken", "토큰 유효성 검사 성공. 남은 만료 시간: $expiresIn 초")

//                    if (expiresIn > 0) {
//                        // 유효한 토큰 -> 메인 화면으로 이동
//                        Log.d("validateAccessToken", "유효한 토큰. MainActivity로 이동.")
//                        navigateToMain()
//                    } else {
//                        // 만료된 토큰 -> 로그인 화면으로 이동
//                        Log.d("validateAccessToken", "토큰이 만료됨. LoginActivity로 이동.")
//                        refreshAccessToken()
//                        navigateToMain()
//                    }
                } else {
                    // 유효하지 않은 토큰 -> 로그인 화면으로 이동
                    Log.e("validateAccessToken", "토큰 유효성 검사 실패. 응답 코드: ${response.code()}, 에러 메시지: ${response.message()}")
                    refreshAccessToken(token)
                    //navigateToMain()
                }
            }

            override fun onFailure(call: Call<KaKaoData>, t: Throwable) {
                // 네트워크 오류 시 로그인 화면으로 이동
                Log.e("validateAccessToken", "네트워크 오류: ${t.message}")
            }
        })
    }


    private fun getSavedAccessToken(): String? {
        val sharedPref = getSharedPreferences("authAccess", Context.MODE_PRIVATE)
        return sharedPref.getString("ACCESS_TOKEN", null)
    }

    private fun moveToMainActivity(accessToken: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("ACCESS_TOKEN", accessToken)
        }
        startActivity(intent)
        finish()
    }


    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        overridePendingTransition(0, 0)
        finish()
    }

    private fun getRefreshToken(): String? {
        val sharedPref = getSharedPreferences("authRefresh", Context.MODE_PRIVATE)
        return sharedPref.getString("REFRESH_TOKEN", null)
    }

    private fun refreshAccessToken(token:String) {
        val refreshToken = getRefreshToken()

        if (refreshToken != null) {
            val service = RetrofitApi.getRetrofitService
            val refreshTokenCookie = "eid_refresh_token=$refreshToken"
            Log.d("리프레시토큰", "리프레시 토큰 내놔: ${refreshTokenCookie}")

            val call = service.postRefreshToken("Bearer $token", refreshTokenCookie)
            call.enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (response.isSuccessful) {
                        val body = response.body()?.string()
                        val jsonObject = JSONObject(body ?: "")
                        val newAccessToken = jsonObject.getJSONObject("data").getString("eid_access_token")

                        Log.d("refreshAccessToken", "갱신된 액세스 토큰: $newAccessToken")
                        saveAccessTokens(newAccessToken)  // 새 액세스 토큰을 SharedPreferences에 저장

                        // 갱신된 액세스 토큰으로 유효성 검사 후 MainActivity로 이동
                        moveToMainActivity(newAccessToken)
                    } else {
                        Log.e("refreshAccessToken", "토큰 갱신 실패: ${response.errorBody()?.string()}")
                        navigateToLogin()  // 갱신 실패 시 로그인 화면으로 이동
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Log.e("refreshAccessToken", "네트워크 오류: ${t.message}")
                    navigateToLogin()  // 네트워크 오류 시 로그인 화면으로 이동
                }
            })
        } else {
            Log.e("refreshAccessToken", "리프레시 토큰이 없습니다. 로그인 화면으로 이동합니다.")
            navigateToLogin()  // 리프레시 토큰이 없을 경우 로그인 화면으로 이동
        }
    }


    private fun saveAccessTokens(accessToken: String) {
        val sharedPref = getSharedPreferences("authAccess", Context.MODE_PRIVATE)

        with(sharedPref.edit()) {
            putString("ACCESS_TOKEN", accessToken)
            commit()
        }
    }
}
