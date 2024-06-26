package com.example.dou

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.dou.databinding.FragmentEmoBinding
import com.github.ybq.android.spinkit.style.ThreeBounce
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File

class EmoFragment : Fragment() {
    private lateinit var binding: FragmentEmoBinding
    private lateinit var naverSpeechService: NaverSpeechService

    private val apiKey: String by lazy {
        BuildConfig.STT_API_KEY
    }

    private val invokeUrl: String by lazy {
        BuildConfig.INVOKE_URL
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentEmoBinding.inflate(inflater, container, false)

        val progressBar = binding.spinKit
        val threeBounce = ThreeBounce()
        progressBar.setIndeterminateDrawable(threeBounce)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Retrofit 빌더 설정
        val retrofit = Retrofit.Builder()
            .baseUrl("$invokeUrl/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(OkHttpClient.Builder().build())
            .build()

        naverSpeechService = retrofit.create(NaverSpeechService::class.java)

        val fileUri = arguments?.getString("fileUri")
        if (fileUri != null) {
            Log.d("FileUri", "fileUri가 존재합니다: $fileUri")

            val audioFile = File(Uri.parse(fileUri).path!!)
            if (audioFile.exists()) {
                recognizeSpeechFromFile(audioFile)
            } else {
                Log.e("FileUri", "파일이 존재하지 않습니다: ${audioFile.path}")
            }
        } else {
            Log.d("FileUri", "fileUri가 존재하지 않습니다.")
        }
    }

    private fun recognizeSpeechFromFile(audioFile: File) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val requestFile = audioFile.asRequestBody("application/octet-stream".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("media", audioFile.name, requestFile)

                val paramsJson = """
                    {
                        "language": "ko-KR",
                        "completion": "sync"
                    }
                """.trimIndent()
                val params = paramsJson.toRequestBody("application/json".toMediaTypeOrNull())

                val call = naverSpeechService.recognizeSpeech(body, params, apiKey)
                call.enqueue(object : Callback<SpeechResponse> {
                    override fun onFailure(call: Call<SpeechResponse>, t: Throwable) {
                        Log.e("NaverSTT", "Failed to recognize speech", t)
                    }

                    override fun onResponse(call: Call<SpeechResponse>, response: Response<SpeechResponse>) {
                        if (response.isSuccessful) {
                            val responseBody = response.body()
                            val originalSentences = responseBody?.text ?: ""
                            val sentences = originalSentences.replace(".", ".\n")
                            Log.d("NaverSTT", "Response: $sentences")
                            handleResponse(sentences, originalSentences)
                        } else {
                            val errorBody = response.errorBody()?.string()
                            Log.e("NaverSTT", "Error response: ${response.code()} - $errorBody")
                        }
                    }
                })
            } catch (e: Exception) {
                Log.e("UploadAudio", "오디오 파일 업로드 중 오류 발생", e)
            }
        }
    }

    private fun handleResponse(sentences: String?, originalSentences: String?) {
        if (sentences != null && originalSentences != null) {
            val intent = Intent(requireContext(), SentenceActivity::class.java).apply {
                putExtra("sentences", sentences)
                putExtra("originalSentences", originalSentences)
            }
            requireActivity().startActivity(intent)
        }
    }
}