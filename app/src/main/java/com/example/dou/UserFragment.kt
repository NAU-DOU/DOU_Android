package com.example.dou

import android.content.Context
import android.content.Intent
import android.os.Bundle
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
            performKakaoLogout()
        }

        return binding.root
    }

    private fun performKakaoLogout() {
        val accessToken = getSavedAccessToken()
        if (accessToken != null) {
            val service = RetrofitApi.getRetrofitService
            val call = service.kakaoLogout("Bearer $accessToken")

            call.enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (response.isSuccessful) {
                        println("카카오 로그아웃 성공")
                        clearTokens() // 토큰 삭제
                        moveToLoginScreen()
                    } else {
                        println("카카오 로그아웃 실패: ${response.errorBody()?.string()}")
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    println("네트워크 오류로 로그아웃 실패: ${t.message}")
                }
            })
        } else {
            println("로그인된 토큰이 없습니다.")
        }
    }

    private fun clearTokens() {
        val sharedPref = requireContext().getSharedPreferences("auth", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            remove("ACCESS_TOKEN")
            remove("REFRESH_TOKEN")
            apply()
        }
    }

    private fun moveToLoginScreen() {
        val intent = Intent(requireContext(), LoginActivity::class.java)
        startActivity(intent)
        requireActivity().finish() // Fragment에서 Activity 종료
    }

    private fun getSavedAccessToken(): String? {
        val sharedPref = requireContext().getSharedPreferences("auth", Context.MODE_PRIVATE)
        return sharedPref.getString("ACCESS_TOKEN", null)
    }
}
