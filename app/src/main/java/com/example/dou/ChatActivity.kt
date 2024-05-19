package com.example.dou

import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dou.databinding.ActivityChatBinding
import model.Message
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class ChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatBinding
    private lateinit var adapter: ChatAdapter
    private val chatItems = mutableListOf<ChatItem>()

    private val messageList = ArrayList<Message>()
    private val JSON = "application/json; charset=utf-8".toMediaType()
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = ChatAdapter(chatItems)
        binding.chatRecycler.layoutManager = LinearLayoutManager(this)
        binding.chatRecycler.adapter = adapter

        val emotionResponse = intent.getParcelableExtra<Parcelable>("emotionResponse") as? EmotionResponse

        binding.sendBtn.setOnClickListener {
            sendMessage()
        }
    }

    private fun sendMessage() {
        val message = binding.editTxt.text.toString().trim()
        if (message.isNotEmpty()) {
            val chatItem = ChatItem(message, isSentByMe = true)
            chatItems.add(chatItem)
            adapter.notifyItemInserted(chatItems.size - 1)
            binding.chatRecycler.smoothScrollToPosition(chatItems.size - 1)
            binding.editTxt.text.clear()

            // 보낸 메시지를 messageList에 추가
            // 사용자인경우 '1'으로 설정함
            val sentMessage = Message("1", message)
            messageList.add(sentMessage)

            val apiKey = BuildConfig.API_KEY
            Log.d("apikey", apiKey)

            val arr = JSONArray()
            val baseAi = JSONObject()
            val userMsg = JSONObject()
            try {
                baseAi.put("role", "user")
                // 여기서 원하는 형태로 content 넣어 주면 될듯
                baseAi.put("content", "나는 당신의 감정을 인식하고 긍정적인 감정을 가질 수 있도록 부드럽게 대화하는 친구같은 도우미입니다.")
                userMsg.put("role", "user")
                userMsg.put("content", message)

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
                                        receiveMessage(content)
                                    } else {
                                        Log.e("API Communication", "No choices found in response.")
                                        Toast.makeText(this@ChatActivity, "응답에서 선택지를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: JSONException) {
                                    Log.e("API Communication", "Error parsing JSON response: $it", e)
                                    Toast.makeText(this@ChatActivity, "JSON 응답을 구문 분석하는 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            val errorMessage = "API 요청 실패 - 응답 코드: ${response.code}, 메시지: ${response.message}"
                            Log.e("API Communication", errorMessage)
                            Toast.makeText(this@ChatActivity, "API 요청 실패", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: okhttp3.Call, e: IOException) {
                        Log.e("API Communication", "API 통신 실패", e)
                        runOnUiThread {
                            Toast.makeText(applicationContext, "API 통신 실패", Toast.LENGTH_SHORT).show()
                        }
                        //Toast.makeText(this@ChatActivity, "API 통신 실패", Toast.LENGTH_SHORT).show()
                    }
                })
            } catch (e: Exception) {
                Log.e("Exception", "예외 발생", e)

                Toast.makeText(this@ChatActivity, "예외 발생", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "메시지를 입력해주세요.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun receiveMessage(message: String) {
        runOnUiThread {
            val chatItem = ChatItem(message, isSentByMe = false)
            chatItems.add(chatItem)
            adapter.notifyItemInserted(chatItems.size - 1)
            binding.chatRecycler.smoothScrollToPosition(chatItems.size - 1)

            // 받은 메시지를 messageList에 추가
            // 컴퓨터인경우 '0'으로 설정함
            val receivedMessage = Message("0", message)
            messageList.add(receivedMessage)

            // messageList 출력
            Log.d("MessageList", "All Messages:")
            for (msg in messageList) {
                Log.d("MessageList", "${msg.sentBy},${msg.message}")
            }
        }
    }
}