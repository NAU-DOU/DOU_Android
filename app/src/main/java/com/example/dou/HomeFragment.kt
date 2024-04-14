package com.example.dou

import android.content.pm.PackageManager
import android.graphics.Color
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Environment
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.dou.databinding.FragmentHomeBinding
import java.io.IOException
import java.util.Date
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
                } else if (!state) {
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
}