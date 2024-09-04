package com.example.dou

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ListAdapter(
    private val listItems: ArrayList<ListItem>,
    private val onItemClicked: (Int) -> Unit  // 클릭 시 roomId 전달하는 람다
) : RecyclerView.Adapter<ListAdapter.ListViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListAdapter.ListViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item, parent, false)
        return ListViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ListAdapter.ListViewHolder, position: Int) {
        val item = listItems[position]
        holder.bind(item)

        // 아이템 클릭 시 roomId 전달
        holder.itemView.setOnClickListener {
            onItemClicked(item.roomId)
        }
    }

    override fun getItemCount(): Int {
        return listItems.size
    }

    class ListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val listCnt: TextView = itemView.findViewById(R.id.list_cnt)
        private val listTxt: TextView = itemView.findViewById(R.id.ch_txt)

        fun bind(item: ListItem) {
            listCnt.text = item.listCnt
            listTxt.text = item.listTxt
        }
    }
}
