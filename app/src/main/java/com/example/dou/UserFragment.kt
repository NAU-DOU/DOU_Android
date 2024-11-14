package com.example.dou

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.dou.databinding.FragmentUserBinding
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UserFragment : Fragment() {
    private lateinit var binding: FragmentUserBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentUserBinding.inflate(inflater, container, false)

        binding.logoutBtn.setOnClickListener {
            kakaoLogout()
        }

        binding.userName.text= "${getUserData()}"
        return binding.root
    }

    private fun kakaoLogout() {
        val accessToken = "Bearer ${getSavedAccessToken()}"
        Log.d("로그아웃  액세스 토큰", "${accessToken}")
        val service = RetrofitApi.getRetrofitService

        val call = service.kakaoLogout(accessToken)

        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    println("로그아웃 성공")
                    clearTokens()
                    moveToLoginScreen()
                } else {
                    println("카카오 로그아웃 실패: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                println("네트워크 오류: ${t.message}")
            }
        })
    }


    private fun clearTokens() {
        val sharedPref = requireContext().getSharedPreferences("authAccess", Context.MODE_PRIVATE)
        val sharedPrefRefresh = requireContext().getSharedPreferences("authRefresh", Context.MODE_PRIVATE)
        val shared = requireContext().getSharedPreferences("userData" , Context.MODE_PRIVATE)

        with(shared.edit()){
            remove("USER_NICKNAME")
            remove("USER_ID")
            commit()
        }

        with(sharedPref.edit()) {
            remove("ACCESS_TOKEN")
            commit()
        }

        with(sharedPrefRefresh.edit()) {
            remove("REFRESH_TOKEN")
            commit()
        }
    }

    private fun moveToLoginScreen() {
        val intent = Intent(requireContext(), LoginActivity::class.java)
        startActivity(intent)
        requireActivity().finish() // Fragment에서 Activity 종료
    }

    private fun getSavedAccessToken(): String? {
        val sharedPref = requireContext().getSharedPreferences("authAccess", Context.MODE_PRIVATE)
        return sharedPref.getString("ACCESS_TOKEN", null)
    }

    private fun getUserData(): String? {
        val sharedPref = requireContext().getSharedPreferences("userData", Context.MODE_PRIVATE)
        //val userId = sharedPref.getInt("USER_ID", -1) // 기본값 -1
        val userNickname = sharedPref.getString("USER_NICKNAME", null)

        // userId와 userNickname을 JSON 형식의 문자열로 반환
        return userNickname
    }

}
