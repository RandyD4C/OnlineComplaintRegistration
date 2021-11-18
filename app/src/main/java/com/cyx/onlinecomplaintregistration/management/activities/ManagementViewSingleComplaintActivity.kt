package com.cyx.onlinecomplaintregistration.management.activities

import android.app.AlertDialog
import android.app.Dialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.icu.text.CompactDecimalFormat
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.*
import androidx.cardview.widget.CardView
import com.bumptech.glide.Glide
import com.cyx.onlinecomplaintregistration.R
import com.cyx.onlinecomplaintregistration.classes.*
import com.cyx.onlinecomplaintregistration.resident.activities.ResidentMainActivity
import com.cyx.onlinecomplaintregistration.resident.activities.home.ViewComplaintsActivity
import com.cyx.onlinecomplaintregistration.resident.activities.map.ViewLocationActivity
import com.cyx.onlinecomplaintregistration.resident.activities.user.MyAccountActivity
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_view_single_complaint.*
import kotlinx.android.synthetic.main.fragment_all_complaint_resident.*
import kotlinx.android.synthetic.main.fragment_all_complaint_resident.swipeToRefresh
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.Exception
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

class ManagementViewSingleComplaintActivity : AppCompatActivity() {

    private lateinit var userImg: ImageView
    private lateinit var userName: TextView
    private lateinit var complaintDescription: TextView
    private lateinit var postDateTime: TextView
    private lateinit var complaintImg: ImageView
    private lateinit var textUpVote: TextView
    private lateinit var textDownVote: TextView
    private lateinit var textUrgent: TextView
    private lateinit var textImportant: TextView
    private lateinit var textComplaintCategory: TextView
    private lateinit var imageVerifiedUser: ImageView
    private lateinit var buttonViewLocation: Button
    private var complaintUID: String = ""
    private var complaintPostBy: String = ""
    private var complaintCategory: String = ""
    private lateinit var database: FirebaseDatabase
    private lateinit var reference: DatabaseReference
    private lateinit var buttonMarkAsSolved: Button
    private lateinit var buttonDischarge: Button
    private lateinit var progressDialog: ProgressDialog
    private var upVoteCount = 0F
    private var downVoteCount = 0F
    private var latitude = 0.0
    private var longitude = 0.0
    private val entries = mutableListOf<BarEntry>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_management_complaint_post)
        this.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "View Complaint"

        userImg = findViewById(R.id.user_avatar)
        userName = findViewById(R.id.text_name)
        complaintDescription = findViewById(R.id.text_complaint_desc)
        postDateTime = findViewById(R.id.text_post_date)
        complaintImg = findViewById(R.id.image_complaint)
        textUpVote = findViewById(R.id.text_upvote)
        textDownVote = findViewById(R.id.text_down_vote)
        textUrgent = findViewById(R.id.text_urgent)
        textImportant = findViewById(R.id.text_important)
        textComplaintCategory = findViewById(R.id.text_complaint_category)
        imageVerifiedUser = findViewById(R.id.image_verified_user)
        buttonMarkAsSolved = findViewById(R.id.button_mark_as_solved)
        buttonDischarge = findViewById(R.id.button_discharge)
        buttonViewLocation = findViewById(R.id.button_view_location)

        complaintUID = intent.getStringExtra("complaint_uid")!!

        Constants.sharedPref =
            getSharedPreferences("sharedPref", Context.MODE_PRIVATE)
        database = Constants.database

        loadComplaintData()
        loadUpVoteCount()
        loadDownVoteCount()

        userImg.setOnClickListener {
            Intent(this, ViewComplaintsActivity::class.java).apply {
                putExtra("user_uid", complaintPostBy)
                startActivity(this)
            }
        }

        userName.setOnClickListener {
            Intent(this, ViewComplaintsActivity::class.java).apply {
                putExtra("user_uid", complaintPostBy)
                startActivity(this)
            }
        }

        imageVerifiedUser.setOnClickListener {
            val toast = Toast.makeText(this, "Verified User", Toast.LENGTH_SHORT)
            toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0)
            toast.show()
        }

        textUpVote.setOnClickListener {
            loadVoteChart()
        }

        textDownVote.setOnClickListener {
            loadVoteChart()
        }

        buttonViewLocation.setOnClickListener {
            val intent = Intent(this, ViewLocationActivity::class.java)
            intent.putExtra("latitude", latitude)
            intent.putExtra("longitude", longitude)
            startActivity(intent)
        }

        buttonMarkAsSolved.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("")
            builder.setMessage("Mark this complaint as SOLVED?")
            builder.setCancelable(true)
            builder.setPositiveButton("Yes") { dialog, _ ->
                dialog.dismiss()
                progressDialog = ProgressDialog(this)
                progressDialog.setMessage("Processing...")
                progressDialog.setCancelable(false)
                progressDialog.show()
                markAsSolved()
            }
            builder.setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            val alert = builder.create()
            alert.show()
        }
    }

    private fun markAsSolved() {
        reference = database.getReference("Complaint/$complaintPostBy/$complaintUID")
        val currentDate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocalDateTime.now()
        } else {
            TODO("VERSION.SDK_INT < O")
        }
        val updateStatus = mapOf<String, String>(
            "complaint_status" to "Done",
            "complaint_solved_date" to currentDate.toString()
        )
        reference.updateChildren(updateStatus)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    loadResidentToken(complaintPostBy, complaintCategory)
                    if (progressDialog.isShowing) progressDialog.dismiss()
                    Dialog(this).apply {
                        requestWindowFeature(Window.FEATURE_NO_TITLE)
                        setCancelable(false)
                        setContentView(R.layout.layout_solved_complaint_successful)
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
                            val intent = Intent(
                                this@ManagementViewSingleComplaintActivity,
                                ManagementMainActivity::class.java
                            )
                            intent.flags =
                                Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(intent)
                        }
                        show()
                    }
                }
            }
    }

    private fun loadResidentToken(complaintPostBy: String, complaintCategory: String) {
        val title = "$complaintCategory Solved"
        val message = "Your complaint has been solved!"

        reference = database.getReference("User/$complaintPostBy")
        reference.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()){
                    if (snapshot.child("user_token").value.toString() != ""){
                        PushNotification(
                            NotificationData(
                                title, message
                            ), snapshot.child("user_token").value.toString()
                        ).also {
                            sendNotification(it)
                        }
                    }
                }
                registerNotification(title, message, complaintPostBy)
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

    private fun registerNotification(title: String, message: String, complaintPostBy: String) {
        reference = database.getReference("Notification/$complaintPostBy")
        val notificationUID = reference.push().key?:""
        val currentDate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocalDateTime.now()
        } else {
            TODO("VERSION.SDK_INT < O")
        }
        val notificationCategory = 0 // Complaint Solved
        val notificationStatus = "Unread"
        val newNotification = Notification(notificationUID, title, message, currentDate.toString(), notificationCategory, notificationStatus, complaintPostBy)
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

    private fun loadVoteChart() {
        Dialog(this).apply {
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
            showBarChart(this)
            val buttonBack = findViewById<ImageView>(R.id.image_back)
            buttonBack.setOnClickListener {
                dismiss()
            }
            show()
        }
    }

    private fun showBarChart(dialog: Dialog) {
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

    override fun onResume() {
        super.onResume()
    }

    private fun loadDownVoteCount() {
        reference = database.getReference("DownVote")
        reference.child("$complaintUID")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    downVoteCount = snapshot.childrenCount.toFloat()
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

    private fun loadUpVoteCount() {
        reference = database.getReference("UpVote")
        reference.child("$complaintUID")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    upVoteCount = snapshot.childrenCount.toFloat()
                    if (snapshot.childrenCount > 1) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            textUpVote.text =
                                "${
                                    CompactDecimalFormat.getInstance(
                                        Locale.US,
                                        CompactDecimalFormat.CompactStyle.SHORT
                                    ).format(snapshot.childrenCount)
                                } Upvotes"
                        }
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            textUpVote.text =
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

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun loadComplaintData() {
        val uid = Constants.userUID
        reference = database.getReference("Complaint")
        reference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(data: DataSnapshot) {
                latitude = 0.0
                longitude = 0.0

                if (data.exists()) {
                    for (i in data.children){
                        for (snapshot in i.children){
                            if (
                                snapshot.child("complaint_uid").value.toString() == complaintUID &&
                                snapshot.child("complaint_managed_by").value.toString() == uid
                            ){
                                complaintPostBy = snapshot.child("complaint_post_by").value.toString()
                                complaintCategory = snapshot.child("complaint_category").value.toString()

                                try {
                                    Glide.with(userImg.context)
                                        .load(snapshot.child("user_avatar").value.toString())
                                        .apply(Constants.requestOptions)
                                        .placeholder(R.drawable.ic_avatar)
                                        .error(R.drawable.errorimg)
                                        .into(userImg)
                                } catch (e: Exception) {
                                    Log.e("Haha", e.message.toString())
                                }

                                latitude = snapshot.child("complaint_latitude").value.toString().toDouble()
                                longitude = snapshot.child("complaint_longitude").value.toString().toDouble()

                                //load user name
                                userName.text = snapshot.child("user_name").value.toString()

                                //load complaint date
                                val dateTime = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    LocalDateTime.parse(snapshot.child("complaint_date_time").value.toString())
                                } else {
                                    TODO("VERSION.SDK_INT < O")
                                }
                                val formatter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                                } else {
                                    TODO("VERSION.SDK_INT < O")
                                }
                                val formatted = dateTime.format(formatter)
                                postDateTime.text = formatted

                                //load complaint priority
                                if (snapshot.child("complaint_priority").value == 1) {
                                    textImportant.visibility = View.VISIBLE
                                    textUrgent.visibility = View.GONE
                                } else if (snapshot.child("complaint_priority").value == 2) {
                                    textImportant.visibility = View.GONE
                                    textUrgent.visibility = View.VISIBLE
                                } else {
                                    textImportant.visibility = View.GONE
                                    textUrgent.visibility = View.GONE
                                }

                                //load Complaint Category
                                textComplaintCategory.text =
                                    "#${snapshot.child("complaint_category").value.toString()}"

                                //load Complaint Description
                                complaintDescription.text =
                                    snapshot.child("complaint_description").value.toString()

                                //load Complaint Image
                                Glide.with(complaintImg.context)
                                    .load(snapshot.child("complaint_photo").value.toString())
                                    .apply(Constants.requestOptions)
                                    .placeholder(R.drawable.loadingimg)
                                    .error(R.drawable.errorimg)
                                    .into(complaintImg)

                            }
                        }
                    }
                } else {
                    val builder = AlertDialog.Builder(this@ManagementViewSingleComplaintActivity)
                    builder.setTitle("Error")
                    builder.setMessage("Unavailable complaint...")
                    builder.setCancelable(false)
                    builder.setPositiveButton("OK") { dialog, _ ->
                        dialog.dismiss()
                        onBackPressed()
                    }
                    val alert = builder.create()
                    alert.show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })

        loadUserData()
    }

    private fun loadUserData() {
        val uid = Constants.userUID
        reference = database.getReference("User/$uid")
        reference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    if (snapshot.child("user_nric").value.toString() != "" && snapshot.child("user_phone_num").value.toString() != "") {
                        imageVerifiedUser.visibility = View.VISIBLE
                    } else {
                        imageVerifiedUser.visibility = View.GONE
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }
}