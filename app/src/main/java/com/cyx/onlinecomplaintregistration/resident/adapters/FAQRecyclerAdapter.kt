package com.cyx.onlinecomplaintregistration.resident.adapters

import android.content.Context
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cyx.onlinecomplaintregistration.R
import com.cyx.onlinecomplaintregistration.classes.Complaint
import com.cyx.onlinecomplaintregistration.classes.Constants
import com.cyx.onlinecomplaintregistration.classes.FAQ
import com.cyx.onlinecomplaintregistration.management.activities.ManagementViewSingleComplaintActivity
import com.cyx.onlinecomplaintregistration.resident.activities.home.ViewSingleComplaintActivity
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

class FAQRecyclerAdapter(
    private var faq: List<FAQ>
) : RecyclerView.Adapter<FAQRecyclerAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textQuestion: TextView = itemView.findViewById(R.id.text_question)
        val textAnswer: TextView = itemView.findViewById(R.id.text_answer)
        init {
            Constants.sharedPref =
                itemView.context.getSharedPreferences("sharedPref", Context.MODE_PRIVATE)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.help_recycler_view, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.textAnswer.text = faq[position].faq_answer
        holder.textQuestion.text = faq[position].faq_question
    }

    override fun getItemCount(): Int {
        return faq.size
    }
}