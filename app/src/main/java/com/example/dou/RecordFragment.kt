package com.example.dou

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.dou.databinding.FragmentListBinding
import com.example.dou.databinding.FragmentRecord2Binding


class RecordFragment : Fragment() {
    private lateinit var binding: FragmentRecord2Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentRecord2Binding.inflate(inflater, container, false)


        return binding.root
    }


}