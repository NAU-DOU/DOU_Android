package com.example.dou

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.dou.databinding.ActivityEmotionBinding
import com.github.ybq.android.spinkit.style.ThreeBounce
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.IOException

class EmotionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEmotionBinding
    private lateinit var naverSpeechService: NaverSpeechService

    private val JSON = "application/json; charset=utf-8".toMediaType()
    private val client = OkHttpClient()

    private val apiKey: String by lazy {
        BuildConfig.STT_API_KEY
    }

    private val invokeUrl: String by lazy {
        BuildConfig.INVOKE_URL
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityEmotionBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        // 뒤로가기 버튼을 눌렀을 때 처리할 콜백 설정
//        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
//            override fun handleOnBackPressed() {
//                Toast.makeText(this@EmotionActivity, "뒤로가기를 할 수 없어", Toast.LENGTH_LONG).show()
//            }
//        })

        val progressBar = binding.spinKit
        val threeBounce = ThreeBounce()
        progressBar.setIndeterminateDrawable(threeBounce)

        // Retrofit 빌더 설정
        val retrofit = Retrofit.Builder()
            .baseUrl("$invokeUrl/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(OkHttpClient.Builder().build())
            .build()

        naverSpeechService = retrofit.create(NaverSpeechService::class.java)

        val fileUri = intent.getStringExtra("fileUri")
        if (fileUri != null) {
            Log.d("FileUri", "fileUri가 존재합니다: $fileUri")

            val audioFile = File(Uri.parse(fileUri).path!!)
            if (audioFile.exists()) {
                recognizeSpeechFromFile(audioFile)
            } else {
                Log.e("FileUri", "파일이 존재하지 않습니다: ${audioFile.path}")
            }
        } else {
            Log.d("FileUri", "fileUri가 존재하지 않습니다.")
        }
    }

    private fun recognizeSpeechFromFile(audioFile: File) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val requestFile = audioFile.asRequestBody("application/octet-stream".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("media", audioFile.name, requestFile)

                val paramsJson = """
                    {
                        "language": "ko-KR",
                        "completion": "sync"
                    }
                """.trimIndent()
                val params = paramsJson.toRequestBody("application/json".toMediaTypeOrNull())

                val call = naverSpeechService.recognizeSpeech(body, params, apiKey)
                call.enqueue(object : Callback<SpeechResponse> {
                    override fun onFailure(call: Call<SpeechResponse>, t: Throwable) {
                        Log.e("NaverSTT", "Failed to recognize speech", t)
                    }

                    override fun onResponse(call: Call<SpeechResponse>, response: Response<SpeechResponse>) {
                        if (response.isSuccessful) {
                            val responseBody = response.body()
                            val originalSentences = responseBody?.text ?: ""
                            Log.d("NaverSTT", "Response: $originalSentences")
                            addRoom { roomId ->
                                sendGPTParagraphMessage(originalSentences, roomId)
                            }
                        } else {
                            val errorBody = response.errorBody()?.string()
                            Log.e("NaverSTT", "Error response: ${response.code()} - $errorBody")
                        }
                    }
                })
            } catch (e: Exception) {
                Log.e("UploadAudio", "오디오 파일 업로드 중 오류 발생", e)
            }
        }
    }

    private fun sendGPTParagraphMessage(userInput: String, roomId: Int) {
        val apiKey = BuildConfig.API_KEY
        Log.d("apikey", apiKey)

        val arr = JSONArray()
        val baseAi = JSONObject()
        val userMsg = JSONObject()
        try {
            baseAi.put("role", "user")
            baseAi.put("content", "단순히 내가 요청한 정보만 제공해주면 돼. 1, 2, 3 이런 식으로 안 나눠도 되고 요약도 안해도 돼 그냥 내가 보낸 문장들을 문단으로 나눠줘 그리고 문단의 뒤에 \n을 붙여서 표현해줬으면 좋겠어, 문장을 ''로 묶지 말아줘 그냥 문단의 뒤에 \n만 붙여줘")
            userMsg.put("role", "user")
            userMsg.put("content", "$userInput \n 라는 글을 문단으로 나눠주고 문단의 뒤에 \n을 붙여줘")

            arr.put(baseAi)
            arr.put(userMsg)
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }

        val jsonObject = JSONObject()
        try {
            jsonObject.put("model", "gpt-3.5-turbo")
            jsonObject.put("messages", arr)
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        val body = RequestBody.create(JSON, jsonObject.toString())
        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .post(body)
            .build()

        try {
            val call = client.newCall(request)
            call.enqueue(object : okhttp3.Callback {
                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        responseBody?.let {
                            try {
                                val jsonObject = JSONObject(it)
                                val jsonArray = jsonObject.getJSONArray("choices")
                                if (jsonArray.length() > 0) {
                                    val content = jsonArray.getJSONObject(0).getJSONObject("message").getString("content")

                                    Log.d("Paragraph Result", content)

                                    handleResponse(content, userInput, roomId)
                                } else {
                                    Log.e("API Communication", "No choices found in response.")
                                    Toast.makeText(this@EmotionActivity, "응답에서 선택지를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: JSONException) {
                                Log.e("API Communication", "Error parsing JSON response: $it", e)
                                Toast.makeText(this@EmotionActivity, "JSON 응답을 구문 분석하는 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        val errorMessage = "API 요청 실패 - 응답 코드: ${response.code}, 메시지: ${response.message}"
                        Log.e("API Communication", errorMessage)
                        Toast.makeText(this@EmotionActivity, "API 요청 실패", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: okhttp3.Call, e: IOException) {
                    Log.e("API Communication", "API 통신 실패", e)
                    runOnUiThread {
                        Toast.makeText(applicationContext, "API 통신 실패", Toast.LENGTH_SHORT).show()
                    }
                }
            })
        } catch (e: Exception) {
            Log.e("Exception", "예외 발생", e)
            Toast.makeText(this@EmotionActivity, "예외 발생", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleResponse(sentences: String?, originalSentences: String?, roomId: Int?) {
        if (sentences != null && originalSentences != null && roomId != null) {
            val intent = Intent(this, SentenceActivity::class.java).apply {
                putExtra("sentences", sentences)
                putExtra("originalSentences", originalSentences)
                putExtra("roomId", roomId)
            }
            startActivity(intent)
            finish()
        }
    }

    private fun addRoom(onRoomCreated: (Int) -> Unit) {
        val request = RoomAddRequest(roomUserId = 2, roomSent = 1)

        val service = RetrofitApi.getRetrofitService
        val call = service.roomAdd(request)

        call.enqueue(object : Callback<RoomAddRespose> {
            override fun onResponse(call: Call<RoomAddRespose>, response: Response<RoomAddRespose>) {
                if (response.isSuccessful) {
                    val roomAddResponse = response.body()
                    val roomId = roomAddResponse?.data?.roomId
                    Log.d("roomAddResponse", "Response: $roomAddResponse")
                    Log.d("roomId", "RoomId Check: $roomId")

                    if (roomId != null) {
                        onRoomCreated(roomId)
                    } else {
                        Log.e("roomId", "Room ID is null")
                        Toast.makeText(this@EmotionActivity, "Room ID 생성 실패", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.d("roomAddResponse", "Failed with response code: ${response.code()}")
                    Toast.makeText(this@EmotionActivity, "Room 생성 실패", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<RoomAddRespose>, t: Throwable) {
                Log.e("RoomAddRequest", "Request failed", t)
                Toast.makeText(this@EmotionActivity, "Room 생성 중 오류 발생", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
