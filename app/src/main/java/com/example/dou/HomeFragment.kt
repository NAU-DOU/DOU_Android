package com.example.dou

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Environment
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
import java.io.File
import java.io.IOException
import java.util.Date
import kotlin.random.Random

class HomeFragment : Fragment() {
    private val CREATE_FILE = 1
    private val RECORD_AUDIO_PERMISSION_REQUEST_CODE = 101
    private lateinit var binding: FragmentHomeBinding
    private var recordedFilePath: String? = null
    private var mediaRecorder: MediaRecorder? = null
    private var state: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Check and request necessary permissions
        requestAudioPermission()
    }

    private fun requestAudioPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.RECORD_AUDIO),
                RECORD_AUDIO_PERMISSION_REQUEST_CODE
            )
        } else {
            //startRecording()
        }
    }

    private var output: String? = null

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
            startRecording()

            binding.recordFin.visibility = View.VISIBLE
            binding.recordCancel.visibility = View.VISIBLE
            binding.recordDesLayoutFirst.visibility = View.INVISIBLE
            binding.recordDesLayout1.visibility = View.VISIBLE
            binding.recordDesLayout3.visibility = View.INVISIBLE
//            if (ContextCompat.checkSelfPermission(
//                    requireContext(),
//                    android.Manifest.permission.RECORD_AUDIO
//                ) != PackageManager.PERMISSION_GRANTED
//            ) {
//                // Permission is not granted
//                val permissions = arrayOf(android.Manifest.permission.RECORD_AUDIO)
//                ActivityCompat.requestPermissions(
//                    requireActivity(),
//                    permissions,
//                    RECORD_AUDIO_PERMISSION_REQUEST_CODE
//                )
//            } else {
//                // Permission is granted, start recording
//                startRecording()
//
//                binding.recordFin.visibility = View.VISIBLE
//                binding.recordCancel.visibility = View.VISIBLE
//                binding.recordDesLayoutFirst.visibility = View.INVISIBLE
//                binding.recordDesLayout1.visibility = View.VISIBLE
//                binding.recordDesLayout3.visibility = View.INVISIBLE
//            }
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

        // recordSee 버튼의 클릭 이벤트 핸들러
        binding.recordSee.setOnClickListener {
            val navController = findNavController()

            // 채팅 화면 테스트를 위한 action 잠시 추가
            navController.navigate(R.id.action_homeFragment_to_emotionFragment)
            Log.d("파일 위치", "$recordedFilePath")
        }

        return binding.root
    }

    private fun cancelRecording() {
        if (state) {
            // 녹음 중이면 녹음 중지
            //stopRecording()
            //binding.recordFin.visibility = View.VISIBLE
            binding.recordDesLayoutFirst.visibility = View.VISIBLE
            binding.recordFin.visibility = View.INVISIBLE
            binding.recordCancel.visibility = View.INVISIBLE
            binding.recordDesLayout1.visibility = View.INVISIBLE
            binding.recordDesLayout3.visibility = View.INVISIBLE

            // 저장 중이던 파일 삭제
            output?.let {
                val file = File(it)
                if (file.exists()) {
                    file.delete()
                    Log.d("CanceledRecording", "Recording file deleted: $it")
                    Toast.makeText(
                        requireContext().applicationContext,
                        "녹음이 취소되었습니다. 파일이 삭제되었습니다.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
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
        val fileName: String = "audio_${Date().time}.mp3"
        val outputDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val outputFile = File(outputDir, fileName)

        mediaRecorder = MediaRecorder()
        mediaRecorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
        mediaRecorder?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        mediaRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        mediaRecorder?.setOutputFile(outputFile.absolutePath)

        try {
            mediaRecorder?.prepare()
            mediaRecorder?.start()
            state = true
            Toast.makeText(
                requireContext().applicationContext,
                "녹음을 시작했습니다.",
                Toast.LENGTH_SHORT
            ).show()

            // Save the path of recorded file
            recordedFilePath = outputFile.absolutePath

        } catch (e: IllegalStateException) {
            e.printStackTrace()
            Toast.makeText(
                requireContext().applicationContext,
                "녹음 실패: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(
                requireContext().applicationContext,
                "녹음 실패: ${e.message}",
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

            Toast.makeText(
                requireContext().applicationContext,
                "녹음을 완료했습니다.",
                Toast.LENGTH_SHORT
            ).show()

            // Create a document for the recorded file
            createAudioFile()
        } else {
            Toast.makeText(
                requireContext().applicationContext,
                "녹음이 시작되지 않았습니다.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun createAudioFile() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "audio/mpeg"
            putExtra(Intent.EXTRA_TITLE, "audio_record.mp3")
        }
        startActivityForResult(intent, CREATE_FILE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_AUDIO_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startRecording()
            } else {
                Toast.makeText(
                    requireContext(),
                    "오디오 권한이 없습니다.",
                    Toast.LENGTH_SHORT
                ).show()
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CREATE_FILE && resultCode == Activity.RESULT_OK) {
            // File creation success
            Toast.makeText(
                requireContext().applicationContext,
                "오늘의 대화가 성공적으로 저장됐어!",
                Toast.LENGTH_SHORT
            ).show()

            // Here you can do further operations with the recorded audio file
        } else {
            Toast.makeText(
                requireContext().applicationContext,
                "오늘의 대화를 저장하지 못했어!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}