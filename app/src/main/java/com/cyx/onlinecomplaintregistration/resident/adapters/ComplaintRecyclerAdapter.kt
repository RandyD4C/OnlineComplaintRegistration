package com.cyx.onlinecomplaintregistration.resident.adapters

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.icu.text.CompactDecimalFormat
import android.os.Build
import android.util.Log
import android.view.*
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cyx.onlinecomplaintregistration.R
import com.cyx.onlinecomplaintregistration.classes.*
import com.cyx.onlinecomplaintregistration.resident.activities.home.ViewComplaintsActivity
import com.cyx.onlinecomplaintregistration.resident.activities.map.ViewLocationActivity
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.layout_report_complaint.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*


class ComplaintRecyclerAdapter(
    private var complaint: List<Complaint>
) : RecyclerView.Adapter<ComplaintRecyclerAdapter.ViewHolder>(), View.OnCreateContextMenuListener {

    private lateinit var database: FirebaseDatabase
    private lateinit var reference: DatabaseReference
    private lateinit var referenceUpVote: DatabaseReference
    private lateinit var referenceDownVote: DatabaseReference
    private var upVoteList = mutableListOf<Int>()
    private var downVoteList = mutableListOf<Int>()
    private var adminList = mutableListOf<String>()
    private val entries = mutableListOf<BarEntry>()

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userImg: ImageView = itemView.findViewById(R.id.user_avatar)
        val userName: TextView = itemView.findViewById(R.id.text_name)
        val complaintDescription: TextView = itemView.findViewById(R.id.text_complaint_desc)
        val postDateTime: TextView = itemView.findViewById(R.id.text_post_date)
        val complaintImg: ImageView = itemView.findViewById(R.id.image_complaint)
        val textUpVote: TextView = itemView.findViewById(R.id.text_upvote)
        val textDownVote: TextView = itemView.findViewById(R.id.text_down_vote)
        val textUrgent: TextView = itemView.findViewById(R.id.text_urgent)
        val textImportant: TextView = itemView.findViewById(R.id.text_important)
        val textComplaintCategory: TextView = itemView.findViewById(R.id.text_complaint_category)
        val buttonUpVote: MaterialButton = itemView.findViewById(R.id.button_upvote)
        val buttonDownVote: MaterialButton = itemView.findViewById(R.id.button_downvote)
        val cardAvatar: CardView = itemView.findViewById(R.id.card_avatar)
        val verifiedUser: ImageView = itemView.findViewById(R.id.image_verified_user)
        val buttonViewLocation: Button = itemView.findViewById(R.id.button_view_location)
        val imageMoreOption: ImageView = itemView.findViewById(R.id.image_more_option)

        init {
            Constants.sharedPref =
                itemView.context.getSharedPreferences("sharedPref", Context.MODE_PRIVATE)
            database = Constants.database
            imageMoreOption.setOnClickListener {
                popUpMenu(it)
            }
        }

        private fun popUpMenu(view: View) {
            val position = complaint[adapterPosition]
            val popUpMenu = PopupMenu(itemView.context, view)
            popUpMenu.inflate(R.menu.menu_report_complaint)
            popUpMenu.setOnMenuItemClickListener {
                when(it.itemId){
                    R.id.ic_report_complaint -> {
                        Dialog(view.context).apply {
                            requestWindowFeature(Window.FEATURE_NO_TITLE)
                            setCancelable(true)
                            setContentView(R.layout.layout_report_complaint)
                            val buttonSubmit = findViewById<Button>(R.id.button_submit)
                            val buttonCancel = findViewById<Button>(R.id.button_cancel)
                            val editTextReport = findViewById<EditText>(R.id.edit_text_report)
                            buttonSubmit.setOnClickListener {
                                if (!editTextReport.text.toString().trim().isNullOrEmpty()){
                                    reportComplaint(editTextReport.text.toString(), position, this)
                                }else{
                                    editTextReport.error = "Please write your reason for reporting this complaint."
                                    return@setOnClickListener
                                }
                            }
                            buttonCancel.setOnClickListener {
                                dismiss()
                            }
                            val layoutParams = WindowManager.LayoutParams()
                            layoutParams.copyFrom(window?.attributes)
                            layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
                            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
                            window?.attributes = layoutParams
                            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                            window?.setWindowAnimations(R.style.DialogAnimation)
                            show()
                        }
                        true
                    }
                    else -> true
                }
            }
            popUpMenu.show()
        }

        private fun reportComplaint(report: String, position: Complaint, dialog: Dialog) {
            reference = database.getReference("ReportedComplaint/${position.complaint_uid}")
            val uid = reference.push().key?:""
            val reportedComplaint = ReportedComplaint(position.complaint_uid,uid,report)
            reference.child(uid).setValue(reportedComplaint).addOnCompleteListener {
                if (it.isSuccessful){
                    loadAdminToken()
                    dialog.dismiss()
                    Dialog(itemView.context).apply {
                        requestWindowFeature(Window.FEATURE_NO_TITLE)
                        setCancelable(true)
                        setContentView(R.layout.layout_report_complaint_successful)
                        val buttonOK = findViewById<Button>(R.id.button_ok)
                        buttonOK.setOnClickListener {
                            dismiss()
                        }
                        val layoutParams = WindowManager.LayoutParams()
                        layoutParams.copyFrom(window?.attributes)
                        layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT
                        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
                        window?.attributes = layoutParams
                        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                        window?.setWindowAnimations(R.style.DialogAnimation)
                        show()
                    }
                }else{
                    dialog.dismiss()
                    val builder = AlertDialog.Builder(itemView.context)
                    builder.setTitle("Error")
                    builder.setMessage("Failed to report this complaint.")
                    builder.setCancelable(true)
                    builder.setPositiveButton("OK") { dialog, _ ->
                        dialog.dismiss()
                    }
                    val alert = builder.create()
                    alert.show()
                }
            }
        }
    }

    private fun loadAdminToken() {
        val title = "Complaint Reported"
        val message = "Someone has reported a complaint."

        reference = database.getReference("User")
        reference.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                adminList.clear()
                if (snapshot.exists()){
                    for (data in snapshot.children){
                        if (data.child("user_role").value.toString() == "Admin"){
                            adminList.add(data.child("user_uid").value.toString())
                            if (data.child("user_token").value.toString() != ""){
                                PushNotification(
                                    NotificationData(
                                        title, message
                                    ), data.child("user_token").value.toString()
                                ).also {
                                    sendNotification(it)
                                }
                            }
                        }
                    }
                }
                for (token in adminList){
                    registerNotification(title, message, token)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

    private fun registerNotification(title: String, message: String, userUID: String) {
        reference = database.getReference("Notification/$userUID")
        val notificationUID = reference.push().key?:""
        val currentDate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocalDateTime.now()
        } else {
            TODO("VERSION.SDK_INT < O")
        }
        val notificationCategory = 2 // Complaint Reported
        val notificationStatus = "Unread"
        val newNotification = Notification(notificationUID, title, message, currentDate.toString(), notificationCategory, notificationStatus, userUID)
        reference.child(notificationUID).setValue(newNotification)
    }

    private fun sendNotification(notification: PushNotification) = CoroutineScope(Dispatchers.IO).launch {
        try {
            val response = RetrofitInstance.api.postNotification(notification)
            if (response.isSuccessful){
                Log.d("Success", "Notification sent")
            }else{
                Log.d("Failed", "Notification unsent")
            }
        }catch (e: Exception){
            Log.e("Error", e.toString())
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.complaint_recycler_view, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Glide.with(holder.userImg.context)
            .load(complaint[position].user_avatar)
            .apply(Constants.requestOptions)
            .placeholder(R.drawable.ic_avatar)
            .error(R.drawable.errorimg)
            .into(holder.userImg)
        holder.userName.text = complaint[position].user_name
        if (complaint[position].complaint_priority == 1) {
            holder.textImportant.visibility = View.VISIBLE
            holder.textUrgent.visibility = View.GONE
        } else if (complaint[position].complaint_priority == 2) {
            holder.textImportant.visibility = View.GONE
            holder.textUrgent.visibility = View.VISIBLE
        } else {
            holder.textImportant.visibility = View.GONE
            holder.textUrgent.visibility = View.GONE
        }
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
        holder.postDateTime.text = formatted
        holder.textComplaintCategory.text = "#${complaint[position].complaint_category}"
        if (complaint[position].complaint_description == "") {
            holder.complaintDescription.visibility = View.GONE
        } else {
            holder.complaintDescription.visibility = View.VISIBLE
            holder.complaintDescription.text = complaint[position].complaint_description
        }

        Glide.with(holder.complaintImg.context)
            .load(complaint[position].complaint_photo)
            .apply(Constants.requestOptions)
            .placeholder(R.drawable.loadingimg)
            .error(R.drawable.errorimg)
            .into(holder.complaintImg)

        loadVerifiedUser(complaint[position].complaint_post_by, holder.verifiedUser)
        isUpVoted(complaint[position].complaint_uid, holder.buttonUpVote, holder.buttonDownVote)
        isDownVoted(complaint[position].complaint_uid, holder.buttonDownVote, holder.buttonUpVote)
        numOfUpVote(complaint[position].complaint_uid, holder.textUpVote)
        numOfDownVote(complaint[position].complaint_uid, holder.textDownVote)

        holder.cardAvatar.setOnClickListener {
            loadDialog(it, holder, position)
        }
        holder.userName.setOnClickListener {
            loadDialog(it, holder, position)
        }
        holder.textComplaintCategory.setOnClickListener {
            Intent(holder.itemView.context, ViewComplaintsActivity::class.java).apply {
                putExtra("complaint_category", complaint[position].complaint_category)
                holder.itemView.context.startActivity(this)
            }
        }
        holder.textImportant.setOnClickListener {
            Intent(holder.itemView.context, ViewComplaintsActivity::class.java).apply {
                putExtra("complaint_priority", complaint[position].complaint_priority.toString())
                holder.itemView.context.startActivity(this)
            }
        }
        holder.textUrgent.setOnClickListener {
            Intent(holder.itemView.context, ViewComplaintsActivity::class.java).apply {
                putExtra("complaint_priority", complaint[position].complaint_priority.toString())
                holder.itemView.context.startActivity(this)
            }
        }

        holder.buttonViewLocation.setOnClickListener {
            Intent(holder.itemView.context, ViewLocationActivity::class.java).apply {
                putExtra("latitude", complaint[position].complaint_latitude)
                putExtra("longitude", complaint[position].complaint_longitude)
                holder.itemView.context.startActivity(this)
            }
        }

        holder.textUpVote.setOnClickListener {
            loadVoteCount(holder.itemView.context, complaint[position].complaint_uid)
        }
        holder.textDownVote.setOnClickListener {
            loadVoteCount(holder.itemView.context, complaint[position].complaint_uid)
        }

        holder.buttonUpVote.setOnClickListener {
            val uid = FirebaseAuth.getInstance().uid
            referenceDownVote = database.getReference("DownVote")
            referenceUpVote = database.getReference("UpVote")

            if (holder.buttonUpVote.tag == "UpVote") {
                if (holder.buttonDownVote.tag == "DownVote") {
                    referenceUpVote.child("${complaint[position].complaint_uid}/$uid")
                        .setValue(true)
                } else {
                    referenceDownVote.child("${complaint[position].complaint_uid}/$uid")
                        .removeValue()
                    referenceUpVote.child("${complaint[position].complaint_uid}/$uid")
                        .setValue(true)
                }
            } else {
                referenceUpVote.child("${complaint[position].complaint_uid}/$uid").removeValue()
            }
        }
        holder.buttonDownVote.setOnClickListener {
            val uid = FirebaseAuth.getInstance().uid
            referenceDownVote = database.getReference("DownVote")
            referenceUpVote = database.getReference("UpVote")

            if (holder.buttonDownVote.tag == "DownVote") {
                if (holder.buttonUpVote.tag == "UpVote") {
                    referenceDownVote.child("${complaint[position].complaint_uid}/$uid")
                        .setValue(true)
                } else {
                    referenceUpVote.child("${complaint[position].complaint_uid}/$uid").removeValue()
                    referenceDownVote.child("${complaint[position].complaint_uid}/$uid")
                        .setValue(true)
                }
            } else {
                referenceDownVote.child("${complaint[position].complaint_uid}/$uid").removeValue()
            }
        }
    }

    private fun loadVoteCount(context: Context?, complaintUid: String) {
        reference = database.getReference("UpVote")
        reference.child(complaintUid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val upVoteCount = snapshot.childrenCount.toFloat()
                reference = database.getReference("DownVote")
                reference.child(complaintUid).addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val downVoteCount = snapshot.childrenCount.toFloat()
                        loadBarChartDialog(context, upVoteCount, downVoteCount)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        TODO("Not yet implemented")
                    }

                })
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

    private fun loadBarChartDialog(context: Context?, upVoteCount: Float, downVoteCount: Float) {
        Dialog(context!!).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setCancelable(true)
            setContentView(R.layout.layout_vote_chart)
            val layoutParams = WindowManager.LayoutParams()
            layoutParams.copyFrom(window?.attributes)
            layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
            window?.attributes = layoutParams
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window?.setWindowAnimations(R.style.DialogAnimation)
            showBarChart(this, context, upVoteCount, downVoteCount)
            val buttonBack = findViewById<ImageView>(R.id.image_back)
            buttonBack.setOnClickListener {
                dismiss()
            }
            show()
        }
    }

    private fun showBarChart(
        dialog: Dialog,
        context: Context,
        upVoteCount: Float,
        downVoteCount: Float
    ) {
        entries.clear()
        entries.add(BarEntry(0f, upVoteCount))
        entries.add(BarEntry(1f, downVoteCount))

        val barDataSet = BarDataSet(entries, "")
        barDataSet.setColors(*ColorTemplate.COLORFUL_COLORS)

        val data = BarData(barDataSet)
        val barChart: BarChart = dialog.findViewById(R.id.barChart)
        barChart.data = data
        barChart.data.notifyDataChanged()
        barChart.notifyDataSetChanged()

        barChart.invalidate()

        barChart.axisLeft.setDrawGridLines(false)
        val xAxis: XAxis = barChart.xAxis
        xAxis.setDrawGridLines(false)
        xAxis.setDrawAxisLine(false)

        //remove right y-axis
        barChart.axisRight.isEnabled = true

        //remove legend
        barChart.legend.isEnabled = false


        //remove description label
        barChart.description.isEnabled = false


        //add animation
        barChart.animateY(1500)

        // to draw label on xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.valueFormatter = MyAxisFormatter()
        xAxis.setDrawLabels(true)
        xAxis.granularity = 1f
    }


    inner class MyAxisFormatter : IndexAxisValueFormatter() {

        override fun getAxisLabel(value: Float, axis: AxisBase?): String {
            val index = value.toInt()
            return if (index == 0) "Upvote" else "Downvote"
        }
    }

    private fun loadDialog(view: View, holder: ViewHolder, position: Int) {
        Dialog(view.context).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setCancelable(true)
            setContentView(R.layout.layout_view_profile)
            val layoutParams = WindowManager.LayoutParams()
            layoutParams.copyFrom(window?.attributes)
            layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
            window?.attributes = layoutParams
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window?.setWindowAnimations(R.style.ViewProfileAnimation)
            showDialogViewProfile(this, holder.itemView, complaint[position].complaint_post_by)
            val buttonBack = findViewById<ImageView>(R.id.image_back)
            val buttonViewComplaint = findViewById<Button>(R.id.button_view_complaint)
            buttonBack.setOnClickListener {
                dismiss()
            }
            buttonViewComplaint.setOnClickListener {
                Intent(view.context, ViewComplaintsActivity::class.java).apply {
                    putExtra("user_uid", complaint[position].complaint_post_by)
                    view.context.startActivity(this)
                }
            }
            show()
        }
    }

    private fun loadVerifiedUser(complaintPostBy: String, verifiedUser: ImageView) {
        reference = database.getReference("User/$complaintPostBy")
        reference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    if (snapshot.child("user_nric").value.toString() != "" && snapshot.child("user_phone_num").value.toString() != "") {
                        verifiedUser.visibility = View.VISIBLE
                    } else {
                        verifiedUser.visibility = View.GONE
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

    private fun showDialogViewProfile(dialog: Dialog, itemView: View, complaintPostBy: String) {
        reference = database.getReference("User/$complaintPostBy")
        reference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val textUserName = dialog.findViewById<TextView>(R.id.text_user_name)
                    val textFullName = dialog.findViewById<TextView>(R.id.text_full_name)
                    val textEmail = dialog.findViewById<TextView>(R.id.text_email)
                    val textNric = dialog.findViewById<TextView>(R.id.text_nric)
                    val textPhoneNum = dialog.findViewById<TextView>(R.id.text_phone_num)
                    val imageAvatar = dialog.findViewById<ImageView>(R.id.image_avatar)
                    val imageVerifiedUser = dialog.findViewById<ImageView>(R.id.image_verified_user)
                    val imageNric = dialog.findViewById<ImageView>(R.id.image_nric)
                    val imagePhone = dialog.findViewById<ImageView>(R.id.image_phone)

                    textUserName.text = snapshot.child("user_name").value.toString()
                    textFullName.text = snapshot.child("user_full_name").value.toString()
                    textEmail.text = snapshot.child("user_email").value.toString()
                    textNric.text = snapshot.child("user_nric").value.toString()
                    textPhoneNum.text = snapshot.child("user_phone_num").value.toString()

                    Glide.with(imageAvatar.context)
                        .load(snapshot.child("user_avatar").value.toString())
                        .apply(Constants.requestOptions)
                        .placeholder(R.drawable.useravatar)
                        .error(R.drawable.errorimg)
                        .into(imageAvatar)

                    if (snapshot.exists()) {
                        if (snapshot.child("user_nric").value.toString() != "" && snapshot.child("user_phone_num").value.toString() != "") {
                            imageVerifiedUser.visibility = View.VISIBLE
                        } else {
                            imageVerifiedUser.visibility = View.GONE
                        }
                    }

                    if (Constants.userRole != "Resident") {
                        textNric.visibility = View.VISIBLE
                        textPhoneNum.visibility = View.VISIBLE
                        imageNric.visibility = View.VISIBLE
                        imagePhone.visibility = View.VISIBLE
                    } else {
                        textNric.visibility = View.GONE
                        textPhoneNum.visibility = View.GONE
                        imageNric.visibility = View.GONE
                        imagePhone.visibility = View.GONE
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

    private fun numOfDownVote(complaintUid: String, textDownVote: TextView) {
        reference = database.getReference("DownVote")
        reference.child("$complaintUid").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                downVoteList.add(snapshot.childrenCount.toInt())
                if (snapshot.childrenCount > 1) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        textDownVote.text =
                            "${
                                CompactDecimalFormat.getInstance(
                                    Locale.US,
                                    CompactDecimalFormat.CompactStyle.SHORT
                                ).format(snapshot.childrenCount)
                            } Downvotes"
                    }

                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        textDownVote.text =
                            "${
                                CompactDecimalFormat.getInstance(
                                    Locale.US,
                                    CompactDecimalFormat.CompactStyle.SHORT
                                ).format(snapshot.childrenCount)
                            } Downvote"
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

    private fun isDownVoted(
        complaintUid: String,
        buttonDownVote: MaterialButton,
        buttonUpVote: MaterialButton
    ) {
        val uid = Constants.userUID
        reference = database.getReference("DownVote/$complaintUid")
        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.child("$uid").exists()) {
                    buttonDownVote.setIconResource(R.drawable.ic_downvote)
                    buttonDownVote.setIconTintResource(R.color.Red_500)
                    buttonDownVote.setTextColor(Color.parseColor("#F44336"))
                    buttonDownVote.tag = "DownVoted"

                    buttonUpVote.setIconResource(R.drawable.ic_outline_upvote)
                    buttonUpVote.setTextColor(Color.parseColor("#999999"))
                    buttonUpVote.setIconTintResource(R.color.gray_200)
                    buttonUpVote.tag = "UpVote"
                } else {
                    buttonDownVote.setIconResource(R.drawable.ic_outline_downvote)
                    buttonDownVote.setTextColor(Color.parseColor("#999999"))
                    buttonDownVote.setIconTintResource(R.color.gray_200)
                    buttonDownVote.tag = "DownVote"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

    private fun numOfUpVote(complaintUID: String, upVoteCount: TextView) {
        reference = database.getReference("UpVote")
        reference.child("$complaintUID").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                upVoteList.add(snapshot.childrenCount.toInt())
                if (snapshot.childrenCount > 1) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        upVoteCount.text =
                            "${
                                CompactDecimalFormat.getInstance(
                                    Locale.US,
                                    CompactDecimalFormat.CompactStyle.SHORT
                                ).format(snapshot.childrenCount)
                            } Upvotes"
                    }
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        upVoteCount.text =
                            "${
                                CompactDecimalFormat.getInstance(
                                    Locale.US,
                                    CompactDecimalFormat.CompactStyle.SHORT
                                ).format(snapshot.childrenCount)
                            } Upvote"
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

    private fun isUpVoted(
        complaintUID: String,
        buttonUpVote: MaterialButton,
        buttonDownVote: MaterialButton
    ) {
        val uid = Constants.userUID
        reference = database.getReference("UpVote/$complaintUID")
        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.child("$uid").exists()) {
                    buttonUpVote.setIconResource(R.drawable.ic_upvote)
                    buttonUpVote.setIconTintResource(R.color.Primary_500)
                    buttonUpVote.setTextColor(Color.parseColor("#0066FF"))
                    buttonUpVote.tag = "UpVoted"

                    buttonDownVote.setIconResource(R.drawable.ic_outline_downvote)
                    buttonDownVote.setTextColor(Color.parseColor("#999999"))
                    buttonDownVote.setIconTintResource(R.color.gray_200)
                    buttonDownVote.tag = "DownVote"
                } else {
                    buttonUpVote.setIconResource(R.drawable.ic_outline_upvote)
                    buttonUpVote.setTextColor(Color.parseColor("#999999"))
                    buttonUpVote.setIconTintResource(R.color.gray_200)
                    buttonUpVote.tag = "UpVote"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

    override fun getItemCount(): Int {
        return complaint.size
    }

    override fun onCreateContextMenu(
        menu: ContextMenu?,
        v: View?,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        TODO("Not yet implemented")
    }

}