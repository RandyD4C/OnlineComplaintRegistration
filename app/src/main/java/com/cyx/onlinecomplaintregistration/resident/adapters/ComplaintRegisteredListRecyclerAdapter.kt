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
import com.cyx.onlinecomplaintregistration.management.activities.ManagementViewSingleComplaintActivity
import com.cyx.onlinecomplaintregistration.resident.activities.home.ViewSingleComplaintActivity
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

class ComplaintRegisteredListRecyclerAdapter(
    private var complaint: List<Complaint>
) : RecyclerView.Adapter<ComplaintRegisteredListRecyclerAdapter.ViewHolder>() {

    private lateinit var geocoder: Geocoder
    private var addresses = mutableListOf<Address>().toMutableList()
    private var address = mutableListOf<String>()

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textPostDate: TextView = itemView.findViewById(R.id.text_post_date)
        val textComplaintCategory: TextView = itemView.findViewById(R.id.text_complaint_category)
        val textComplaintLocation: TextView = itemView.findViewById(R.id.text_complaint_location)
        private val textViewMore: TextView = itemView.findViewById(R.id.text_view_more)
        val textUrgent: TextView = itemView.findViewById(R.id.text_urgent)
        val textImportant: TextView = itemView.findViewById(R.id.text_important)

        init {
            for (i in complaint.indices) {
                loadLocation(
                    itemView.context,
                    complaint[i].complaint_latitude,
                    complaint[i].complaint_longitude
                )
            }
            Constants.sharedPref =
                itemView.context.getSharedPreferences("sharedPref", Context.MODE_PRIVATE)
            textViewMore.setOnClickListener {
                val position: Int = adapterPosition
                val intent = if (Constants.userRole == "Resident") Intent(
                    itemView.context,
                    ViewSingleComplaintActivity::class.java
                ) else Intent(itemView.context, ManagementViewSingleComplaintActivity::class.java)
                intent.putExtra("complaint_uid", complaint[position].complaint_uid)
                itemView.context.startActivity(intent)
            }
        }
    }

    private fun loadLocation(context: Context, latitude: Double, longitude: Double) {
        geocoder = Geocoder(context, Locale.getDefault())
        addresses.clear()
        addresses =
            geocoder.getFromLocation(latitude, longitude, 1)!!
        address.add(addresses[0].getAddressLine(0))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.complaint_registered_recycler_view, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val dateTime = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocalDateTime.parse(complaint[position].complaint_date_time)
        } else {
            TODO("VERSION.SDK_INT < O")
        }
        val formatter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
        } else {
            TODO("VERSION.SDK_INT < O")
        }
        val formatted = dateTime.format(formatter)
        holder.textPostDate.text = formatted
        holder.textImportant.visibility =
            if (complaint[position].complaint_priority == 1) View.VISIBLE else View.GONE
        holder.textUrgent.visibility =
            if (complaint[position].complaint_priority == 2) View.VISIBLE else View.GONE
        holder.textComplaintCategory.text = "#${complaint[position].complaint_category}"
        holder.textComplaintLocation.text = address[position]
    }

    override fun getItemCount(): Int {
        return complaint.size
    }
}