package com.cyx.onlinecomplaintregistration.adapters

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.view.*
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.cyx.onlinecomplaintregistration.R
import com.cyx.onlinecomplaintregistration.classes.Constants
import com.cyx.onlinecomplaintregistration.classes.Notification
import com.cyx.onlinecomplaintregistration.resident.activities.home.ComplaintPostActivity
import com.cyx.onlinecomplaintregistration.resident.activities.home.ViewComplaintsActivity
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle


class NotificationRecyclerAdapter(
    private var notification: List<Notification>
) : RecyclerView.Adapter<NotificationRecyclerAdapter.ViewHolder>() {

    private lateinit var database: FirebaseDatabase
    private lateinit var reference: DatabaseReference

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textTitle: TextView = itemView.findViewById(R.id.text_title)
        val textMessage: TextView = itemView.findViewById(R.id.text_message)
        val textDateTime: TextView = itemView.findViewById(R.id.text_date)
        val cardNotification: CardView = itemView.findViewById(R.id.card_notification)

        init {
            database = Constants.database
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.notification_recycler_view, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.textTitle.text = notification[position].notification_title
        holder.textMessage.text = notification[position].notification_message
        val dateTime = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocalDateTime.parse(notification[position].notification_date_time)
        } else {
            TODO("VERSION.SDK_INT < O")
        }
        val formatter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
        } else {
            TODO("VERSION.SDK_INT < O")
        }
        val formatted = dateTime.format(formatter)
        holder.textDateTime.text = formatted

        if (notification[position].notification_status == "Read") {
            holder.textTitle.setTextColor(Color.parseColor("#cccccc"))
            holder.textMessage.setTextColor(Color.parseColor("#cccccc"))
            holder.textDateTime.setTextColor(Color.parseColor("#cccccc"))
        } else {
            holder.textTitle.setTextColor(Color.BLACK)
            holder.textMessage.setTextColor(Color.BLACK)
            holder.textDateTime.setTextColor(Color.BLACK)
        }

        holder.cardNotification.setOnClickListener {
            setNotificationRead(
                notification[position].notification_uid,
                notification[position].user_uid,
                notification[position].notification_category,
                it
            )
        }
    }

    private fun setNotificationRead(
        notificationUid: String,
        userUID: String,
        notificationCategory: Int,
        view: View
    ) {
        reference = database.getReference("Notification/$userUID/$notificationUid")
        val updateStatus = mapOf(
            "notification_status" to "Read"
        )
        reference.updateChildren(updateStatus).addOnCompleteListener {
            if (it.isSuccessful) {
                when (notificationCategory) {
                    0 -> { // Complaint Solved -> Resident
                        Intent(view.context, ComplaintPostActivity::class.java).apply {
                            putExtra("complaint_status", "Solved")
                            view.context.startActivity(this)
                        }
                    }
                    1 -> { // Complaint Removed -> Resident
                        Intent(view.context, ComplaintPostActivity::class.java).apply {
                            putExtra("complaint_status", "Removed")
                            view.context.startActivity(this)
                        }
                    }
                    2 -> { // Complaint Reported -> Admin
                        // Intent to Complaint Reported Activity
                    }
                    3 -> { // Register Management -> Admin
                        // Intent to Handle Management Registration Activity
                    }
                    4 -> { // Register Management Successfully -> Management

                    }
                    5 -> { // Register Management Rejected -> Resident

                    }
                    6 -> { // Urgent Complaint -> Management
                        Intent(view.context, ViewComplaintsActivity::class.java).apply {
                            putExtra("complaint_priority", "2")
                            view.context.startActivity(this)
                        }
                    }
                }
            }
        }
    }

    fun deleteNotification(position: Int, context: Context) {
        reference =
            database.getReference("Notification/${notification[position].user_uid}/${notification[position].notification_uid}")
        reference.removeValue().addOnCompleteListener {
            if (it.isSuccessful) {
                Toast.makeText(context, "Notification deleted.", Toast.LENGTH_SHORT).show()
                notifyDataSetChanged()
            }
        }
    }

    override fun getItemCount(): Int {
        return notification.size
    }

}