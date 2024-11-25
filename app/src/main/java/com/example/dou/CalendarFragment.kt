package com.example.dou

import android.content.Context
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

        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)

        val lastDayOfMonth = getLastDayOfMonth(currentYear, currentMonth)

        val calItems = ArrayList<CalItem>()
        for (i in 1..lastDayOfMonth) {
            calItems.add(CalItem(i, null)) // Initialize with null sentiment
        }

        // RecyclerView Adapter for Calendar
        val calAdapter = CalAdapter(calItems) { position ->
            val selectedSentiment = calItems[position].sentiment
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.YEAR, currentYear)
            calendar.set(Calendar.MONTH, currentMonth)
            calendar.set(Calendar.DAY_OF_MONTH, position + 1)

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val formattedDate = dateFormat.format(calendar.time)

            // Pass both the formattedDate and sentiment to the bottom sheet
            openBottomSheet(formattedDate, selectedSentiment)
        }

        binding.calRecycler.layoutManager = GridLayoutManager(context, 7)
        binding.calRecycler.adapter = calAdapter

        // Fetch data for the calendar
        fetchRoomData(calItems, calAdapter)

        // BottomSheet setup
        bottomSheetBehavior = BottomSheetBehavior.from(binding.emotionTitleLayout)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        binding.calTxtLayout.setOnClickListener {
            toggleBottomSheet()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun getLastDayOfMonth(year: Int, month: Int): Int {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.MONTH, month)
        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    private fun toggleBottomSheet() {
        if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_HIDDEN) {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        } else {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }
    }

    private fun fetchRoomData(calItems: ArrayList<CalItem>, calAdapter: CalAdapter) {
        val userId = getUserId()
        val limit = 1000 // Maximum number of days in a month

        RetrofitApi.getRetrofitService.getAllRooms(userId = userId, cursorId = 0, limit = limit)
            .enqueue(object : Callback<RoomListResponse> {
                override fun onResponse(call: Call<RoomListResponse>, response: Response<RoomListResponse>) {
                    if (response.isSuccessful) {
                        val roomList = response.body()?.data ?: emptyList()

                        // Log full API response for debugging
                        Log.d("CalendarFragment", "API Response: $roomList")

                        // Map room data to calendar items
                        roomList.forEach { room ->
                            val day = room.roomDate.split(".")[2].toIntOrNull() ?: return@forEach
                            if (day in 1..calItems.size) {
                                calItems[day - 1] = CalItem(day, room.roomSent)
                                Log.d("CalendarFragment", "Mapped day $day with roomSent: ${room.roomSent}")
                            }
                        }
                        calAdapter.notifyDataSetChanged()
                    } else {
                        Log.e("CalendarFragment", "API 호출 실패: ${response.code()} - ${response.errorBody()?.string()}")
                    }
                }

                override fun onFailure(call: Call<RoomListResponse>, t: Throwable) {
                    Log.e("CalendarFragment", "API 호출 실패", t)
                }
            })
    }

    private fun openBottomSheet(formattedDate: String, sentiment: Int?) {
        // Log the selected date and sentiment
        Log.d("CalendarFragment", "Selected Date: $formattedDate, Sentiment: $sentiment")

        val sentimentText = when (sentiment) {
            0 -> "행복이야"
            1 -> "놀람이야"
            2 -> "중립이야"
            3 -> "슬픔이야"
            4 -> "꺼림이야"
            5 -> "분노야"
            6 -> "두려움이야"
            else -> "알 수 없어"
        }

        binding.douTxt.text = "이 날의 감정은 ${sentimentText}"

        val douTxt2Message = when (sentiment) {
            0 -> "너 정말 기뻤구나! 자주 이야기 나누자!"
            1 -> "뭔가 놀랐구나! 무슨 일이 있었어?"
            2 -> "이 날은 무난한 하루였네, 그래도 대화 더 자주하자!"
            3 -> "너 많이 슬펐구나. 힘들었겠다. 이야기 나누고 싶을 때 언제든 말해."
            4 -> "좀 힘들었나봐. 무슨 생각을 했어?"
            5 -> "너 정말 화가 났구나. 다음엔 더 편안한 대화를 나눌 수 있길 바라!"
            6 -> "두려움이 있었네. 괜찮아, 네가 행복하길 바랄게."
            else -> "더 자주 대화하자!"
        }

        binding.douTxt2.text = douTxt2Message

        val imageResource = when (sentiment) {
            0 -> R.drawable.ic_happy
            1 -> R.drawable.home_dou
            2 -> R.drawable.ic_neutral
            3 -> R.drawable.ic_sad
            4 -> R.drawable.ic_hmm
            5 -> R.drawable.ic_bad
            6 -> R.drawable.ic_bad
            else -> R.drawable.home_dou
        }

        binding.calDou.setImageResource(imageResource)

        if (sentiment == null) {
            binding.nonTalkLayout.visibility = View.VISIBLE
            Log.d("CalendarFragment", "No sentiment data for selected date.")
        } else {
            binding.nonTalkLayout.visibility = View.VISIBLE
        }

        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }


    private fun getUserId(): Int {
        val sharedPref = requireContext().getSharedPreferences("userData", Context.MODE_PRIVATE)
        return sharedPref.getInt("USER_ID", -1)
    }
}
