package com.example.dou

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CalAdapter(private val itemCal: ArrayList<CalItem>, private val onItemClick: (Int) -> Unit) :
    RecyclerView.Adapter<CalAdapter.CalViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.cal_item, parent, false)
        return CalViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return itemCal.size
    }

    override fun onBindViewHolder(holder: CalViewHolder, position: Int) {
        val item = itemCal[position]
        holder.bind(item)
    }

    inner class CalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        init {
            // 각 아이템을 클릭하는 이벤트를 처리합니다.
            itemView.setOnClickListener {
                onItemClick(adapterPosition) // 클릭한 아이템의 위치를 가져와서 전달합니다.
            }
        }

        fun bind(item: CalItem) {
            // 아이템의 위치 값을 사용하여 텍스트로 표시합니다.
            // 이 부분에서는 위치 값에 대한 표시를 하지 않습니다.
        }
    }
}