package com.example.dou

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter(private val chatItems: ArrayList<ChatItem>) :
    RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.chat_item, parent, false)
        return ChatViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val item = chatItems[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int {
        return chatItems.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (chatItems[position].isSentByMe) {
            SENT_MESSAGE_TYPE
        } else {
            RECEIVED_MESSAGE_TYPE
        }
    }

    inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val message: TextView = itemView.findViewById(R.id.chat_txt)

        fun bind(item: ChatItem) {
            message.text = item.message
            // 보내는 메시지인 경우 배경 색상 변경
            if (item.isSentByMe) {
                itemView.setBackgroundResource(R.drawable.sent_message_background)
            } else {
                itemView.setBackgroundResource(R.drawable.received_message_background)
            }
        }
    }

    companion object {
        private const val SENT_MESSAGE_TYPE = 0
        private const val RECEIVED_MESSAGE_TYPE = 1
    }
}