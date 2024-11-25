//package com.example.dou
//
//import android.content.Intent
//import android.net.Uri
//import android.os.Bundle
//import android.util.Log
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import androidx.fragment.app.Fragment
//import androidx.lifecycle.lifecycleScope
//import com.example.dou.databinding.FragmentEmoBinding
//import com.github.ybq.android.spinkit.style.ThreeBounce
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import okhttp3.MediaType.Companion.toMediaTypeOrNull
//import okhttp3.MultipartBody
//import okhttp3.OkHttpClient
//import okhttp3.RequestBody.Companion.asRequestBody
//import okhttp3.RequestBody.Companion.toRequestBody
//import retrofit2.Call
//import retrofit2.Callback
//import retrofit2.Response
//import retrofit2.Retrofit
//import retrofit2.converter.gson.GsonConverterFactory
//import java.io.File
//
//class EmoFragment : Fragment() {
//    private lateinit var binding: FragmentEmoBinding
//    private lateinit var naverSpeechService: NaverSpeechService
//
//    private val apiKey: String by lazy {
//        BuildConfig.STT_API_KEY
//    }
//
//    private val invokeUrl: String by lazy {
//        BuildConfig.INVOKE_URL
//    }
//
//    private val clientId : String by lazy {
//        BuildConfig.STT_CLIENT_ID
//    }
//
//    private val clientSecret : String by lazy {
//        BuildConfig.STT_CLIENT_SECRET
//    }
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View? {
//        binding = FragmentEmoBinding.inflate(inflater, container, false)
//
//        val progressBar = binding.spinKit
//        val threeBounce = ThreeBounce()
//        progressBar.setIndeterminateDrawable(threeBounce)
//
//        return binding.root
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        // Retrofit 빌더 설정
//        val retrofit = Retrofit.Builder()
//            .baseUrl("$invokeUrl/")
//            .addConverterFactory(GsonConverterFactory.create())
//            .client(OkHttpClient.Builder().build())
//            .build()
//
//        naverSpeechService = retrofit.create(NaverSpeechService::class.java)
//
//        val fileUri = arguments?.getString("fileUri")
//        if (fileUri != null) {
//            Log.d("FileUri", "fileUri가 존재합니다: $fileUri")
//
//            val audioFile = File(Uri.parse(fileUri).path!!)
//            if (audioFile.exists()) {
//                recognizeSpeechFromFile(audioFile)
//            } else {
//                Log.e("FileUri", "파일이 존재하지 않습니다: ${audioFile.path}")
//            }
//        } else {
//            Log.d("FileUri", "fileUri가 존재하지 않습니다.")
//        }
//    }
//
//    private fun recognizeSpeechFromContent(contentUri: Uri) {
//        lifecycleScope.launch(Dispatchers.IO) {
//            try {
//                // Content URI에서 파일 가져오기
//                val inputStream = contentResolver.openInputStream(contentUri)
//                    ?: throw IOException("InputStream을 열 수 없습니다: $contentUri")
//
//                // 파일을 바이트 배열로 변환
//                val byteArray = inputStream.use { it.readBytes() }
//
//                // JSON 데이터 생성
//                val paramsJson = """
//            {
//                "language": "ko-KR",
//                "completion": "sync"
//            }
//            """.trimIndent()
//
//                // 요청 Body 생성 (JSON + 파일 바이너리)
//                val requestBody = MultipartBody.Builder()
//                    .setType(MultipartBody.FORM)
//                    .addFormDataPart("params", null, paramsJson.toRequestBody("application/json".toMediaTypeOrNull()))
//                    .addFormDataPart("media", "audio.mp3", byteArray.toRequestBody("application/octet-stream".toMediaTypeOrNull()))
//                    .build()
//
//                // Retrofit 호출
//                val call = naverSpeechService.recognizeSpeech(requestBody, clientId, clientSecret)
//                call.enqueue(object : Callback<SpeechResponse> {
//                    override fun onResponse(call: Call<SpeechResponse>, response: Response<SpeechResponse>) {
//                        if (response.isSuccessful) {
//                            val responseBody = response.body()
//                            Log.d("STTResponse", "응답: ${responseBody?.text}")
//                        } else {
//                            val errorBody = response.errorBody()?.string()
//                            Log.e("STTResponseError", "오류: ${response.code()} - $errorBody")
//                        }
//                    }
//
//                    override fun onFailure(call: Call<SpeechResponse>, t: Throwable) {
//                        Log.e("STTRequestFailure", "요청 실패", t)
//                    }
//                })
//            } catch (e: Exception) {
//                Log.e("ContentUriProcessingError", "오류 발생: $contentUri", e)
//            }
//        }
//    }
//
//
//    private fun handleResponse(sentences: String?, originalSentences: String?) {
//        if (sentences != null && originalSentences != null) {
//            val intent = Intent(requireContext(), SentenceActivity::class.java).apply {
//                putExtra("sentences", sentences)
//                putExtra("originalSentences", originalSentences)
//            }
//            requireActivity().startActivity(intent)
//        }
//    }
//}