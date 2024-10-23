package com.example.dou

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dou.databinding.FragmentFirstBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FirstFragment : Fragment() {

    private lateinit var binding: FragmentFirstBinding
    private lateinit var recordData: RecordGetData
    private lateinit var chatAdapter: ChatAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 전달된 데이터를 받는다
        arguments?.let {
            recordData = it.getParcelable(ARG_RECORD_DATA)!!
        }

        // RecyclerView 어댑터 초기화
        chatAdapter = ChatAdapter(emptyList())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentFirstBinding.inflate(inflater, container, false)

        // RecyclerView 설정
        binding.chatRecycler2.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = chatAdapter
        }

        // 데이터를 UI에 바인딩
        binding.tvRecordSum.text = recordData.recordSummary

        // rec_id로 채팅 데이터를 요청
        fetchChatData(recordData.recordId)

        return binding.root
    }

    // 채팅 데이터를 서버에서 가져오기
    private fun fetchChatData(recordId: Int) {
        val service = RetrofitApi.getRetrofitService  // Retrofit 인스턴스 가져오기
        val call = service.getChat(recordId, cursorId = 0, limit = 0)  // 채팅 API 요청 생성

        call.enqueue(object : Callback<ChatGetResponse> {
            override fun onResponse(call: Call<ChatGetResponse>, response: Response<ChatGetResponse>) {
                if (response.isSuccessful) {
                    val chatGetResponse = response.body()
                    chatGetResponse?.let {
                        // 서버로부터 데이터를 성공적으로 받으면 RecyclerView 업데이트
                        updateRecyclerView(it.data.reversed())
                    }
                } else {
                    Log.e("FirstFragment", "채팅 데이터 가져오기 실패: ${response.code()} - ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<ChatGetResponse>, t: Throwable) {
                Log.e("FirstFragment", "채팅 데이터 요청 실패", t)
            }
        })
    }

    // RecyclerView 업데이트 함수
    private fun updateRecyclerView(chatList: List<ChatGetData>) {
        val chatItems = chatList.map {
            ChatItem(it.chatContext, it.isUser == 1) // 채팅 데이터 매핑
        }
        chatAdapter = ChatAdapter(chatItems)
        binding.chatRecycler2.adapter = chatAdapter
    }

    companion object {
        private const val ARG_RECORD_DATA = "record_data"

        // newInstance 메서드를 사용하여 데이터 전달
        fun newInstance(record: RecordGetData): FirstFragment {
            val fragment = FirstFragment()
            val args = Bundle().apply {
                putParcelable(ARG_RECORD_DATA, record)
            }
            fragment.arguments = args
            return fragment
        }
    }
}