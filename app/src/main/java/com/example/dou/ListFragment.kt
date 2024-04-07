package com.example.dou

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.NavHostFragment.Companion.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dou.databinding.FragmentHomeBinding
import com.example.dou.databinding.FragmentListBinding

class ListFragment : Fragment() {
    private lateinit var binding: FragmentListBinding
    private val listItem = arrayListOf<ListItem>()
    private val listAdapter = ListAdapter(listItem)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        listItem.apply {
            add(ListItem("#1", "힘든 하루에 대한 대화"))
            add(ListItem("#2", "친구들이랑 밥먹고 영화에 대한 대화"))
            add(ListItem("#3", "사회생활 중 갈등 상황에 대한 대화"))
            add(ListItem("#4", "40"))
            add(ListItem("#5", "50"))
            add(ListItem("#6", "60"))
            add(ListItem("#7", "40"))
            add(ListItem("#8", "50"))
            add(ListItem("#9", "60"))
            add(ListItem("#10", "60"))
            // 필요한 만큼 데이터를 추가할 수 있습니다.
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentListBinding.inflate(inflater, container, false)

        binding.talkBtn.setOnClickListener {
            val navController = findNavController()

            navController.navigate(R.id.action_listFragment_to_homeFragment)
        }

        binding.listRecycler.layoutManager = LinearLayoutManager(context)
        binding.listRecycler.adapter = listAdapter
        return binding.root
    }
}