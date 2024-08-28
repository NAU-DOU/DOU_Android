package com.example.dou

import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.ShapeDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import androidx.appcompat.widget.TooltipCompat
import androidx.core.content.ContextCompat
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
        private val calColorView: View = itemView.findViewById(R.id.cal_color)

        init {
            itemView.setOnClickListener {
                onItemClick(adapterPosition)
            }

            itemView.setOnLongClickListener {
                showTooltip(itemCal[adapterPosition])
                true
            }
        }

        fun bind(item: CalItem) {
            val gradientDrawable = GradientDrawable()
            gradientDrawable.cornerRadius = 8f
            gradientDrawable.setColor(ContextCompat.getColor(itemView.context, when (item.sentiment) {
                0 -> R.color.sentiment0Color
                1 -> R.color.sentiment1Color
                2 -> R.color.sentiment2Color
                3 -> R.color.sentiment3Color
                4 -> R.color.sentiment4Color
                5 -> R.color.sentiment5Color
                6 -> R.color.sentiment6Color
                else -> R.color.defaultColor
            }))

            // Set the background of the view
            calColorView.background = gradientDrawable
        }

        private fun showTooltip(item: CalItem) {
            // Inflate the custom tooltip layout
            val inflater = LayoutInflater.from(itemView.context)
            val tooltipView = inflater.inflate(R.layout.tooltip_layout, null)
            val tooltipText: TextView = tooltipView.findViewById(R.id.tooltip_text)

            // Set the sentiment text
            val sentimentText = when (item.sentiment) {
                0 -> "행복"
                1 -> "놀람"
                2 -> "중립"
                3 -> "슬픔"
                4 -> "꺼림"
                5 -> "분노"
                6 -> "두려움"
                else -> "기본"
            }

            tooltipText.text = sentimentText

            // Create the PopupWindow
            val popupWindow = PopupWindow(tooltipView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true)

            // Set xOffset and yOffset based on sentiment
            val xOffset = if (item.sentiment == 6) -30 else -12
            val yOffset = 10

            // Show the tooltip with the calculated offsets
            popupWindow.showAsDropDown(calColorView, xOffset, yOffset)
        }
    }
}