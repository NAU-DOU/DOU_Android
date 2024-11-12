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
            kakaoLogout()
        }

        return binding.root
    }

    private fun kakaoLogout() {
        val accessToken = "Bearer ${getSavedAccessToken()}"

        if (accessToken != null) {
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
        } else {
            println("로그아웃 요청에 필요한 액세스 토큰이 없습니다.")
        }
    }


    private fun clearTokens() {
        val sharedPref = requireContext().getSharedPreferences("authAccess", Context.MODE_PRIVATE)
        val sharedPrefRefresh = requireContext().getSharedPreferences("authRefresh", Context.MODE_PRIVATE)
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
}
