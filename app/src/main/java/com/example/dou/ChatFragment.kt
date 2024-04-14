package com.example.dou

import android.graphics.Rect
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dou.databinding.FragmentCalendarBinding
import com.example.dou.databinding.FragmentChatBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

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

        // EditText가 선택될 때 키보드 상태를 감지하여 처리
        binding.editTxt.viewTreeObserver.addOnGlobalLayoutListener {
            val r = Rect()
            binding.editTxt.getWindowVisibleDisplayFrame(r)
            val screenHeight = binding.editTxt.rootView.height
            val keypadHeight = screenHeight - r.bottom
            if (keypadHeight > screenHeight * 0.15) { // if keypad height > 15% of the screen height
                hideBottomNavigation()
            } else {
                showBottomNavigation()

                // 키보드가 닫혔을 때 NestedScrollView를 항상 마지막 부분으로 스크롤
                binding.chatLayout.post {
                    binding.chatLayout.fullScroll(View.FOCUS_DOWN)
                }
            }
        }

        // RecyclerView의 아이템이 추가될 때마다 NestedScrollView를 자동 스크롤
        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                binding.chatLayout.post {
                    binding.chatLayout.fullScroll(View.FOCUS_DOWN)
                    binding.chatRecycler.smoothScrollToPosition(chatItems.size - 1) // RecyclerView 가장 아래쪽으로 스크롤

                }
            }
        })

        // 메시지를 보낸 후에도 EditText에 포커스를 유지
        binding.sendBtn.setOnClickListener {
            sendMessage()
            binding.editTxt.requestFocus()
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

    // 하단 네비게이션 바를 숨기는 함수
    private fun hideBottomNavigation() {
        val bottomNav = requireActivity().findViewById<View>(R.id.bottomNavi)
        bottomNav.visibility = View.GONE
    }

    // 하단 네비게이션 바를 보여주는 함수
    private fun showBottomNavigation() {
        val bottomNav = requireActivity().findViewById<View>(R.id.bottomNavi)
        bottomNav.visibility = View.VISIBLE
    }
    // RecyclerView를 마지막 아이템의 위치로 스크ㅗㄹ. 항상 마지막 채팅 메시지가 보이도록
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.chatRecycler.scrollToPosition(chatItems.size - 1)
    }
}