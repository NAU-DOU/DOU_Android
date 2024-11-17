package com.example.dou

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import androidx.fragment.app.Fragment
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

        // Set user name
        binding.userName.text = getUserData()


        fetchUseDate()
        fetchSentCount(1) // Always using 1 for happiness


        // Logout button
        binding.logoutBtn.setOnClickListener {
            kakaoLogout()
        }

        return binding.root
    }

    private fun fetchUseDate() {
        val service = RetrofitApi.getRetrofitService

        service.postUseDate().enqueue(object : Callback<PostUseDateResponse> {
            override fun onResponse(call: Call<PostUseDateResponse>, response: Response<PostUseDateResponse>) {
                if (response.isSuccessful) {
                    val useDate = response.body()?.data?.useDate ?: 0
                    Log.d("UserFragment", "Use date: $useDate")

                    // Update UI with use date
                    binding.dayDou2.text = "${useDate}일"
                } else {
                    Log.e("UserFragment", "Failed to fetch use date: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<PostUseDateResponse>, t: Throwable) {
                Log.e("UserFragment", "Network error while fetching use date: ${t.message}")
            }
        })
    }

    private fun fetchSentCount(sentCode: Int) {
        val service = RetrofitApi.getRetrofitService
        val request = PostSentCountRequest(sentCode)

        service.postSentCount(request).enqueue(object : Callback<PostSentCountResponse> {
            override fun onResponse(call: Call<PostSentCountResponse>, response: Response<PostSentCountResponse>) {
                if (response.isSuccessful) {
                    val useSent = response.body()?.useSent ?: "알 수 없음"
                    val sentCount = response.body()?.sentCount ?: 0
                    Log.d("UserFragment", "UseSent: $useSent, SentCount: $sentCount")

                    // Update UI with sentiment count
                    binding.timeDou2.text = "${sentCount}번"
                } else {
                    Log.e("UserFragment", "Failed to fetch sentiment count: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<PostSentCountResponse>, t: Throwable) {
                Log.e("UserFragment", "Network error while fetching sentiment count: ${t.message}")
            }
        })
    }

    private fun kakaoLogout() {
        val accessToken = "Bearer ${getSavedAccessToken()}"
        Log.d("UserFragment", "Access token for logout: $accessToken")
        val service = RetrofitApi.getRetrofitService

        service.kakaoLogout(accessToken).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    Log.d("UserFragment", "Logout successful")
                    clearTokens()
                    moveToLoginScreen()
                    clearWebViewCookies()
                } else {
                    Log.e("UserFragment", "Logout failed: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("UserFragment", "Network error during logout: ${t.message}")
            }
        })
    }

    private fun clearTokens() {
        val sharedPref = requireContext().getSharedPreferences("authAccess", Context.MODE_PRIVATE)
        val sharedPrefRefresh = requireContext().getSharedPreferences("authRefresh", Context.MODE_PRIVATE)
        val shared = requireContext().getSharedPreferences("userData", Context.MODE_PRIVATE)

        with(shared.edit()) {
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
        requireActivity().finish()
    }

    private fun clearWebViewCookies() {
        val cookieManager = CookieManager.getInstance()
        cookieManager.removeAllCookies(null)
        cookieManager.flush()
    }

    private fun getSavedAccessToken(): String? {
        val sharedPref = requireContext().getSharedPreferences("authAccess", Context.MODE_PRIVATE)
        return sharedPref.getString("ACCESS_TOKEN", null)
    }

    private fun getUserData(): String? {
        val sharedPref = requireContext().getSharedPreferences("userData", Context.MODE_PRIVATE)
        return sharedPref.getString("USER_NICKNAME", "알 수 없는 사용자")
    }
}
