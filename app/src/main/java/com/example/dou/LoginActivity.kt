package com.example.dou

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.example.dou.databinding.ActivityLoginBinding
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.regex.Pattern
import kotlin.math.exp

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private var expire:Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        App.prefs.token = null

        val accessToken = getSavedAccessToken()
        Log.e("시작 액세스 토큰","$accessToken")
        if (accessToken != null) {
            validateAccessToken(accessToken) // 토큰 검사 및 자동 이동 처리
        } else {
            binding.btnKakao.setOnClickListener {
                kakaoLogin()
            }
        }
        binding.btnKakao.setOnClickListener {
            kakaoLogin()
        }

        val scaleAnimation = AnimationUtils.loadAnimation(this, R.anim.dou_scale)
        binding.homeDou.startAnimation(scaleAnimation)

    }

    private fun kakaoLogin() {
        val service = RetrofitApi.getRetrofitService
        val call = service.getKakao()

        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    val body = response.body()?.string()
                    val url = extractLoginUrl(body)

                    if (url != null) {
                        openWebView(url)
                    } else {
                        println("로그인 URL을 찾지 못했습니다.")
                    }
                } else {
                    println("카카오 로그인 실패: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                println("네트워크 오류: ${t.message}")
            }
        })
    }

    private fun extractLoginUrl(html: String?): String? {
        if (html == null) return null

        val pattern = Pattern.compile("continueUrl\":\"(https[^\"]+)\"")
        val matcher = pattern.matcher(html)

        return if (matcher.find()) {
            matcher.group(1)
        } else {
            null
        }
    }

    private fun openWebView(url: String) {
        val webView = WebView(this)
        setContentView(webView)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true // DOM Storage 활성화
        webView.addJavascriptInterface(WebAppInterface(webView), "Android")

        // Cookie 설정
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.setAcceptThirdPartyCookies(webView, true)
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                Log.d("WebView", "Page loaded: $url")

                // JavaScript 호출
                view.loadUrl("javascript:window.Android.sendDataToApp(document.body.innerText);")

                // 쿠키 처리
                val cookies = cookieManager.getCookie(url)
                Log.d("Cookies", "Cookies for $url: $cookies")

                if (cookies != null) {
                    val refreshToken = extractRefreshToken(cookies)
                    if (refreshToken != null) {
                        Log.d("RefreshToken", "Extracted refresh token: $refreshToken")
                        saveRefreshTokens(refreshToken)
                    } else {
                        Log.e("Cookie Error", "Refresh token not found in cookies.")
                    }
                } else {
                    Log.e("Cookie Error", "No cookies found for $url.")
                }
            }
        }

        webView.loadUrl(url)
    }

    private fun extractRefreshToken(cookies: String): String? {
        val pattern = Pattern.compile("eid_refresh_token=([^;]+)")
        val matcher = pattern.matcher(cookies)
        return if (matcher.find()) matcher.group(1) else null
    }

    // WebAppInterface 클래스 수정
    inner class WebAppInterface(private val webView: WebView) {
        @JavascriptInterface
        fun sendDataToApp(data: String) {
            Log.d("Received Data", data)

            try {
                val jsonObject = JSONObject(data)
                val dataObject = jsonObject.getJSONObject("data")
                val accessToken = dataObject.getString("eid_access_token")
                val userId = dataObject.getInt("userId")
                val userNickname = dataObject.getString("userNickname")

                Log.d("AccessToken", accessToken)
                Log.d("UserId", userId.toString())
                Log.d("UserNickname", userNickname)

                saveUserData(userId, userNickname)
                saveAccessTokens(accessToken)

                App.prefs.token = accessToken
                Log.d("Login_Token", "Access Token : ${accessToken}")

                // 쿠키 가져오기
                val cookieManager = CookieManager.getInstance()
                val cookies = cookieManager.getCookie("https://your-backend-url.com") // 서버의 정확한 도메인
                if (cookies != null) {
                    val refreshToken = extractRefreshToken(cookies)
                    if (refreshToken != null) {
                        saveRefreshTokens(refreshToken)
                    }
                }

                // 데이터 처리가 완료되면 웹뷰를 제거
                runOnUiThread {
                    (webView.parent as? ViewGroup)?.removeView(webView)
                }

                // MainActivity로 이동
                moveToMainActivity(accessToken)
            } catch (e: Exception) {
                Log.e("JSON Parsing Error", "Error parsing JSON: ${e.message}")
            }
        }
    }


    // userId와 userNickname을 SharedPreferences에 저장하는 메서드
    private fun saveUserData(userId: Int, userNickname: String) {
        val sharedPref = getSharedPreferences("userData", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putInt("USER_ID", userId)
            putString("USER_NICKNAME", userNickname)
            apply()
        }
    }

    private fun moveToMainActivity(accessToken: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("ACCESS_TOKEN", accessToken)
        }
        startActivity(intent)
        finish()
    }

    private fun saveAccessTokens(accessToken: String) {
        val sharedPref = getSharedPreferences("authAccess", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("ACCESS_TOKEN", accessToken)
            commit()
        }
    }

    private fun saveRefreshTokens(refreshToken:String) {
        val sharedPref = getSharedPreferences("authRefresh", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("REFRESH_TOKEN", refreshToken)
            commit()
        }
    }


    private fun getSavedToken(key: String): String? {
        val sharedPref = getSharedPreferences("authAccess", Context.MODE_PRIVATE)
        return sharedPref.getString(key, null)
    }

    private fun getRefreshToken(key: String): String? {
        val sharedPref = getSharedPreferences("authRefresh", Context.MODE_PRIVATE)
        return sharedPref.getString(key, null)
    }

    private fun getSavedAccessToken(): String? = getSavedToken("ACCESS_TOKEN")
    private fun getSavedRefreshToken(): String? = getRefreshToken("REFRESH_TOKEN")


    private fun validateAccessToken(token: String) {
        val service = RetrofitApi.getKaKaoRetrofitService
        val accessToken = "Bearer $token"
        Log.d("토큰 정보에서 토큰 길이 확인용", "토큰 확인용: ${accessToken}")

        val call = service.getkakaoInfo(accessToken)

        call.enqueue(object : Callback<KaKaoData> {
            override fun onResponse(p0: Call<KaKaoData>, p1: Response<KaKaoData>) {
                if (p1.isSuccessful) {
                    val expiresIn = p1.body()?.expires_in ?: 0
                    if (expiresIn > 0) {
                        // 유효한 토큰이므로 MainActivity로 이동
                        println("유효한 토큰입니다. MainActivity로 이동합니다.")
                        moveToMainActivity(token)
                    } else {
                        println("토큰이 만료되었습니다. 갱신을 시도합니다.")
                        refreshAccessToken()
                    }
                } else {
                    val errorBody = p1.errorBody()?.string()
                    errorBody?.let {
                        try {
                            val jsonObject = JSONObject(it)
                            val code = jsonObject.getInt("code")

                            if (code == -401) {
                                expire = true
                                println("토큰이 만료되었습니다. 갱신을 시도합니다.")
                                refreshAccessToken()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            println("JSON 파싱 오류: ${e.message}")
                        }
                    }
                }
            }

            override fun onFailure(p0: Call<KaKaoData>, p1: Throwable) {
                println("토큰 정보 API 네트워크 오류: ${p1.message}")
            }
        })
    }


//    private fun accessTokenExpired(): Boolean {
//        val sharedPref = getSharedPreferences("authAccess", Context.MODE_PRIVATE)
//        return sharedPref.getString("ACCESS_TOKEN", null) == null
//    }

    private fun refreshAccessToken() {
        val token = getSavedAccessToken()
        val refreshToken = getSavedRefreshToken()

        if (refreshToken != null) {
            val service = RetrofitApi.getRetrofitService
            val refreshTokenCookie = "eid_refresh_token=$refreshToken"
            val accessToken = "Bearer $token"

            val call = service.postRefreshToken(accessToken,refreshTokenCookie)

            call.enqueue(object : Callback<KaKaoRefreshResponse> {
                override fun onResponse(call: Call<KaKaoRefreshResponse>, response: Response<KaKaoRefreshResponse>) {
                    if (response.isSuccessful) {
                        val data = response.body()?.data
                        Log.d("로그인 리프레시 토큰", "로그인 리프레시 토큰: $data")
                        // 토큰 값 추출
                        val newAccessToken = data?.eid_access_token

//                        val body = response.body()?.string()
//                        val jsonObject = JSONObject(body ?: "")
//                        val newAccessToken = jsonObject.getJSONObject("data").getString("eid_access_token")

                        if (newAccessToken != null) {
                            println("갱신된 액세스 토큰: $newAccessToken")
                            saveAccessTokens(newAccessToken)
                            App.prefs.token = newAccessToken
                            moveToMainActivity(newAccessToken)
                        } else {
                            println("갱신된 액세스 토큰이 null입니다.")
                        }
                    } else {
                        println("토큰 갱신 실패: ${response.errorBody()?.string()}")
                    }
                }

                override fun onFailure(call: Call<KaKaoRefreshResponse>, t: Throwable) {
                    println("네트워크 오류: ${t.message}")
                }
            })
        } else {
            println("리프레시 토큰이 없습니다. 로그인 화면으로 이동합니다.")
            kakaoLogin() // 리프레시 토큰이 없으면 로그인 재시도
        }
    }
}