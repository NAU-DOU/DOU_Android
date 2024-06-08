package com.example.dou

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.arthenica.mobileffmpeg.FFmpeg
import com.example.dou.databinding.FragmentHomeBinding
import java.io.File
import java.io.IOException
import java.util.Date

class HomeFragment : Fragment() {
    private val CREATE_FILE = 1
    private val RECORD_AUDIO_PERMISSION_REQUEST_CODE = 101
    private lateinit var binding: FragmentHomeBinding
    private var mediaRecorder: MediaRecorder? = null
    private var state: Boolean = false

    private var createdFileUri: Uri? = null

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

        binding.recordSee.setOnClickListener {
            val navController = findNavController()

            // 생성된 파일의 URI가 있는지 확인하고 전달
            createdFileUri?.let { uri ->
                // STT 변환 요청
                //requestSttConversion(uri)

//                // EmotionFragment로 이동
//                val bundle = Bundle()
//                bundle.putString("fileUri", uri.toString())
//                navController.navigate(R.id.action_homeFragment_to_emoFragment2, bundle)

                // EmotionActivity로 이동
                val intent = Intent(requireContext(), EmotionActivity::class.java).apply {
                    putExtra("fileUri", uri.toString())
                }
                requireActivity().startActivity(intent)

            } ?: run {
                // 생성된 파일의 URI가 없는 경우에 대한 처리
                Toast.makeText(requireContext(), "파일이 아직 생성되지 않았습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        return binding.root
    }

    private fun cancelRecording() {
        if (state) {
            stopRecording()
            binding.recordDesLayoutFirst.visibility = View.VISIBLE
            binding.recordFin.visibility = View.INVISIBLE
            binding.recordCancel.visibility = View.INVISIBLE
            binding.recordDesLayout1.visibility = View.INVISIBLE
            binding.recordDesLayout3.visibility = View.INVISIBLE

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
        state = false
        Toast.makeText(
            requireContext().applicationContext,
            "녹음이 취소되었습니다",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun startRecording() {
        val fileName: String = "audio_${Date().time}.mp3"
        val outputDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_MUSIC)
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
            output = outputFile.absolutePath
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
        output?.let { filePath ->
            val file = File(filePath)
            if (file.exists()) {
                val values = ContentValues().apply {
                    put(MediaStore.Audio.Media.DISPLAY_NAME, file.nameWithoutExtension + ".flac")
                    put(MediaStore.Audio.Media.MIME_TYPE, "audio/flac")
                    put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC)
                }

                val resolver = requireContext().contentResolver
                val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)

                try {
                    val outputFile = File(requireContext().getExternalFilesDir(Environment.DIRECTORY_MUSIC), file.nameWithoutExtension + ".flac")
                    val command = "-i $filePath -c:a flac ${outputFile.absolutePath}"
                    val rc = FFmpeg.execute(command)

                    if (rc == 0) {
                        Log.d("CreateAudioFile", "File conversion succeeded.")
                        createdFileUri = Uri.fromFile(outputFile)
                        file.delete()
                        Log.d("CreateAudioFile", "Saved file path: ${outputFile.path}")
                        Log.d("CreateAudioFile", "Saved file URI: $uri")
                        Log.d("CreateAudioFile", "Saved file URI: $createdFileUri")
                        onActivityResult(CREATE_FILE, Activity.RESULT_OK, null)
                    } else {
                        Log.e("CreateAudioFile", "File conversion failed with rc=$rc.")
                        Toast.makeText(
                            requireContext().applicationContext,
                            "오디오 파일 변환 중 오류가 발생했습니다.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: IOException) {
                    Log.e("CreateAudioFile", "Error converting file: ${e.message}")
                    Toast.makeText(
                        requireContext().applicationContext,
                        "오디오 파일 변환 중 오류가 발생했습니다.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

//    private fun requestSttConversion(uri: Uri) {
//        val retrofit = Retrofit.Builder()
//            .baseUrl("YOUR_CLOVA_SPEECH_API_BASE_URL")
//            .client(OkHttpClient.Builder().build())
//            .addConverterFactory(GsonConverterFactory.create())
//            .build()
//
//        val service = retrofit.create(ClovaSpeechApiService::class.java)
//
//        val params = "{\"language\":\"ko-KR\"}".toRequestBody("application/json".toMediaTypeOrNull())
//        val audioFile = File(uri.path!!)
//        val requestFile = audioFile.asRequestBody("audio/flac".toMediaTypeOrNull())
//        val body = MultipartBody.Part.createFormData("audio", audioFile.name, requestFile)
//
//        val call = service.recognizeSpeech(params, body)
//        call.enqueue(object : Callback<ClovaSpeechResponse> {
//            override fun onResponse(call: Call<ClovaSpeechResponse>, response: Response<ClovaSpeechResponse>) {
//                if (response.isSuccessful) {
//                    val sttResponse = response.body()
//                    sttResponse?.segments?.forEach {
//                        Log.d("STT Response", "Segment: ${it.text}")
//                    }
//                } else {
//                    Log.e("STT Request", "STT 요청 실패: ${response.code()} - ${response.errorBody()?.string()}")
//                }
//            }
//
//            override fun onFailure(call: Call<ClovaSpeechResponse>, t: Throwable) {
//                Log.e("STT Request", "STT 요청 실패", t)
//            }
//        })
//    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CREATE_FILE) {
            if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(
                    requireContext().applicationContext,
                    "오늘의 대화가 성공적으로 저장됐어!",
                    Toast.LENGTH_SHORT
                ).show()

            } else {
                Toast.makeText(
                    requireContext().applicationContext,
                    "오늘의 대화를 저장하지 못했어!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
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
}