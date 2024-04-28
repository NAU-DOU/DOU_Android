package com.example.dou

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.example.dou.databinding.FragmentEmoBinding
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.speech.v1.LongRunningRecognizeMetadata
import com.google.cloud.speech.v1.RecognitionAudio
import com.google.cloud.speech.v1.RecognitionConfig
import com.google.cloud.speech.v1.RecognizeRequest
import com.google.cloud.speech.v1.SpeechClient
import com.google.cloud.speech.v1.SpeechSettings
import com.google.protobuf.ByteString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class EmoFragment : Fragment() {
    private lateinit var binding: FragmentEmoBinding
    private lateinit var speechClient: SpeechClient

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentEmoBinding.inflate(inflater, container, false)
        binding.emoTxt.text = "변환 중..."
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fileUri = arguments?.getString("fileUri")
        if (fileUri != null) {
            Log.d("FileUri", "fileUri가 존재합니다: $fileUri")
            initializeSpeechClient(Uri.parse(fileUri))
        } else {
            Log.d("FileUri", "fileUri가 존재하지 않습니다.")
        }
    }

    private fun initializeSpeechClient(audioUri: Uri) {
        GlobalScope.launch(Dispatchers.IO) {
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

                // 오디오 파일에서 음성 데이터를 가져와 ByteString으로 변환
                val audioData = readAudioFile(audioUri)

                Log.d("AudioData", "오디오 파일에서 음성 데이터 가져오기 완료")

                // RecognitionConfig 설정
                val config = RecognitionConfig.newBuilder()
                    .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                    .setSampleRateHertz(16000)
                    .setLanguageCode("ko-KR")
                    .build()

                Log.d("RecognitionConfig", "RecognitionConfig 설정 완료")

                // RecognitionAudio 설정
                val audio = RecognitionAudio.newBuilder()
                    .setContent(audioData)
                    .build()

                Log.d("RecognitionAudio", "RecognitionAudio 설정 완료")

                // RecognitionRequest 설정
                val request = RecognizeRequest.newBuilder()
                    .setConfig(config)
                    .setAudio(audio)
                    .build()

                Log.d("RecognitionRequest", "RecognitionRequest 설정 완료")

                // Speech-to-Text 변환 요청 보내기
                val response = speechClient.recognize(request)

                Log.d("RecognitionRequest", "Speech-to-Text 변환 요청 보내기 완료")

                // 변환된 텍스트 처리
                val resultText = response.resultsList.joinToString("\n") { result ->
                    result.alternativesList.joinToString("\n") { alternative ->
                        alternative.transcript
                    }
                }
                Log.d("변환한 내용", resultText)

                // UI 업데이트
                launch(Dispatchers.Main) {
                    // 변환 완료 메시지 토스트 표시
                    Toast.makeText(requireContext(), "변환 완료", Toast.LENGTH_SHORT).show()

                    // 변환된 텍스트 표시
                    binding.emoTxt.text = resultText
                }

                // 클라이언트 종료
                speechClient.close()

                Log.d("SpeechClient", "Speech-to-Text 클라이언트 종료")
            } catch (e: Exception) {
                Log.e("RecognitionRequest", "Speech-to-Text 변환 요청 중 오류 발생", e)
            }
        }
    }

    private fun readAudioFile(audioUri: Uri): ByteString {
        requireContext().contentResolver.openInputStream(audioUri)?.use { inputStream ->
            return ByteString.readFrom(inputStream)
        } ?: throw IllegalStateException("InputStream이 null입니다.")
    }
}