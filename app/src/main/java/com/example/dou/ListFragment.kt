package com.example.dou

import android.animation.Animator
import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



        val douImageView = binding.listDou

        // 애니메이션 설정
        startBouncingAndFlippingAnimation(douImageView)
//        val animator = AnimatorInflater.loadAnimator(requireContext(), R.anim.bouce) as AnimatorSet
//        animator.setTarget(douImageView)
//        animator.start()
    }

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
            // roomId를 RecordActivity로 전달
            val intent = Intent(requireContext(), RecordActivity::class.java).apply {
                putExtra("roomId", roomId)
            }
            startActivity(intent)
        }

        binding.listRecycler.layoutManager = LinearLayoutManager(context)
        binding.listRecycler.adapter = listAdapter
    }

    private fun getUserId(): Int {
        val sharedPref = requireContext().getSharedPreferences("userData", Context.MODE_PRIVATE)
        return sharedPref.getInt("USER_ID", -1) // 기본값 -1
    }

    // 방 리스트를 서버에서 가져오기
    private fun fetchRoomList() {
        val userId = getUserId()
        if (userId != -1) {
            Log.d("UserData", "User ID: $userId")
        } else {
            Log.d("UserData", "No User ID found in SharedPreferences")
        }

        val service = RetrofitApi.getRetrofitService  // Retrofit 인스턴스 가져오기
        val call = service.getAllRooms(
            userId  = userId,
            cursorId = 0,
            limit = 10
        )

        call.enqueue(object : Callback<RoomListResponse> {
            override fun onResponse(call: Call<RoomListResponse>, response: Response<RoomListResponse>) {
                if (response.isSuccessful) {
                    val roomListResponse = response.body()
                    roomListResponse?.let {
                        updateRecyclerView(it.data)
                        Log.d( "ListFragment", "방리스트 가져오기 성공, ${it.data}")
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
        roomList.forEach { room ->
            // room_id를 사용하여 ListItem 생성
            val newListItem = ListItem(
                roomId = room.roomId,         // room_id를 roomId로 사용
                listCnt = "#${room.roomId}",
                listTxt = room.roomDate
            //.substring(0, 10)  // 날짜만 추출
            )
            listItem.add(newListItem)
        }
        listAdapter.notifyDataSetChanged()  // 어댑터에 변경 사항을 알려줌
    }

    @SuppressLint("ResourceType")
    private fun startBouncingAndFlippingAnimation(imageView: ImageView) {
        val distance = 300f // 이동 거리

        // 왼쪽으로 상대적 이동 애니메이션
        val moveLeft = ObjectAnimator.ofFloat(imageView, "translationX", 0f, -distance).apply {
            duration = 3000
        }

        // 오른쪽으로 상대적 이동 애니메이션
        val moveRight = ObjectAnimator.ofFloat(imageView, "translationX", -distance, 0f).apply {
            duration = 3000
        }

        // 통통 튀는 애니메이션 (Y축)
        val bounce = ObjectAnimator.ofFloat(imageView, "translationY", 0f, -10f, 0f).apply {
            duration = 500
            repeatCount = 6 // 이동 중 6번 튀기
            repeatMode = ObjectAnimator.RESTART
        }

        // 이미지 뒤집기 (왼쪽 -> 오른쪽)
        val flipToRight = ObjectAnimator.ofFloat(imageView, "scaleX", -1f).apply {
            duration = 0 // 즉시 실행
        }

        // 이미지 뒤집기 (오른쪽 -> 왼쪽)
        val flipToLeft = ObjectAnimator.ofFloat(imageView, "scaleX", 1f).apply {
            duration = 0 // 즉시 실행
        }

        // 왼쪽 이동 + 통통 튀기
        val leftSet = AnimatorSet().apply {
            playTogether(moveLeft, bounce.clone()) // 이동 중에 튀기
        }

        // 오른쪽 이동 + 통통 튀기
        val rightSet = AnimatorSet().apply {
            playTogether(moveRight, bounce.clone()) // 이동 중에 튀기
        }

        // 애니메이션 순서 정의
        val animatorSet = AnimatorSet().apply {
            playSequentially(
                leftSet,       // 왼쪽으로 이동
                flipToRight,   // 이미지 뒤집기 (오른쪽 보기)
                rightSet,      // 오른쪽으로 이동
                flipToLeft     // 이미지 뒤집기 (왼쪽 보기)
            )
        }

        // 애니메이션 무한 반복
        animatorSet.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                animatorSet.start() // 애니메이션 다시 시작
            }
        })

        animatorSet.start()
    }
}