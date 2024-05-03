package com.example.dou

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter(private val chatItems: List<ChatItem>) :
    RecyclerView.Adapter<ChatAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.chat_item, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val chatItem = chatItems[position]
        if (chatItem.isSentByMe) {
            holder.leftChatView.visibility = View.GONE
            holder.rightChatView.visibility = View.VISIBLE
            holder.rightChatTextView.text = chatItem.message
        } else {
            holder.rightChatView.visibility = View.GONE
            holder.leftChatView.visibility = View.VISIBLE
            holder.leftChatTextView.text = chatItem.message
        }
    }

    override fun getItemCount(): Int = chatItems.size

    override fun getItemViewType(position: Int): Int {
        return if (chatItems[position].isSentByMe) {
            SENT_MESSAGE_TYPE
        } else {
            RECEIVED_MESSAGE_TYPE
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val leftChatView: LinearLayout = itemView.findViewById(R.id.left_chat_view)
        val rightChatView: LinearLayout = itemView.findViewById(R.id.right_chat_view)
        val leftChatTextView: TextView = itemView.findViewById(R.id.left_chat_txt)
        val rightChatTextView: TextView = itemView.findViewById(R.id.right_chat_txt)
    }

    companion object {
        private const val SENT_MESSAGE_TYPE = 0
        private const val RECEIVED_MESSAGE_TYPE = 1
    }
}