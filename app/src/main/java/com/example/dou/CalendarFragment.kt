package com.example.dou

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dou.databinding.FragmentCalendarBinding
import com.example.dou.databinding.FragmentListBinding
import java.util.Calendar

class CalendarFragment : Fragment() {
    private lateinit var binding: FragmentCalendarBinding
    //private val calItem = arrayListOf<CalItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
            Log.d("CalendarFragment", "Clicked item position: $position")
        }

        binding.calRecycler.layoutManager = GridLayoutManager(context, 7)
        binding.calRecycler.adapter = calAdapter

        return binding.root
    }
    fun getLastDayOfMonth(year: Int, month: Int): Int {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.MONTH, month)
        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    }
}