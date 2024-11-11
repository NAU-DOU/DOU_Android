package com.example.dou

import android.content.Context
import android.content.Intent
import android.os.Bundle
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

        // 1. 저장된 액세스 토큰 확인
        val accessToken = getSavedAccessToken()
        if (accessToken != null) {
            // 저장된 토큰이 있다면 바로 MainActivity로 이동
            moveToMainActivity(accessToken)
        } else {
            // 저장된 토큰이 없으면 로그인 버튼 설정
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

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                view.loadUrl("javascript:window.Android.sendDataToApp(document.body.innerText);")
            }
        }

        webView.loadUrl(url)
    }

    inner class WebAppInterface {
        @JavascriptInterface
        fun sendDataToApp(data: String) {
            println("받은 데이터: $data")

            try {
                val jsonObject = JSONObject(data)
                val dataObject = jsonObject.getJSONObject("data")
                val accessToken = dataObject.getString("eid_access_token")

                println("추출된 액세스 토큰: $accessToken")

                // 2. 액세스 토큰 저장
                saveAccessToken(accessToken)

                // 3. MainActivity로 이동
                moveToMainActivity(accessToken)
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

    // 액세스 토큰을 SharedPreferences에 저장
    private fun saveAccessToken(accessToken: String) {
        val sharedPref = getSharedPreferences("auth", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("ACCESS_TOKEN", accessToken)
            apply()
        }
    }

    // 저장된 액세스 토큰 가져오기
    private fun getSavedAccessToken(): String? {
        val sharedPref = getSharedPreferences("auth", Context.MODE_PRIVATE)
        return sharedPref.getString("ACCESS_TOKEN", null)
    }
}
