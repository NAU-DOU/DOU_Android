package com.example.dou

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dou.databinding.FragmentCalendarBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CalendarFragment : Fragment() {
    private lateinit var binding: FragmentCalendarBinding
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    // 대화내용을 출력하기 위한 어댑터 => 채팅할 때 사용한 어댑터와 동일함
    private lateinit var adapter: ChatAdapter
    private val chatItems = mutableListOf<ChatItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = ChatAdapter(chatItems)
        binding.calChatRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.calChatRecycler.adapter = adapter
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentCalendarBinding.inflate(inflater, container, false)

        // 현재 연도와 월을 가져옵니다.
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)

        // 현재 연도와 월을 기준으로 해당 달의 마지막 날짜를 가져옵니다.
        val lastDayOfMonth = getLastDayOfMonth(currentYear, currentMonth)

        // CalItem 객체들을 생성하여 리스트에 추가합니다.
        val calItems = ArrayList<CalItem>()
        for (i in 1..lastDayOfMonth) {
            calItems.add(CalItem(i))
        }

        // RecyclerView에 어댑터 설정
        val calAdapter = CalAdapter(calItems) { position ->
            // 클릭한 아이템의 위치(position)를 받아서 처리합니다.

            val calendar = Calendar.getInstance()
            calendar.set(Calendar.YEAR, currentYear)
            calendar.set(Calendar.MONTH, currentMonth)
            calendar.set(Calendar.DAY_OF_MONTH, position + 1) // position은 0부터 시작하므로 +1 처리합니다.

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val formattedDate = dateFormat.format(calendar.time)
            Log.d("CalendarFragment", "Clicked date: $formattedDate")

            openBottomSheet(formattedDate)
        }

        binding.calRecycler.layoutManager = GridLayoutManager(context, 7)
        binding.calRecycler.adapter = calAdapter

        // BottomSheet 설정
        bottomSheetBehavior = BottomSheetBehavior.from(binding.emotionTitleLayout)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN // 초기에는 숨김 상태로 설정

        // BottomSheet 토글 버튼 클릭 이벤트 처리
        binding.calTxtLayout.setOnClickListener {
            toggleBottomSheet()
        }

        return binding.root
    }
    fun getLastDayOfMonth(year: Int, month: Int): Int {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.MONTH, month)
        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    // BottomSheet 토글 함수
    private fun toggleBottomSheet() {
        if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_HIDDEN) {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        } else {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }
    }

    // BottomSheet를 열기 위한 함수
    private fun openBottomSheet(formattedDate: String) {
        // userId는 추후에 로그인 한 후에 설정해주면 될 듯
        val request = DateRequest(formattedDate)
        val service = RetrofitApi.getRetrofitService
        val call = service.recordDate(request)

        call.enqueue(object : Callback<DateResponse> {
            override fun onResponse(call: Call<DateResponse>, response: Response<DateResponse>) {
                if(response.isSuccessful){
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED

                    // 받은 데이터를 바탕으로 대화 리사이클러뷰 구성하면 될듯!
                    // 0: 컴퓨터 isSentByMe = false, 1: 사용자 isSentByMe = true
                    // 0이면 isSentByMe를 false로 설정한 후 item add 해주면 될 듯
//                    val chatItem = ChatItem(message, isSentByMe = false)
//                    chatItems.add(chatItem)

                    val dateResponse = response.body()
                    // 아마 for 문 돌면서 화면에 출력하는 방향으로?
                }
                else{
                    Log.e("날짜별 기록 조회 API", "API 호출 실패: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<DateResponse>, t: Throwable) {
                Log.e("날짜별 기록 조회 API_에러", "API 호출 실패", t)
            }
        })

        // api 요청했을 때 응답으로 돌아오는 값이 없는 경우는 대화가 없다는 의미여서 일단 이런 식으로 구현해두기
        binding.nonTalkLayout.visibility = View.VISIBLE
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }
}