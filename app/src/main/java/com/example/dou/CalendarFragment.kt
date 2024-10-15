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
    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    // 대화내용을 출력하기 위한 어댑터 => 채팅할 때 사용한 어댑터와 동일함
    private lateinit var adapter: ChatAdapter
    private val chatItems = mutableListOf<ChatItem>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ChatAdapter(chatItems)
        binding.calChatRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.calChatRecycler.adapter = adapter

        // 현재 연도와 월을 가져옵니다.
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)

        // 현재 연도와 월을 기준으로 해당 달의 마지막 날짜를 가져옵니다.
        val lastDayOfMonth = getLastDayOfMonth(currentYear, currentMonth)

        // CalItem 객체들을 생성하여 리스트에 추가합니다.
        val calItems = ArrayList<CalItem>()
        for (i in 1..lastDayOfMonth) {
            calItems.add(CalItem(i, null)) // 초기에는 sentiment를 null로 설정
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

        // 각 날짜에 대한 room_sent 데이터를 가져오고 업데이트합니다.
        for (i in 1..lastDayOfMonth) {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.YEAR, currentYear)
            calendar.set(Calendar.MONTH, currentMonth)
            calendar.set(Calendar.DAY_OF_MONTH, i)
            val formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

            // API 호출
            RetrofitApi.getRetrofitService.getRoomDate(formattedDate, cursorId = 0, limit=10).enqueue(object : Callback<RoomListResponse> {
                override fun onResponse(call: Call<RoomListResponse>, response: Response<RoomListResponse>) {
                    if (response.isSuccessful) {
                        val roomList = response.body()?.data ?: emptyList()
                        val roomSent = roomList.lastOrNull()?.roomSent

                        // CalItem 리스트 업데이트
                        calItems[i - 1] = CalItem(i, roomSent)
                        calAdapter.notifyItemChanged(i - 1) // 어댑터에 변경된 아이템을 알림
                    } else {
                        Log.e("CalendarFragment", "API 호출 실패: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<RoomListResponse>, t: Throwable) {
                    Log.e("CalendarFragment", "API 호출 실패", t)
                }
            })
        }

        // BottomSheet 설정
        bottomSheetBehavior = BottomSheetBehavior.from(binding.emotionTitleLayout)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN // 초기에는 숨김 상태로 설정

        // BottomSheet 토글 버튼 클릭 이벤트 처리
        binding.calTxtLayout.setOnClickListener {
            toggleBottomSheet()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
        // 여기서 API를 호출해서 해당 날짜의 대화 내용을 가져오고, BottomSheet에 표시할 수 있습니다.

        // 예시로 nonTalkLayout을 표시하는 코드
        binding.nonTalkLayout.visibility = View.VISIBLE
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }
}