package com.example.dou

import android.content.Context
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
        BuildConfig.STT_SECRET_KEY
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

            val contentUri = Uri.parse(fileUri)
            if (contentUri.scheme == "content") {
                recognizeSpeechFromContent(contentUri)  // Content URI 처리
            } else {
                val audioFile = File(contentUri.path!!)
                if (audioFile.exists()) {
                    recognizeSpeechFromContent(contentUri)
                } else {
                    Log.e("FileUri", "파일이 존재하지 않습니다: ${audioFile.path}")
                }
            }
        } else {
            Log.d("FileUri", "fileUri가 존재하지 않습니다.")
        }
    }

    private fun recognizeSpeechFromContent(contentUri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Content URI에서 파일 가져오기
                val inputStream = contentResolver.openInputStream(contentUri)
                inputStream?.use { stream ->
                    val tempFile = File.createTempFile("audio_", ".mp3", cacheDir) // 임시 파일 생성
                    tempFile.outputStream().use { outputStream ->
                        stream.copyTo(outputStream)
                    }

                    // 바이너리 데이터 준비
                    val requestFile = tempFile.asRequestBody("application/octet-stream".toMediaTypeOrNull())
                    val body = MultipartBody.Part.createFormData("media", tempFile.name, requestFile)

                    // JSON 형식의 파라미터 준비
                    val paramsJson = """
                    {
                        "language": "ko-KR",
                        "completion": "sync"
                    }
                """.trimIndent()
                    val params = paramsJson.toRequestBody("application/json".toMediaTypeOrNull())

                    // Retrofit 호출 (요청 전 로그 출력)
                    val call = naverSpeechService.recognizeSpeech(body, params, apiKey)

                    // 요청 URL 로그 출력
                    Log.d("Request URL", call.request().url.toString())

                    // 요청 헤더 로그 출력
                    Log.d("Request Headers", call.request().headers.toString())

                    // 요청 본문 로그 출력 (주의: 바이너리 데이터는 내용이 크므로 체크)
                    val buffer = okio.Buffer()
                    call.request().body?.writeTo(buffer)
                    Log.d("Request Body", "Body size: ${buffer.size}")
                    Log.d("Request Body (Partial)", buffer.readUtf8().take(500)) // 앞 500자만 로그

                    // 실제 API 호출
                    call.enqueue(object : Callback<SpeechResponse> {
                        override fun onFailure(call: Call<SpeechResponse>, t: Throwable) {
                            Log.e("NaverSTT", "Failed to recognize speech", t)
                            runOnUiThread {
                                Toast.makeText(this@EmotionActivity, "음성 인식 실패", Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onResponse(call: Call<SpeechResponse>, response: Response<SpeechResponse>) {
                            if (response.isSuccessful) {
                                val responseBody = response.body()
                                Log.d("Response,Naver", "Response: $response")
                                val originalSentences = responseBody?.text ?: ""
                                Log.d("NaverSTT", "Response: $originalSentences")

                                // Room 생성 후 GPT 요청
                                addRoom { roomId ->
                                    sendGPTParagraphMessage(originalSentences, roomId)
                                }
                            } else {
                                val errorBody = response.errorBody()?.string()
                                Log.e("NaverSTT", "Error response: ${response.code()} - $errorBody")
                                runOnUiThread {
                                    Toast.makeText(this@EmotionActivity, "오류 발생: ${response.code()}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    })
                } ?: run {
                    Log.e("ContentUri", "Content URI에서 파일을 열 수 없습니다.")
                    runOnUiThread {
                        Toast.makeText(this@EmotionActivity, "파일을 열 수 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("ContentUri", "Content URI 처리 중 오류 발생", e)
                runOnUiThread {
                    Toast.makeText(this@EmotionActivity, "파일 처리 중 오류 발생", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun sendGPTParagraphMessage(userInput: String, roomId: Int) {
        val apiKey = BuildConfig.API_KEY
        Log.d("apikey", apiKey)

        Log.d("userInput", "userInput: $userInput")

        val arr = JSONArray()
        val baseAi = JSONObject()
        val userMsg = JSONObject()
        try {
            try {
                baseAi.put("role", "user")
                baseAi.put("content", "주어진 텍스트를 문단으로 나누세요. 의미 있는 문장이 끝날 때만 줄바꿈(\n)을 추가하세요. 다른 설명이나 빈 문단은 포함하지 마세요.")

                userMsg.put("role", "user")
                userMsg.put("content", "텍스트:\n$userInput")
            } catch (e: JSONException) {
                Log.e("JSON Error", "프롬프트 생성 중 오류 발생", e)
            }


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
                                    var content = jsonArray.getJSONObject(0).getJSONObject("message").getString("content")

                                    // Clean paragraphs to remove empty lines
                                    content = cleanParagraphs(content) // 여기에 cleanParagraphs 호출

                                    Log.d("Paragraph Result", "Paragraph Result:  $content")

                                    handleResponse(content, userInput, roomId)

//                                    Log.d("Paragraph Result", "Paragraph Result:  $content")
//
//                                    handleResponse(content, userInput, roomId)
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

    private fun getUserId(): Int {
        val sharedPref = getSharedPreferences("userData", Context.MODE_PRIVATE)
        return sharedPref.getInt("USER_ID", -1) // 기본값 -1
    }

    private fun addRoom(onRoomCreated: (Int) -> Unit) {
        val userId = getUserId()
        if (userId != -1) {
            Log.d("UserData", "User ID: $userId")
        } else {
            Log.d("UserData", "No User ID found in SharedPreferences")
        }

        val request = RoomAddRequest(roomUserId = userId, roomSent = 1)

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

    private fun cleanParagraphs(rawOutput: String): String {
        // 줄바꿈(\n) 기준으로 분리하고, 빈 문장 제거
        return rawOutput.lines()
            .filter { it.isNotBlank() } // 빈 줄 제거
            .joinToString("\n") // 다시 합치기
    }

}