package com.example.dou

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dou.databinding.FragmentListBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ListFragment : Fragment() {
    private lateinit var binding: FragmentListBinding
    private val listItem = arrayListOf<ListItem>()
    private lateinit var listAdapter: ListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentListBinding.inflate(inflater, container, false)

        // 리사이클러뷰 및 어댑터 설정
        setupRecyclerView()

        // 방 리스트 가져오기
        fetchRoomList()

        binding.talkBtn.setOnClickListener {
            val navController = findNavController()
            navController.navigate(R.id.action_listFragment_to_homeFragment)
        }

        return binding.root
    }

    // 리사이클러뷰와 어댑터 설정
    private fun setupRecyclerView() {
        listAdapter = ListAdapter(listItem) { roomId ->
            // roomId를 RecordFragment로 전달
            val bundle = Bundle().apply {
                putInt("roomId", roomId)
            }
            findNavController().navigate(R.id.action_listFragment_to_recordFragment2, bundle)
        }

        binding.listRecycler.layoutManager = LinearLayoutManager(context)
        binding.listRecycler.adapter = listAdapter
    }

    // 방 리스트를 서버에서 가져오기
    private fun fetchRoomList() {
        val service = RetrofitApi.getRetrofitService  // Retrofit 인스턴스 가져오기
        val call = service.getAllRooms()

        call.enqueue(object : Callback<RoomListResponse> {
            override fun onResponse(call: Call<RoomListResponse>, response: Response<RoomListResponse>) {
                if (response.isSuccessful) {
                    val roomListResponse = response.body()
                    roomListResponse?.let {
                        updateRecyclerView(it.data)
                    }
                } else {
                    Log.e("ListFragment", "방 리스트 가져오기 실패: ${response.code()} - ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<RoomListResponse>, t: Throwable) {
                Log.e("ListFragment", "방 리스트 요청 실패", t)
            }
        })
    }

    // 리사이클러뷰 데이터 업데이트
    private fun updateRecyclerView(roomList: List<RoomListData>) {
        listItem.clear()  // 기존 데이터를 지우고
        roomList.reversed().forEach { room ->
            // room_id를 사용하여 ListItem 생성
            val newListItem = ListItem(
                roomId = room.room_id,         // room_id를 roomId로 사용
                listCnt = "#${room.room_id}",
                listTxt = room.room_date.substring(0, 10)  // 날짜만 추출
            )
            listItem.add(newListItem)
        }
        listAdapter.notifyDataSetChanged()  // 어댑터에 변경 사항을 알려줌
    }
}