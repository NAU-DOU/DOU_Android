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
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.speech.v1.LongRunningRecognizeRequest
import com.google.cloud.speech.v1.LongRunningRecognizeResponse
import com.google.cloud.speech.v1.RecognitionAudio
import com.google.cloud.speech.v1.RecognitionConfig
import com.google.cloud.speech.v1.SpeechClient
import com.google.cloud.speech.v1.SpeechSettings
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.StorageOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EmoFragment : Fragment() {

    private lateinit var binding: FragmentEmoBinding
    private lateinit var speechClient: SpeechClient

    private val bucketName = "nau_dou"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentEmoBinding.inflate(inflater, container, false)

        val progressBar = binding.spinKit
        val threeBounce = ThreeBounce()
        progressBar.setIndeterminateDrawable(threeBounce)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fileUri = arguments?.getString("fileUri")
        if (fileUri != null) {
            Log.d("FileUri", "fileUri가 존재합니다: $fileUri")

            uploadAudioToBucket(Uri.parse(fileUri))
        } else {
            Log.d("FileUri", "fileUri가 존재하지 않습니다.")
        }
    }

    private fun uploadAudioToBucket(audioUri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 오디오 파일을 읽고 Byte 배열로 변환
                val audioData = readAudioFile(audioUri)
                Log.d("오디오 데이터", audioData.joinToString(", "))

                // Google Cloud Storage 클라이언트 초기화
                val credentials = GoogleCredentials.fromStream(
                    requireContext().resources.openRawResource(R.raw.naudou)
                )
                val storage = StorageOptions.newBuilder().setCredentials(credentials).build().service

                val blobInfo = BlobInfo.newBuilder(bucketName, "audio_file.flac")
                    .setContentType("audio/flac")
                    .build()

                // 버킷에 오디오 파일 업로드
                storage.create(blobInfo, audioData)

                // 버킷에 업로드된 오디오 파일의 URI 생성
                val audioUriForSpeechToText = "gs://$bucketName/audio_file.flac"

                Log.d("AudioURI", "오디오 파일이 버킷에 업로드되었습니다. URI: $audioUriForSpeechToText")

                // Speech-to-Text 클라이언트 초기화 및 변환 요청
                initializeSpeechClient(audioUriForSpeechToText)
            } catch (e: Exception) {
                Log.e("UploadAudio", "오디오 파일 업로드 중 오류 발생", e)
            }
        }
    }

    private fun initializeSpeechClient(audioUri: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Speech-to-Text 클라이언트 초기화
                val credentials = GoogleCredentials.fromStream(
                    requireContext().resources.openRawResource(R.raw.naudou)
                )
                val settings = SpeechSettings.newBuilder()
                    .setCredentialsProvider { credentials }
                    .build()
                speechClient = SpeechClient.create(settings)

                Log.d("SpeechClient", "Speech-to-Text 클라이언트 초기화 완료")

                // RecognitionConfig 설정
                val config = RecognitionConfig.newBuilder()
                    .setEncoding(RecognitionConfig.AudioEncoding.FLAC)
                    .setSampleRateHertz(8000)
                    .setLanguageCode("ko-KR")
                    .setAudioChannelCount(1)
                    .setEnableAutomaticPunctuation(true)
                    .build()

                Log.d("RecognitionConfig", "RecognitionConfig 설정 완료")

                // RecognitionAudio 설정
                val audio = RecognitionAudio.newBuilder()
                    .setUri(audioUri)
                    .build()

                Log.d("RecognitionAudio", "RecognitionAudio 설정 완료")
                Log.d("오디오 요청", audio.toString())

                // LongRunningRecognizeRequest 설정
                val request = LongRunningRecognizeRequest.newBuilder()
                    .setConfig(config)
                    .setAudio(audio)
                    .build()

                Log.d("RecognitionRequest", "RecognitionRequest 설정 완료")
                Log.d("요청 정보", request.toString())

                val response = speechClient.longRunningRecognizeAsync(request).get()

                Log.d("Response", "아직 변환 중...")

                Log.d("while문 탈출", "response.done이 드디어 true?")
                handleResponse(response)

            } catch (e: Exception) {
                Log.e("RecognitionRequest", "Speech-to-Text 변환 요청 중 오류 발생", e)
            } finally {
                // 클라이언트 종료
                speechClient.close()
//                requireActivity().runOnUiThread {
//                    findNavController().navigate(R.id.action_emoFragment2_to_chatActivity)
//                }

                Log.d("SpeechClient", "Speech-to-Text 클라이언트 종료")
            }
        }
    }

    private fun handleResponse(response: LongRunningRecognizeResponse) {
        // 결과 리스트를 가져옵니다.
        val resultsList = response.resultsList

        // 결과가 비어 있는지 확인합니다.
        if (resultsList.isEmpty()) {
            Log.d("handleResponse", "No results found")
            return
        }

        // 문장들을 저장할 변수 생성
        var sentences = ""
        var originalSentences = ""

        // 결과가 있을 경우 각 결과를 처리합니다.
        for (result in resultsList) {
            // 결과의 alternative들을 가져와서 출력합니다.
            val alternativesList = result.alternativesList
            for (alternative in alternativesList) {
                // 텍스트 추출
                val transcript = alternative.transcript

                // 원본 텍스트를 originalSentences에 추가합니다.
                originalSentences += transcript

                // 각 문장 끝에 \n을 붙여서 sentences 변수에 추가합니다.
                val formattedTranscript = transcript.replace(".", ".\n")
                sentences += formattedTranscript

                // UI에 텍스트 표시 또는 다른 처리 수행
                Log.d("handleResponse", "Transcript: $transcript")
                Log.d("문장 모음", "$sentences")
            }
        }

        // gpt STT된 내용 전체 요약
        //summaryEmotion(originalSentences) // 원본 텍스트를 전달
        //Log.d("originalSentences", "$originalSentences")

        val intent = Intent(requireContext(), SentenceActivity::class.java).apply {
            putExtra("sentences", sentences)
            putExtra("originalSentences", originalSentences)
        }
        requireActivity().startActivity(intent)


        // 감정 분석 요약
        //analyzeEmotion(sentences)

    }


    private fun readAudioFile(audioUri: Uri): ByteArray {
        requireContext().contentResolver.openInputStream(audioUri)?.use { inputStream ->
            return inputStream.readBytes()
        } ?: throw IllegalStateException("InputStream이 null입니다.")
    }

//    private fun analyzeEmotion(sentence: String) {
//        val request = EmotionRequest(userId = 0, sentence = sentence)
//        Log.d("EmotionRequest", "Request: $request")
//        val service = RetrofitApi.getRetrofitService
//        val call = service.emotion(request)
//
//        call.enqueue(object : Callback<EmotionResponse> {
//            override fun onResponse(
//                call: Call<EmotionResponse>,
//                response: Response<EmotionResponse>
//            ) {
//                if (response.isSuccessful) {
//                    val emotionResponse = response.body()
//                    if (emotionResponse != null) {
//
//                        val response = emotionResponse.data
//                        Log.d("감정 분석 응답", "$response")
//                        // Gson을 사용하여 EmotionResponse 객체를 JSON 문자열로 변환
//                        val gson = Gson()
//                        val emotionResponseJson = gson.toJson(emotionResponse)
//                        Log.d("emotionResponseJson", "$emotionResponseJson")
//
//                        // Intent로 JSON 문자열 전달
//                        val intent = Intent(requireContext(), ChatActivity::class.java).apply {
//                            putExtra("emotionResponseJson", emotionResponseJson)
//
//                        }
//                        requireActivity().startActivity(intent)
//                    }
//                } else {
//                    Log.e("EmotionAnalyzer", "API 호출 실패: ${response.code()} - ${response.errorBody()?.string()}")
//                }
//            }
//
//            override fun onFailure(call: Call<EmotionResponse>, t: Throwable) {
//                Log.e("EmotionAnalyzer", "API 호출 실패", t)
//            }
//        })
//    }

//    private fun summaryEmotion(context: String) {
//        val request = SummaryRequest(userId = 0, context = context)
//        Log.d("SummaryRequest", "Request: $request")
//        val service = RetrofitApi.getRetrofitService
//        val call = service.summary(request)
//
//        call.enqueue(object : Callback<SummaryResponse> {
//            override fun onResponse(
//                call: Call<SummaryResponse>,
//                response: Response<SummaryResponse>
//            ) {
//                if (response.isSuccessful) {
//                    val summaryResponse = response.body()
//                    if (summaryResponse != null) {
//
//                        val response = summaryResponse.data
//                        Log.d("SummaryResponse", "Summary: ${response.response}")
//
//
//
//                    }
//                } else {
//                    Log.e("SummaryAPI", "API 호출 실패: ${response.code()} - ${response.errorBody()?.string()}")
//                }
//            }
//
//            override fun onFailure(call: Call<SummaryResponse>, t: Throwable) {
//                Log.e("SummaryAPI", "API 호출 실패", t)
//            }
//        })
//    }

}