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
import java.io.File
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
        val randomIndex = (randomTexts.indices).random()
        val randomText = randomTexts[randomIndex]

        // 선택된 텍스트를 강조하여 화면에 표시
        val highlightedText = highlightSentencesInText(randomText, sentencesToHighlight, colorCode)
        binding.homeRandomText.text = highlightedText

        if(binding.recordFin.visibility == View.VISIBLE || binding.recordCancel.visibility == View.VISIBLE
            || binding.recordSee.visibility == View.VISIBLE ){
            binding.homeRecord.isClickable = false
        }

        binding.homeRecord.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Permission is not granted
                val permissions = arrayOf(android.Manifest.permission.RECORD_AUDIO)
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    permissions,
                    RECORD_AUDIO_PERMISSION_REQUEST_CODE
                )
            } else {
                // Permission is granted, start recording
                startRecording()

                binding.recordFin.visibility = View.VISIBLE
                binding.recordCancel.visibility = View.VISIBLE
                binding.recordDesLayoutFirst.visibility = View.INVISIBLE
                binding.recordDesLayout1.visibility = View.VISIBLE
                binding.recordDesLayout3.visibility = View.INVISIBLE
            }
        }

        // "record_cancel" 버튼 클릭 이벤트 처리
        binding.recordCancel.setOnClickListener {
            cancelRecording()
        }

        binding.recordFin.setOnClickListener {
            if (state) {
                stopRecording()
                binding.recordFin.visibility = View.INVISIBLE
                binding.recordSee.visibility = View.VISIBLE
                binding.recordCancel.visibility = View.INVISIBLE
                binding.recordDesLayout2.visibility = View.VISIBLE
                binding.recordDesLayout1.visibility = View.INVISIBLE
            }
        }
        return binding.root
    }

    private fun cancelRecording() {
        if (state) {
            // 녹음 중이면 녹음 중지
            stopRecording()
            //binding.recordFin.visibility = View.VISIBLE
            binding.recordDesLayoutFirst.visibility = View.VISIBLE
            binding.recordFin.visibility = View.INVISIBLE
            binding.recordCancel.visibility = View.INVISIBLE
            binding.recordDesLayout1.visibility = View.INVISIBLE
            binding.recordDesLayout3.visibility = View.INVISIBLE
        }
        // 저장 중이던 파일이 있으면 삭제
        output?.let {
            val file = File(it)
            if (file.exists()) {
                file.delete()
            }
        }
        // 녹음 상태 초기화
        state = false
        // 취소 메시지 표시
        Toast.makeText(
            requireContext().applicationContext,
            "녹음이 취소되었습니다",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun startRecording() {
        val fileName: String = Date().time.toString() + ".mp3"
        val outputDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val outputFile = File(outputDir, fileName)

        mediaRecorder = MediaRecorder()
        mediaRecorder?.setAudioSource((MediaRecorder.AudioSource.MIC))
        mediaRecorder?.setOutputFormat((MediaRecorder.OutputFormat.MPEG_4))
        mediaRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        mediaRecorder?.setOutputFile(outputFile.absolutePath)

        binding.homeRecord.isClickable = false
//        if(binding.recordFin.visibility == View.VISIBLE || binding.recordCancel.visibility == View.VISIBLE
//            || binding.recordSee.visibility == View.VISIBLE ){
//            binding.homeRecord.isClickable = false
//        }

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
            Toast.makeText(
                requireContext().applicationContext,
                "녹음 시작에 문제가 발생했습니다: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(
                requireContext().applicationContext,
                "녹음 시작에 문제가 발생했습니다: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun stopRecording() {
        if (state) {
            mediaRecorder?.stop()
            mediaRecorder?.reset()
            mediaRecorder?.release()
            state = false

            if(binding.recordCancel.visibility != View.VISIBLE)
            {
                Toast.makeText(
                    requireContext().applicationContext,
                    "녹음이 완료 되었습니다",
                    Toast.LENGTH_SHORT
                ).show()
            }
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