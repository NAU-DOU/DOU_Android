package com.example.dou

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dou.databinding.FragmentCalendarBinding
import com.example.dou.databinding.FragmentChatBinding

class ChatFragment : Fragment() {
    private lateinit var binding: FragmentChatBinding
    private lateinit var adapter: ChatAdapter
    private val chatItems = mutableListOf<ChatItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentChatBinding.inflate(inflater, container, false)

        adapter = ChatAdapter(chatItems as ArrayList<ChatItem>)
        binding.chatRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.chatRecycler.adapter = adapter

        binding.sendBtn.setOnClickListener {
            sendMessage()
        }

        return binding.root
    }

    private fun sendMessage() {
        val message = binding.editTxt.text.toString().trim()
        if (message.isNotEmpty()) {
            val chatItem = ChatItem(message, isSentByMe = true)
            chatItems.add(chatItem)
            adapter.notifyItemInserted(chatItems.size - 1)
            binding.chatRecycler.smoothScrollToPosition(chatItems.size - 1)
            binding.editTxt.text.clear()
        }
    }

    // 서버에서 받은 메시지를 처리하는 함수
    private fun receiveMessage(message: String) {
        val chatItem = ChatItem(message, isSentByMe = false)
        chatItems.add(chatItem)
        adapter.notifyItemInserted(chatItems.size - 1)
        binding.chatRecycler.smoothScrollToPosition(chatItems.size - 1)
    }
}