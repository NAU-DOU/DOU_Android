package com.example.dou

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dou.databinding.ActivityChatBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatBinding
    private lateinit var adapter: ChatAdapter
    private val chatItems = mutableListOf<ChatItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = ChatAdapter(chatItems)
        binding.chatRecycler.layoutManager = LinearLayoutManager(this)
        binding.chatRecycler.adapter = adapter

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

            // OpenAI API를 통해 답변을 받음
            val request = OpenAIDataClass.Request(
                model = "gpt-3.5-turbo",
                prompt = message,
                temperature = 0.7f,
                maxTokens = 100
            )

            val apiKey = OpenAI.getApiKey()
            Log.d("apikey", "$apiKey")
            OpenAI.service.sendMessage(apiKey,request).enqueue(object : Callback<OpenAIDataClass.Response> {
                override fun onResponse(call: Call<OpenAIDataClass.Response>, response: Response<OpenAIDataClass.Response>) {
                    if (response.isSuccessful) {
                        val aiResponse = response.body()?.choices?.get(0)?.text
                        aiResponse?.let { receiveMessage(it) }
                    } else {
                        // API 요청이 실패한 경우 응답 코드와 메시지를 로그로 출력
                        val errorMessage = "API 요청 실패 - 응답 코드: ${response.code()}, 메시지: ${response.message()}"
                        Log.e("API Communication", errorMessage)
                        // API 요청이 실패한 경우 에러 메시지 표시
                        Toast.makeText(this@ChatActivity, "API 요청 실패", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<OpenAIDataClass.Response>, t: Throwable) {
                    // 통신 실패 시 에러 메시지 표시
                    Toast.makeText(this@ChatActivity, "통신 실패: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            Toast.makeText(this, "메시지를 입력해주세요.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun receiveMessage(message: String) {
        val chatItem = ChatItem(message, isSentByMe = false)
        chatItems.add(chatItem)
        adapter.notifyItemInserted(chatItems.size - 1)
        binding.chatRecycler.smoothScrollToPosition(chatItems.size - 1)
    }
}