package com.example.dou

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPagerAdapter(
    fragment: Fragment,
    private val recordList: List<RecordGetData>  // 레코드 리스트를 어댑터로 전달
) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int {
        return recordList.size  // 레코드 개수만큼 페이지 수 설정
    }

    override fun createFragment(position: Int): Fragment {
        // 각 페이지에서 데이터를 표시할 Fragment를 생성하여 전달
        val record = recordList[position]
        return FirstFragment.newInstance(record)
    }
}