package com.example.dou

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.dou.databinding.ActivityRecordBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RecordActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRecordBinding
    private var roomId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRecordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Retrieve roomId from intent
        roomId = intent.getIntExtra("roomId", -1)

        // Fetch records from the server and setup ViewPager
        fetchRecordsByRoomId(roomId)
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
                        // Set up ViewPager after getting the data
                        setupViewPager(it)
                    }
                } else {
                    Log.e("RecordActivity", "Failed to retrieve records: ${response.code()} - ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<RecordGetResponse>, t: Throwable) {
                Log.e("RecordActivity", "Failed to request records", t)
            }
        })
    }

    private fun setupViewPager(recordList: List<RecordGetData>) {
        // Set up ViewPager with ViewPagerAdapter
        val viewPagerAdapter = ViewPagerAdapter(this, recordList)
        binding.viewPager.adapter = viewPagerAdapter

        // Connect DotsIndicator with ViewPager
        binding.dotsIndicator.attachTo(binding.viewPager)
    }
}
