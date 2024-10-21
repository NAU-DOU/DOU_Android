package com.example.dou

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPagerAdapter(
    activity: AppCompatActivity,
    private val recordList: List<RecordGetData>  // Pass the list of records to the adapter
) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int {
        return recordList.size  // Set the number of pages to the number of records
    }

    override fun createFragment(position: Int): Fragment {
        // Create and return the fragment for each page
        val record = recordList[position]
        return FirstFragment.newInstance(record)
    }
}
