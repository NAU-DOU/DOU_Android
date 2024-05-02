package com.example.dou

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.dou.databinding.FragmentEmoBinding
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
        binding.emoTxt.text = "변환 중..."
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fileUri = arguments?.getString("fileUri")
        if (fileUri != null) {
            Log.d("FileUri", "fileUri가 존재합니다: $fileUri")

            // 여기 코드가 제대로 작동하는걸 보면 파일자체에 문제는 없어보임. file이 현재 버킷으로 저장되면서 발생하는 오류인 것 같음..
//            val audioTest = readAudioFile(Uri.parse(fileUri))
//            playAudioFromByteString(audioTest)
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

                //playAudioFromCloudStorage(audioUriForSpeechToText)

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
                Log.d("SpeechClient", "Speech-to-Text 클라이언트 종료")
            }
        }
    }

//    private fun playAudioFromByteString(audioData: ByteString) {
//        try {
//            // 바이트 스트림 데이터를 오디오 파일로 저장
//            val audioFile = File(requireContext().cacheDir, "audio_data.wav")
//            FileOutputStream(audioFile).use { outputStream ->
//                audioData.writeTo(outputStream)
//            }
//
//            // 저장된 오디오 파일을 재생
//            val mediaPlayer = MediaPlayer().apply {
//                setDataSource(audioFile.path)
//                prepare()
//                start()
//            }
//
//        } catch (e: Exception) {
//            Log.e("PlayAudio", "오디오 재생 중 오류 발생", e)
//        }
//    }

    private fun handleResponse(response: LongRunningRecognizeResponse) {
        // 결과 리스트를 가져옵니다.
        val resultsList = response.resultsList

        // 결과가 비어 있는지 확인합니다.
        if (resultsList.isEmpty()) {
            Log.d("handleResponse", "No results found")
            return
        }

        // 결과가 있을 경우 각 결과를 처리합니다.
        for (result in resultsList) {
            // 결과의 alternative들을 가져와서 출력합니다.
            val alternativesList = result.alternativesList
            for (alternative in alternativesList) {
                // 텍스트 추출
                val transcript = alternative.transcript
                // UI에 텍스트 표시 또는 다른 처리 수행
                Log.d("handleResponse", "Transcript: $transcript")
            }
        }
    }

    /*private fun readAudioFile(audioUri: Uri): ByteString {
        requireContext().contentResolver.openInputStream(audioUri)?.use { inputStream ->
            return ByteString.readFrom(inputStream)
        } ?: throw IllegalStateException("InputStream이 null입니다.")
    }*/

    private fun readAudioFile(audioUri: Uri): ByteArray {
        requireContext().contentResolver.openInputStream(audioUri)?.use { inputStream ->
            return inputStream.readBytes()
        } ?: throw IllegalStateException("InputStream이 null입니다.")
    }
}