package com.example.dou

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.example.dou.databinding.FragmentHomeBinding
import java.io.IOException
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class HomeFragment : Fragment() {
    private lateinit var binding: FragmentHomeBinding
    private lateinit var speechRecognizer: SpeechRecognizer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    private var isRecording = false

    private val randomTexts = listOf(
        "도우는 오늘 피자 도우를 먹었어\n넌 맛있는 밥 먹었어?",
        "오늘도 수고했어!\n내일 하루도 힘내서 살아보자!",
        "오늘은 기분이 어때?\n나랑 대화해볼래?",
        "먼 훗날이 아니라,\n너가 지금 당장 행복했으면",
        "그 동안 수고했던 너에게\n도우가 토닥토닥 해줄게",
        "바람도 쉬고 햇살도 쉬고 별들도 쉬어,\n너도 힘들면 쉬어도 돼"
    )
    private val sentencesToHighlight = listOf(
        "도우", "수고했어!", "나랑 대화해볼래?",
        "지금 당장 행복했으면", "도우가 토닥토닥 해줄게", "힘들면 쉬어도 돼", "넌 맛있는 밥 먹었어?"
    )
    private val colorCode = "#1fff1b"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentHomeBinding.inflate(inflater, container, false)

        // 랜덤한 텍스트 선택
        val randomIndex = (0 until randomTexts.size).random()
        val randomText = randomTexts[randomIndex]

        // 선택된 텍스트를 강조하여 화면에 표시
        val highlightedText = highlightSentencesInText(randomText, sentencesToHighlight, colorCode)
        binding.homeRandomText.text = highlightedText

        // 권한 설정
        requestPermission()

        // RecognizerIntent 생성
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
            RecognizerIntent.EXTRA_CALLING_PACKAGE,
            requireActivity().packageName
        )    // 여분의 키
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")

        binding.homeRecord.setOnClickListener {
            startRecognition()
        }

        binding.recordFin.setOnClickListener {
            if (isRecording) {
                // 녹음이 진행 중인 경우 녹음을 중지합니다
                speechRecognizer.stopListening()
                isRecording = false
                // UI를 업데이트하거나 필요한 작업을 수행합니다

                binding.recordFin.visibility = View.INVISIBLE
                binding.recordSee.visibility = View.VISIBLE
                binding.recordDesLayout2.visibility = View.VISIBLE
                binding.recordDesLayout1.visibility = View.INVISIBLE

                Toast.makeText(
                    requireContext().applicationContext,
                    "녹음이 중지되었습니다",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

//        binding.recordSee.setOnClickListener {
//            // 음성 인식 결과를 로그로 출력합니다
//            if (resultText.isNotEmpty()) {
//                Log.d("음성 인식 결과", resultText)
//            } else {
//                Log.d("음성 인식 결과", "인식된 내용이 없습니다.")
//            }
//        }

        return binding.root
    }

    // 음성 인식 시작 메서드
    private fun startRecognition() {
        isRecording = true
        binding.recordFin.visibility = View.VISIBLE
        binding.recordDesLayoutFirst.visibility = View.INVISIBLE
        binding.recordDesLayout1.visibility = View.VISIBLE
        // 아직 예외처리중
        binding.recordDesLayout2.visibility = View.INVISIBLE
        binding.recordDesLayout3.visibility = View.INVISIBLE

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
            RecognizerIntent.EXTRA_CALLING_PACKAGE,
            requireActivity().packageName
        )    // 여분의 키
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")         // 언어 설정

        // 새 SpeechRecognizer 를 만드는 팩토리 메서드
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext())
        speechRecognizer.setRecognitionListener(recognitionListener)    // 리스너 설정
        speechRecognizer.startListening(intent)

        Toast.makeText(
            requireContext().applicationContext,
            "녹음 시작되었습니다",
            Toast.LENGTH_SHORT
        ).show()// 듣기 시작
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Fragment가 종료될 때 SpeechRecognizer를 해제해야 합니다.
        speechRecognizer.destroy()
    }

    // 권한 설정 메소드
    private fun requestPermission() {
        // Check if the permission is not granted
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request the permission
            requestPermissions(
                arrayOf(android.Manifest.permission.RECORD_AUDIO),
                RECORD_AUDIO_PERMISSION_CODE
            )
        }
    }

    companion object {
        private const val RECORD_AUDIO_PERMISSION_CODE = 1
    }

    // 리스너 설정
    private val recognitionListener: RecognitionListener = object : RecognitionListener {
        // 말하기 시작할 준비가되면 호출
        override fun onReadyForSpeech(params: Bundle) {
//            isRecording = true
//            binding.recordFin.visibility = View.VISIBLE
//            binding.recordDesLayoutFirst.visibility = View.INVISIBLE
//            binding.recordDesLayout1.visibility = View.VISIBLE
//            binding.recordDesLayout3.visibility = View.INVISIBLE
//

        }

        // 말하기 시작 했을 때 호출
        override fun onBeginningOfSpeech() {

        }

        // 입력받는 소리의 크기를 알려줌
        override fun onRmsChanged(rmsdB: Float) {}

        // 말을 시작하고 인식이 된 단어를 buffer에 담음
        override fun onBufferReceived(buffer: ByteArray) {}

        // 말하기를 중지하면 호출
        override fun onEndOfSpeech() {
        }

        // 오류 발생했을 때 호출
        override fun onError(error: Int) {
            val message = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "오디오 에러"
                SpeechRecognizer.ERROR_CLIENT -> "클라이언트 에러"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "퍼미션 없음"
                SpeechRecognizer.ERROR_NETWORK -> "네트워크 에러"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "네트웍 타임아웃"
                SpeechRecognizer.ERROR_NO_MATCH -> "찾을 수 없음"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "RECOGNIZER 가 바쁨"
                SpeechRecognizer.ERROR_SERVER -> "서버가 이상함"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "말하는 시간초과"
                else -> "알 수 없는 오류임"
            }
        }

        // 인식 결과가 준비되면 호출
        override fun onResults(results: Bundle) {
            // 말을 하면 ArrayList에 단어를 넣고 textView에 단어를 이어줌
            val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                // 첫 번째 결과만 사용할 경우
                //val recognizedText = matches[0]
                // UI에 인식된 텍스트를 표시할 수 있도록 처리
                // binding.textView.text = recognizedText

                // 모든 결과를 사용하고 싶을 경우
                for (match in matches) {
                    Log.d("대화 내용", match)
                    // UI에 인식된 각 텍스트를 표시할 수 있도록 처리
                }
            }
        }

        // 부분 인식 결과를 사용할 수 있을 때 호출
        override fun onPartialResults(partialResults: Bundle) {}

        // 향후 이벤트를 추가하기 위해 예약
        override fun onEvent(eventType: Int, params: Bundle) {}
    }

    private fun highlightSentencesInText(
        text: String,
        sentencesToHighlight: List<String>,
        colorCode: String
    ): SpannableString {
        val color = Color.parseColor(colorCode)
        val spannableString = SpannableString(text)
        for (sentence in sentencesToHighlight) {
            val startIndex = text.indexOf(sentence)
            if (startIndex != -1) {
                spannableString.setSpan(
                    ForegroundColorSpan(color),
                    startIndex,
                    startIndex + sentence.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
        return spannableString
    }
}