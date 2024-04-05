package com.example.dou

import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Environment
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

class HomeFragment : Fragment() {
    private lateinit var binding: FragmentHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    private var output: String? = null
    private var mediaRecorder: MediaRecorder? = null
    private var state: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentHomeBinding.inflate(inflater,container,false)

        binding.homeRecord.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(),
                    android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(requireContext(),
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                //Permission is not granted
                val permissions = arrayOf(
                    android.Manifest.permission.RECORD_AUDIO,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                )
                ActivityCompat.requestPermissions(requireActivity(), permissions, 0)
            } else {
                startRecording()
                binding.recordFin.visibility = View.VISIBLE
                binding.recordDesLayoutFirst.visibility = View.INVISIBLE
                binding.recordDesLayout1.visibility = View.VISIBLE
            }
            binding.recordFin.setOnClickListener {
                stopRecording()
                binding.recordFin.visibility = View.INVISIBLE
                binding.recordSee.visibility = View.VISIBLE
                binding.recordDesLayout2.visibility = View.VISIBLE
                binding.recordDesLayout1.visibility = View.INVISIBLE
            }
        }
        return binding.root
    }

    private fun startRecording(){
        //config and create MediaRecorder Object
        val fileName: String = Date().time.toString() + ".mp3"
        output = Environment.getExternalStorageDirectory().absolutePath + "/Download/" + fileName //내장메모리 밑에 위치
        mediaRecorder = MediaRecorder()
        mediaRecorder?.setAudioSource((MediaRecorder.AudioSource.MIC))
        mediaRecorder?.setOutputFormat((MediaRecorder.OutputFormat.MPEG_4))
        mediaRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        mediaRecorder?.setOutputFile(output)

        try {
            mediaRecorder?.prepare()
            mediaRecorder?.start()
            state = true
            Toast.makeText(requireContext().applicationContext, "레코딩 시작되었습니다.", Toast.LENGTH_SHORT).show()
        } catch (e: IllegalStateException){
            e.printStackTrace()
        } catch (e: IOException){
            e.printStackTrace()
        }
    }

    private fun stopRecording(){
        if(state){
            mediaRecorder?.stop()
            mediaRecorder?.reset()
            mediaRecorder?.release()
            state = false
            Toast.makeText(requireContext().applicationContext, "중지 되었습니다.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext().applicationContext, "레코딩 상태가 아닙니다.", Toast.LENGTH_SHORT).show()
        }
    }
}
