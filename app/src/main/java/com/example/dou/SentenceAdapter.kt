package com.example.dou

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SentenceAdapter(private val sentenceItems: MutableList<SentenceItem>, private val onItemClick: (Int) -> Unit) :
    RecyclerView.Adapter<SentenceAdapter.SentenceViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SentenceViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.sentence_item, parent, false)
        return SentenceViewHolder(itemView, onItemClick)
    }

    override fun onBindViewHolder(holder: SentenceViewHolder, position: Int) {
        val item = sentenceItems[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int {
        return sentenceItems.size
    }

    inner class SentenceViewHolder(itemView: View, private val onItemClick: (Int) -> Unit) : RecyclerView.ViewHolder(itemView) {
        private val category: TextView = itemView.findViewById(R.id.category_sentence)
        private val bgClickSentence: View = itemView.findViewById(R.id.bg_click_sentence)

        init {
            itemView.setOnClickListener {
                onItemClick(adapterPosition)
                notifyDataSetChanged() // 클릭된 아이템이 변경되었으므로 어댑터에 데이터 변경을 알림
            }
        }

        fun bind(item: SentenceItem) {
            category.text = item.category
            bgClickSentence.visibility = if (item.isSelected) View.VISIBLE else View.INVISIBLE
        }
    }
}