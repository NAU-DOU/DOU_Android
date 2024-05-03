package com.example.dou

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dou.databinding.ActivityChatBinding

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