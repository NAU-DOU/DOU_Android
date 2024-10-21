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
            calItems.add(CalItem(i, null)) // 초기에는 sentiment를 null로 설정
        }

        // RecyclerView에 어댑터 설정
        val calAdapter = CalAdapter(calItems) { position ->
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.YEAR, currentYear)
            calendar.set(Calendar.MONTH, currentMonth)
            calendar.set(Calendar.DAY_OF_MONTH, position + 1)

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val formattedDate = dateFormat.format(calendar.time)
            val sentiment = calItems[position].sentiment

            // Pass both the formattedDate and sentiment to the bottom sheet
            openBottomSheet(formattedDate, sentiment)
        }

        binding.calRecycler.layoutManager = GridLayoutManager(context, 7)
        binding.calRecycler.adapter = calAdapter

        for (i in 1..lastDayOfMonth) {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.YEAR, currentYear)
            calendar.set(Calendar.MONTH, currentMonth)
            calendar.set(Calendar.DAY_OF_MONTH, i)
            val formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

            RetrofitApi.getRetrofitService.getRoomDate(formattedDate, cursorId = 0, limit=10).enqueue(object : Callback<RoomListResponse> {
                override fun onResponse(call: Call<RoomListResponse>, response: Response<RoomListResponse>) {
                    if (response.isSuccessful) {
                        val roomList = response.body()?.data ?: emptyList()
                        val roomSent = roomList.lastOrNull()?.roomSent

                        // Update CalItem list
                        calItems[i - 1] = CalItem(i, roomSent)
                        calAdapter.notifyItemChanged(i - 1)
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
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

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

    private fun toggleBottomSheet() {
        if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_HIDDEN) {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        } else {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }
    }

    // Updated openBottomSheet to accept the sentiment value
    private fun openBottomSheet(formattedDate: String, sentiment: Int?) {
        // Display the selected date in the BottomSheet (you can customize how you want to show this)
        //binding.selectedDate.text = formattedDate

        // Map sentiment to a user-friendly string
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

        // android:text="이 날은 나와 놀지 않았어"
        // Set sentiment in the BottomSheet
        binding.douTxt.text = "이 날의 감정은 ${sentimentText}"


        // Set different messages for douTxt2 based on the sentiment
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

        // Set douTxt2's text based on the sentiment
        binding.douTxt2.text = douTxt2Message
        //android:text="더 자주 대화하자!"

        // Change the image based on the sentiment
        val imageResource = when (sentiment) {
            0 -> R.drawable.ic_happy // Replace with the actual resource ID for happiness
            1 -> R.drawable.home_dou // Replace with the actual resource ID for surprise
            2 -> R.drawable.ic_neutral // Replace with the actual resource ID for neutral
            3 -> R.drawable.ic_sad // Replace with the actual resource ID for sadness
            4 -> R.drawable.ic_hmm // Replace with the actual resource ID for discomfort
            5 -> R.drawable.ic_bad // Replace with the actual resource ID for anger
            6 -> R.drawable.ic_bad // Replace with the actual resource ID for fear
            else -> R.drawable.home_dou // Default image for undefined sentiment
        }

        // Set the image resource to the ImageView in the BottomSheet
        binding.calDou.setImageResource(imageResource)

        // If no chat is found, you can show a message
        if (sentiment == null) {
            binding.nonTalkLayout.visibility = View.VISIBLE
        } else {
            binding.nonTalkLayout.visibility = View.VISIBLE
        }

        // Expand the BottomSheet
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }
}
