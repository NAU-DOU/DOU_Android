package com.example.dou

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.dou.databinding.FragmentEmoBinding
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.speech.v1.RecognitionAudio
import com.google.cloud.speech.v1.RecognitionConfig
import com.google.cloud.speech.v1.RecognizeRequest
import com.google.cloud.speech.v1.SpeechClient
import com.google.cloud.speech.v1.SpeechSettings
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.StorageOptions
import com.google.protobuf.ByteString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
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
        binding.emoTxt.text = "변환 중..."
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
        GlobalScope.launch(Dispatchers.IO) {
            try {
                // 오디오 파일을 읽고 Byte 배열로 변환
                val audioData = readAudioFile(audioUri).toByteArray()

                // Google Cloud Storage 클라이언트 초기화
                val credentials = GoogleCredentials.fromStream(
                    requireContext().resources.openRawResource(R.raw.naudou)
                )
                val storage = StorageOptions.newBuilder().setCredentials(credentials).build().service

                // 버킷에 오디오 파일 업로드
                storage.create(BlobInfo.newBuilder(bucketName, "audio_file.wav").build(), audioData)

                // 버킷에 업로드된 오디오 파일의 URI 생성
                val audioUriForSpeechToText = "gs://$bucketName/audio_file.wav"

                Log.d("AudioURI", "오디오 파일이 버킷에 업로드되었습니다. URI: $audioUriForSpeechToText")

                // Speech-to-Text 클라이언트 초기화 및 변환 요청
                initializeSpeechClient(audioUriForSpeechToText)
            } catch (e: Exception) {
                Log.e("UploadAudio", "오디오 파일 업로드 중 오류 발생", e)
            }
        }
    }

    private fun initializeSpeechClient(audioUri: String) {
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

                // RecognitionConfig 설정
                val config = RecognitionConfig.newBuilder()
                    .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                    .setSampleRateHertz(16000)
                    .setLanguageCode("ko-KR")
                    .build()

                Log.d("RecognitionConfig", "RecognitionConfig 설정 완료")

                // RecognitionAudio 설정
                val audio = RecognitionAudio.newBuilder()
                    .setUri(audioUri)
                    .build()

                Log.d("RecognitionAudio", "RecognitionAudio 설정 완료")
                Log.d("오디오 요청", audio.toString())

                // RecognitionRequest 설정
                val request = RecognizeRequest.newBuilder()
                    .setConfig(config)
                    .setAudio(audio)
                    .build()

                Log.d("RecognitionRequest", "RecognitionRequest 설정 완료")
                Log.d("요청 정보", request.toString())

                // Speech-to-Text 변환 요청 보내기
                val response = speechClient.recognize(request)

                Log.d("응답 정보", response.toString())
                Log.d("RecognitionRequest", "Speech-to-Text 변환 요청 보내기 완료")

                // 변환된 텍스트 처리
                val resultText = response.resultsList.joinToString("\n") { result ->
                    result.alternativesList.joinToString("\n") { alternative ->
                        alternative.transcript
                    }
                }
                Log.d("변환한 내용", resultText)

                // 변환된 텍스트 표시
                val finalText = "변환 완료\n$resultText"
                Log.e("변환 과정", finalText)

                // UI 업데이트
                launch(Dispatchers.Main) {
                    // 변환 완료 메시지 토스트 표시
                    Toast.makeText(requireContext(), "변환 완료", Toast.LENGTH_SHORT).show()

                    // 변환된 텍스트 표시
                    binding.emoTxt.text = finalText
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