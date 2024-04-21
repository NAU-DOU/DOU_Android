import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.dou.R
import com.example.dou.databinding.FragmentEmotionBinding
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.speech.v1.RecognitionAudio
import com.google.cloud.speech.v1.RecognitionConfig
import com.google.cloud.speech.v1.RecognizeRequest
import com.google.cloud.speech.v1.SpeechClient
import com.google.cloud.speech.v1.SpeechSettings
import com.google.protobuf.ByteString
import java.io.File
import java.io.InputStream

class EmotionFragment : Fragment() {
    private lateinit var binding: FragmentEmotionBinding
    private lateinit var speechClient: SpeechClient
    // 외부 저장소 읽기 권한 요청 코드
    private val READ_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE = 123

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentEmotionBinding.inflate(inflater, container, false)

        binding.emoTxt.text = "안녕"
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 권한 요청
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 123)
//        } else {
//            initializeSpeechClient()
//            startSpeechToTextConversion()
//        }

        // 외부 저장소 읽기 권한 요청
        requestReadExternalStoragePermission()
    }

    // 사용자에게 외부 저장소 읽기 권한을 요청하는 함수
    private fun requestReadExternalStoragePermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // 권한이 허용되지 않은 경우, 사용자에게 권한 요청 팝업을 표시합니다.
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                READ_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE
            )
        } else {
            // 이미 권한이 허용된 경우
            // 외부 저장소에서 Speech-to-Text 변환 작업을 수행합니다.
            initializeSpeechClient()
            startSpeechToTextConversion()
        }
    }


    // 권한 요청 결과 처리하는 함수
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            READ_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE -> {
                // 외부 저장소 읽기 권한 요청에 대한 결과 확인
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 권한이 허용된 경우
                    // 외부 저장소에서 Speech-to-Text 변환 작업을 수행합니다.
                    initializeSpeechClient()
                    startSpeechToTextConversion()
                    Toast.makeText(requireContext(), "외부 저장소 읽기 권한이 있습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    // 권한이 거부된 경우
                    // 사용자에게 권한이 필요함을 알리거나, 권한 설정 화면으로 이동할 수 있습니다.
                    // 여기서는 간단히 토스트 메시지를 통해 알립니다.
                    Toast.makeText(requireContext(), "외부 저장소 읽기 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
                }
                return
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    // 외부 저장소에서 파일을 읽어오는 함수
    private fun readExternalStorage() {
        // 외부 저장소에서 파일을 읽어오는 작업을 수행합니다.
        // 여기에 파일을 읽어오는 코드를 추가합니다.
    }
    private fun initializeSpeechClient() {
        val resourceId = R.raw.naudou // R.raw.naudou는 JSON 파일의 리소스 ID입니다.
        val credentials = getGoogleCredentialsFromResource(requireContext(), resourceId)

        // Google Cloud Speech-to-Text 클라이언트 초기화
        val settingsBuilder = SpeechSettings.newBuilder()
            .setCredentialsProvider { credentials }
            .build()
        speechClient = SpeechClient.create(settingsBuilder)
    }

    private fun startSpeechToTextConversion() {
        // example_file.mp3의 경로
        val filePath = "/storage/emulated/0/Download/example_file.mp3"

        // RecognitionAudio 객체 생성
        val audioData: ByteString = ByteString.copyFrom(File(filePath).readBytes())
        val audio = RecognitionAudio.newBuilder().setContent(audioData).build()

        // RecognitionConfig 생성
        val config = RecognitionConfig.newBuilder()
            .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16) // 예제 파일의 인코딩에 따라 적절히 설정
            .setSampleRateHertz(16000) // 예제 파일의 샘플 레이트에 따라 적절히 설정
            .setLanguageCode("ko-KR") // 음성 파일의 언어 코드에 따라 적절히 설정
            .build()

        // 변환 요청 생성
        val request = RecognizeRequest.newBuilder()
            .setConfig(config)
            .setAudio(audio)
            .build()

        // Speech-to-Text API로 변환 요청 보내기
        val response = speechClient.recognize(request)

        // 변환된 텍스트 처리
        val resultText = response.resultsList.joinToString("\n") { result ->
            result.alternativesList.joinToString("\n") { alternative ->
                alternative.transcript
            }
        }

        // 변환된 텍스트를 화면에 표시하거나 다른 곳에 전달하는 등의 작업 수행
        Log.d("Speech-to-Text Result", resultText)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        speechClient.close()
    }

    private fun getGoogleCredentialsFromResource(context: Context, resourceId: Int): GoogleCredentials {
        val inputStream: InputStream = context.resources.openRawResource(resourceId)
        return GoogleCredentials.fromStream(inputStream)
    }
}