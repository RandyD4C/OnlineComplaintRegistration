package com.cyx.onlinecomplaintregistration.management.adapters

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.icu.text.CompactDecimalFormat
import android.net.Uri
import android.os.Build
import android.text.SpannableString
import android.text.style.UnderlineSpan
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


class ManagementComplaintRecyclerAdapter(
    private var complaint: List<Complaint>
) : RecyclerView.Adapter<ManagementComplaintRecyclerAdapter.ViewHolder>(), View.OnCreateContextMenuListener {

    private lateinit var database: FirebaseDatabase
    private lateinit var reference: DatabaseReference
    private var upVoteList = mutableListOf<Int>()
    private var downVoteList = mutableListOf<Int>()
    private var ownUserRole: String = ""
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
        val buttonHandle: MaterialButton = itemView.findViewById(R.id.button_handle)
        val cardAvatar: CardView = itemView.findViewById(R.id.card_avatar)
        val verifiedUser: ImageView = itemView.findViewById(R.id.image_verified_user)
        val buttonViewLocation: Button = itemView.findViewById(R.id.button_view_location)
        val imageMoreOption: ImageView = itemView.findViewById(R.id.image_more_option)

        init {
            Constants.sharedPref =
                itemView.context.getSharedPreferences("sharedPref", Context.MODE_PRIVATE)
            database = Constants.database
            loadOwnUserRole()
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
                        if (data.child("user_role").value.toString() == "Admin" && data.child("user_token").value.toString() != ""){
                            adminList.add(data.child("user_uid").value.toString())
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

    private fun loadOwnUserRole() {
        val uid = Constants.userUID
        reference = database.getReference("User/$uid")
        reference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    ownUserRole = snapshot.child("user_role").value.toString()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.complaint_management_recycler_view, parent, false)
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

        holder.buttonHandle.setOnClickListener {
            val builder = AlertDialog.Builder(it.context)
            builder.setMessage("Are you sure you want to handle this complaint?")
            builder.setTitle(complaint[position].complaint_category)
            builder.setCancelable(true)
            builder.setPositiveButton("Yes") { dialog, _ ->
                dialog.dismiss()
                checkManagedByIsEmpty(complaint[position].complaint_uid, complaint[position].complaint_post_by, it)
            }
            builder.setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            val alert = builder.create()
            alert.show()
        }

    }

    private fun checkManagedByIsEmpty(complaintUid: String, complaintPostBy: String, view: View) {
        reference = database.getReference("Complaint/$complaintPostBy/$complaintUid")
        reference.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()){
                    if (snapshot.child("complaint_managed_by").value.toString() == ""){
                        handleComplaint(complaintUid, complaintPostBy, view)
                    }else{
                        Toast.makeText(view.context, "This complaint's already been handled.", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

    private fun handleComplaint(complaintUid: String, complaintPostBy: String, view: View) {
        reference = database.getReference("Complaint")
        val userUID = Constants.userUID?:""
        val updateManagedBy = mapOf(
            "complaint_managed_by" to userUID
        )
        reference.child("$complaintPostBy/$complaintUid").updateChildren(updateManagedBy).addOnCompleteListener {
            if (it.isSuccessful){
                Dialog(view.context).apply {
                    notifyDataSetChanged()
                    requestWindowFeature(Window.FEATURE_NO_TITLE)
                    setCancelable(true)
                    setContentView(R.layout.layout_handle_complaint_successful)
                    val layoutParams = WindowManager.LayoutParams()
                    layoutParams.copyFrom(window?.attributes)
                    layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT
                    layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
                    window?.attributes = layoutParams
                    window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                    window?.setWindowAnimations(R.style.DialogAnimation)
                    val buttonOK = findViewById<Button>(R.id.button_ok)
                    buttonOK.setOnClickListener {
                        dismiss()
                    }
                    show()
                }
            }else{
                Toast.makeText(view.context, "An error occurred.", Toast.LENGTH_SHORT).show()
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
            val textEmail = findViewById<TextView>(R.id.text_email)
            val textPhoneNum = findViewById<TextView>(R.id.text_phone_num)
            textEmail.setOnClickListener {
                val intent = Intent(Intent.ACTION_SENDTO)
                intent.data = Uri.parse("mailto: ${textEmail.text}")
                intent.putExtra(Intent.EXTRA_SUBJECT, "")
                if (intent.resolveActivity(view.context.packageManager) != null) {
                    view.context.startActivity(intent)
                }
            }
            textPhoneNum.setOnClickListener {
                val dialIntent = Intent(Intent.ACTION_DIAL)
                dialIntent.data = Uri.parse("tel:" + textPhoneNum.text)
                view.context.startActivity(dialIntent)
            }
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
                    val underlinedPhoneNum = SpannableString(snapshot.child("user_phone_num").value.toString())
                    underlinedPhoneNum.setSpan(UnderlineSpan(), 0, underlinedPhoneNum.length, 0)
                    val underlinedEmail = SpannableString(snapshot.child("user_email").value.toString())
                    underlinedEmail.setSpan(UnderlineSpan(), 0, underlinedEmail.length, 0)

                    textUserName.text = snapshot.child("user_name").value.toString()
                    textFullName.text = snapshot.child("user_full_name").value.toString()
                    textNric.text = snapshot.child("user_nric").value.toString()
                    textEmail.text = underlinedEmail
                    textPhoneNum.text = underlinedPhoneNum

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

                    if (ownUserRole != "Resident") {
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