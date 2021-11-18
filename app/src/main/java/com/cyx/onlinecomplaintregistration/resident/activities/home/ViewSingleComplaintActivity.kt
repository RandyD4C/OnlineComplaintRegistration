package com.cyx.onlinecomplaintregistration.resident.activities.home

import android.app.AlertDialog
import android.app.Dialog
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
import com.cyx.onlinecomplaintregistration.classes.Constants
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
import java.lang.Exception
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

class ViewSingleComplaintActivity : AppCompatActivity() {

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
    private lateinit var database: FirebaseDatabase
    private lateinit var reference: DatabaseReference
    private lateinit var buttonEdit: Button
    private lateinit var buttonDelete: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var cardNewChanges: CardView
    private var isNewChanges = false
    private var upVoteCount = 0F
    private var downVoteCount = 0F
    private var latitude = 0.0
    private var longitude = 0.0
    private val entries = mutableListOf<BarEntry>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_single_complaint)
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
        buttonEdit = findViewById(R.id.button_edit)
        buttonDelete = findViewById(R.id.button_delete)
        progressBar = findViewById(R.id.progress_bar)
        cardNewChanges = findViewById(R.id.card_new_changes)
        buttonViewLocation = findViewById(R.id.button_view_location)

        complaintUID = intent.getStringExtra("complaint_uid")!!

        Constants.sharedPref =
            getSharedPreferences("sharedPref", Context.MODE_PRIVATE)
        database = Constants.database
        loadComplaintData()
        loadUpVoteCount()
        loadDownVoteCount()
        refreshApp()
        loadNewChanges()

        cardNewChanges.setOnClickListener {
            cardNewChanges.visibility = View.GONE
            progressBar.visibility = View.VISIBLE
            isNewChanges = false
            loadComplaintData()
            loadUpVoteCount()
            loadDownVoteCount()
        }

        userImg.setOnClickListener {
            val intent = Intent(this, MyAccountActivity::class.java)
            startActivity(intent)
        }

        userName.setOnClickListener {
            val intent = Intent(this, MyAccountActivity::class.java)
            startActivity(intent)
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

        buttonEdit.setOnClickListener {
            val intent = Intent(this, EditComplaintActivity::class.java)
            intent.putExtra("complaint_uid", complaintUID)
            startActivity(intent)
        }

        buttonDelete.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Delete Complaint")
            builder.setMessage("Are you sure you want to DELETE this Complaint?")
            builder.setCancelable(true)
            builder.setPositiveButton("Yes") { dialog, _ ->
                dialog.dismiss()
                deleteComplaint()
            }
            builder.setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            val alert = builder.create()
            alert.show()
        }

        buttonViewLocation.setOnClickListener {
            val intent = Intent(this, ViewLocationActivity::class.java)
            intent.putExtra("latitude", latitude)
            intent.putExtra("longitude", longitude)
            startActivity(intent)
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

    private fun loadNewChanges() {
        val uid = Constants.userUID
        reference = database.getReference("Complaint/$uid/$complaintUID")
        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (isNewChanges) {
                    cardNewChanges.visibility = View.VISIBLE
                } else {
                    cardNewChanges.visibility = View.GONE
                }
                isNewChanges = true
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

    private fun deleteComplaint() {
        val uid = Constants.userUID
        reference = database.getReference("Complaint/$uid/$complaintUID")
        reference.removeValue().addOnCompleteListener {
            if (it.isSuccessful) {
                val reference = database.getReference("UpVote/$complaintUID")
                reference.removeValue().addOnCompleteListener {
                    val reference = database.getReference("DownVote/$complaintUID")
                    reference.removeValue().addOnCompleteListener {
                        val builder = AlertDialog.Builder(this)
                        builder.setTitle("Delete Successfully")
                        builder.setMessage("This Complaint has been DELETED.")
                        builder.setCancelable(false)
                        builder.setPositiveButton("OK") { dialog, _ ->
                            dialog.dismiss()
                            super.onBackPressed()
                        }
                        val alert = builder.create()
                        alert.show()
                    }
                }
            }
        }
    }

    private fun refreshApp() {
        swipeToRefresh.setOnRefreshListener {
            cardNewChanges.visibility = View.GONE
            loadComplaintData()
            loadUpVoteCount()
            loadDownVoteCount()
            Toast.makeText(this, "Page Refreshed.", Toast.LENGTH_SHORT).show()
            swipeToRefresh.isRefreshing = false
        }
    }

    override fun onResume() {
        super.onResume()
        refreshApp()
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
        reference = database.getReference("Complaint/$uid/$complaintUID")
        reference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                latitude = 0.0
                longitude = 0.0

                if (snapshot.exists()) {
                    //load avatar
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
                    if (snapshot.child("complaint_priority").value.toString() == "1") {
                        textImportant.visibility = View.VISIBLE
                        textUrgent.visibility = View.GONE
                    } else if (snapshot.child("complaint_priority").value.toString() == "2") {
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

                    progressBar.visibility = View.GONE
                } else {
                    progressBar.visibility = View.GONE
                    val builder = AlertDialog.Builder(this@ViewSingleComplaintActivity)
                    builder.setTitle("Error")
                    builder.setMessage("This complaint maybe DELETED or has been REMOVED.")
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