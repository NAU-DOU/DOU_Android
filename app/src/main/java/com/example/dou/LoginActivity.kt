package com.example.dou

import android.content.Context
import android.content.Intent
import android.os.Bundle
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

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val accessToken = getSavedAccessToken()
        if (accessToken != null) {
            validateAccessToken(accessToken)
        } else {
            binding.btnKakao.setOnClickListener {
                kakaoLogin()
            }
        }
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
        webView.addJavascriptInterface(WebAppInterface(), "Android")

        //Cookie 관리를 위한 매니저
        CookieManager.getInstance().setAcceptCookie(true)

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                println("웹페이지 로딩 완료, JavaScript 호출 시도")
                view.loadUrl("javascript:window.Android.sendDataToApp(document.body.innerText);")

                // 쿠키를 가져오는 부분
                val cookieManager = CookieManager.getInstance()
                val cookies = cookieManager.getCookie(url)

                if (cookies != null) {
                    println("웹뷰 쿠키: $cookies")

                    // 리프레시 토큰 추출
                    val refreshToken = extractRefreshToken(cookies)
                    if (refreshToken != null) {
                        println("리프레시 토큰: $refreshToken")
                        saveRefreshTokens(refreshToken)
                    }
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

    inner class WebAppInterface {
        @JavascriptInterface
        fun sendDataToApp(data: String) {
            println("받은 데이터: $data")

            try {
                val jsonObject = JSONObject(data)
                val dataObject = jsonObject.getJSONObject("data")
                val accessToken = dataObject.getString("eid_access_token")
                //val refreshToken = dataObject.getString("refresh_token")

                println("받은 액세스 토큰: $accessToken")
                moveToMainActivity(accessToken)
                saveAccessTokens(accessToken)
            } catch (e: Exception) {
                e.printStackTrace()
                println("JSON 파싱 오류: ${e.message}")
            }
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
            apply()
        }
    }

    private fun saveRefreshTokens(refreshToken:String) {
        val sharedPref = getSharedPreferences("authRefresh", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("REFRESH_TOKEN", refreshToken)
            apply()
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


    private fun validateAccessToken(accessToken: String) {
        if (accessTokenExpired()) {
            println("토큰이 만료되었습니다. 갱신을 시도합니다.")
            refreshAccessToken()
        } else {
            println("유효한 토큰입니다. MainActivity로 이동합니다.")
            moveToMainActivity(accessToken)
        }
    }

    private fun accessTokenExpired(): Boolean {
        val sharedPref = getSharedPreferences("authAccess", Context.MODE_PRIVATE)
        return sharedPref.getString("ACCESS_TOKEN", null) == null
    }

    private fun refreshAccessToken() {
        val token = getSavedAccessToken()
        val refreshToken = getSavedRefreshToken()

        if (refreshToken != null) {
            val service = RetrofitApi.getRetrofitService
            val refreshTokenCookie = "eid_refresh_token=$refreshToken"
            val accessToken = "Bearer $token"

            val call = service.postRefreshToken(accessToken,refreshTokenCookie)

            call.enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (response.isSuccessful) {
                        val body = response.body()?.string()
                        val jsonObject = JSONObject(body ?: "")
                        val newAccessToken = jsonObject.getJSONObject("data").getString("eid_access_token")

                        println("갱신된 액세스 토큰: $newAccessToken")
                        saveAccessTokens(newAccessToken)
                        moveToMainActivity(newAccessToken)
                    } else {
                        println("토큰 갱신 실패: ${response.errorBody()?.string()}")
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    println("네트워크 오류: ${t.message}")
                }
            })
        } else {
            println("리프레시 토큰이 없습니다. 로그인 화면으로 이동합니다.")
            kakaoLogin() // 리프레시 토큰이 없으면 로그인 재시도
        }
    }
}
