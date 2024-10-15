package com.example.dou

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.dou.databinding.FragmentListBinding
import com.example.dou.databinding.FragmentRecord2Binding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class RecordFragment : Fragment() {
    private lateinit var binding: FragmentRecord2Binding
    private var roomId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // roomId 받기
        arguments?.let {
            roomId = it.getInt("roomId", -1)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentRecord2Binding.inflate(inflater, container, false)

        // 서버로부터 데이터를 가져와서 ViewPager 설정
        fetchRecordsByRoomId(roomId)

        return binding.root
    }

    private fun fetchRecordsByRoomId(roomId: Int) {
        val service = RetrofitApi.getRetrofitService
        val call = service.getRecordsByRoomId(
            roomId, cursorId = 0, limit = 10)

        call.enqueue(object : Callback<RecordGetResponse> {
            override fun onResponse(call: Call<RecordGetResponse>, response: Response<RecordGetResponse>) {
                if (response.isSuccessful) {
                    val recordList = response.body()?.data
                    recordList?.let {
                        // ViewPager 설정
                        setupViewPager(it)
                    }
                } else {
                    Log.e("RecordFragment", "레코드 가져오기 실패: ${response.code()} - ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<RecordGetResponse>, t: Throwable) {
                Log.e("RecordFragment", "레코드 요청 실패", t)
            }
        })
    }

    private fun setupViewPager(recordList: List<RecordGetData>) {
        // ViewPager 어댑터 설정
        val viewPagerAdapter = ViewPagerAdapter(this,recordList)
        binding.viewPager.adapter = viewPagerAdapter

        // DotsIndicator와 ViewPager2를 연결
        binding.dotsIndicator.attachTo(binding.viewPager)
    }


}