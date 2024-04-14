package com.example.dou

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.MediaRecorder
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
    private val RECORD_AUDIO_PERMISSION_REQUEST_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    private var output: String? = null
    private var mediaRecorder: MediaRecorder? = null
    private var state: Boolean = false
    private var sttResult: String? = null

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
        "지금 당장 행복했으면", "도우가 토닥토닥 해줄게", "힘들면 쉬어도 돼","넌 맛있는 밥 먹었어?"
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

        binding.homeRecord.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Permission is not granted
                val permissions = arrayOf(
                    android.Manifest.permission.RECORD_AUDIO,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                )
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    permissions,
                    RECORD_AUDIO_PERMISSION_REQUEST_CODE
                )
            } else {
                if (!state && binding.recordDesLayout3.visibility == View.VISIBLE) {
                    pauseRecording()
                    binding.recordFin.visibility = View.VISIBLE
                    binding.recordDesLayoutFirst.visibility = View.INVISIBLE
                    binding.recordDesLayout1.visibility = View.VISIBLE
                    binding.recordDesLayout3.visibility = View.INVISIBLE
                }
                else if (!state && binding.recordDesLayout2.visibility == View.VISIBLE) {
                    binding.homeRecord.isClickable = false
                }
                else if (!state) {
                    startRecording()
                    binding.recordFin.visibility = View.VISIBLE
                    binding.recordDesLayoutFirst.visibility = View.INVISIBLE
                    binding.recordDesLayout1.visibility = View.VISIBLE
                    binding.recordDesLayout3.visibility = View.INVISIBLE
                } else {
                    pauseRecording()
                    binding.recordFin.visibility = View.INVISIBLE // 재생 버튼으로 변경하거나 숨기거나
                    binding.recordDesLayoutFirst.visibility = View.INVISIBLE
                    binding.recordDesLayout1.visibility = View.INVISIBLE
                    binding.recordDesLayout3.visibility = View.VISIBLE
                }
            }
        }

        binding.recordFin.setOnClickListener {
            if (state) {
                stopRecording()
                binding.recordFin.visibility = View.INVISIBLE
                binding.recordSee.visibility = View.VISIBLE
                binding.recordDesLayout2.visibility = View.VISIBLE
                binding.recordDesLayout1.visibility = View.INVISIBLE

                // 녹음이 완료된 후 STT를 수행합니다.
                // performSTT()
            }
        }

        // record_see 버튼 클릭 이벤트 핸들러
        binding.recordSee.setOnClickListener {
            // STT 결과가 있는지 확인하고, EmotionFragment로 전달
            if (!sttResult.isNullOrEmpty()) {
                val bundle = Bundle().apply {
                    putString("stt_result", sttResult)
                }
                // NavController를 통해 EmotionFragment로 이동
                findNavController().navigate(
                    R.id.action_homeFragment_to_emotionFragment,
                    bundle
                )
            } else {
                // STT 결과가 없는 경우에 대한 처리
                Toast.makeText(requireContext(), "No STT result available", Toast.LENGTH_SHORT).show()
            }
        }

        return binding.root
    }

    private fun startRecording() {
        //config and create MediaRecorder Object
        val fileName: String = Date().time.toString() + ".mp3"
        output =
            Environment.getExternalStorageDirectory().absolutePath + "/Download/" + fileName //내장메모리 밑에 위치
        mediaRecorder = MediaRecorder()
        mediaRecorder?.setAudioSource((MediaRecorder.AudioSource.MIC))
        mediaRecorder?.setOutputFormat((MediaRecorder.OutputFormat.MPEG_4))
        mediaRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        mediaRecorder?.setOutputFile(output)

        try {
            mediaRecorder?.prepare()
            mediaRecorder?.start()
            state = true
            Toast.makeText(
                requireContext().applicationContext,
                "녹음 시작되었습니다",
                Toast.LENGTH_SHORT
            ).show()

            // 녹음이 시작되면 동시에 STT를 수행합니다.
            performSTT() // STT를 시작합니다.
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun pauseRecording() {
        if (state) {
            mediaRecorder?.pause()
            state = false
            Toast.makeText(
                requireContext().applicationContext,
                "녹음을 일시중지합니다",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            if (binding.recordDesLayout3.visibility == View.VISIBLE) {
                mediaRecorder?.resume()
                state = true
                Toast.makeText(
                    requireContext().applicationContext,
                    "녹음이 다시 시작되었습니다",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    requireContext().applicationContext,
                    "녹음 중이 아닙니다.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun stopRecording() {
        if (state) {
            mediaRecorder?.stop()
            mediaRecorder?.reset()
            mediaRecorder?.release()
            state = false
            Toast.makeText(
                requireContext().applicationContext,
                "녹음이 완료 되었습니다",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                requireContext().applicationContext,
                "녹음 중이 아닙니다",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            RECORD_AUDIO_PERMISSION_REQUEST_CODE -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission is granted, start recording
                    startRecording()
                } else {
                    // Permission denied
                    Toast.makeText(requireContext(), "Permission Denied!", Toast.LENGTH_SHORT).show()
                }
                return
            }
        }
    }

    // highlightSentencesInText 함수 정의
    private fun highlightSentencesInText(text: String, sentencesToHighlight: List<String>, colorCode: String): SpannableString {
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

    private fun performSTT() {
        val speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())

        val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext(), ComponentName("com.google.android.googlequicksearchbox", "com.google.android.voicesearch.serviceapi.GoogleRecognitionService"))
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                // 사용자의 음성이 감지되었을 때 호출됩니다.
            }

            override fun onBeginningOfSpeech() {
                // 사용자가 음성을 시작했을 때 호출됩니다.
            }

            override fun onRmsChanged(rmsdB: Float) {
                // 사용자의 음성 입력이 감지되는 동안 호출됩니다.
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                // 음성 입력 버퍼가 수신될 때 호출됩니다.
            }

            override fun onEndOfSpeech() {
                // 사용자의 음성 입력이 종료되었을 때 호출됩니다.
            }

            override fun onError(error: Int) {
                // 오류가 발생했을 때 호출됩니다.
                Toast.makeText(requireContext(), "음성 인식 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }

            override fun onResults(results: Bundle?) {
                // 음성 인식 결과가 준비되었을 때 호출됩니다.
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (matches != null && matches.isNotEmpty()) {
                    // 전역 변수에 결과를 저장합니다.
                    sttResult = matches[0]

                    Log.d("STT_Result", "STT Result: $sttResult")
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                // 부분적인 음성 인식 결과가 수신될 때 호출됩니다.
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                // 다양한 이벤트에 대한 추가 정보를 제공하는 경우 호출됩니다.
            }
        })

        // SpeechRecognizer에 음성 인식 요청을 시작합니다.
        speechRecognizer.startListening(speechRecognizerIntent)
    }
}