package com.cyx.onlinecomplaintregistration.resident.fragments

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.icu.text.CompactDecimalFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.cyx.onlinecomplaintregistration.R
import com.cyx.onlinecomplaintregistration.classes.Complaint
import com.cyx.onlinecomplaintregistration.classes.Constants
import com.cyx.onlinecomplaintregistration.resident.activities.home.ComplaintListActivity
import com.cyx.onlinecomplaintregistration.resident.activities.home.ComplaintPostActivity
import com.cyx.onlinecomplaintregistration.resident.activities.home.RegisteringComplaintActivity
import com.cyx.onlinecomplaintregistration.resident.activities.user.MyAccountActivity
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.fragment_all_complaint_resident.*
import net.cachapa.expandablelayout.ExpandableLayout
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class MyComplaintFragment : Fragment() {

    private lateinit var cardAvatar: MaterialCardView
    private lateinit var imageViewAvatar: ImageView
    private lateinit var textUserName: TextView
    private lateinit var textUpVoteCount: TextView
    private lateinit var cardComplaintRegistered: CardView
    private lateinit var cardComplaintSolved: CardView
    private lateinit var cardComplaintRemoved: CardView
    private lateinit var textRegisteredCount: TextView
    private lateinit var textSolvedCount: TextView
    private lateinit var textRemovedCount: TextView
    private lateinit var imageVerifiedUser: ImageView
    private lateinit var buttonAddComplaint: FloatingActionButton
    private lateinit var expandableLayout0: ExpandableLayout
    private lateinit var expandableLayout1: ExpandableLayout
    private lateinit var expandableLayout2: ExpandableLayout
    private lateinit var database: FirebaseDatabase
    private lateinit var reference: DatabaseReference
    private lateinit var reference2: DatabaseReference
    private lateinit var photoFile: File
    private var complaintRegisteredList = mutableListOf<Complaint>()
    private var complaintSolvedList = mutableListOf<Complaint>()
    private var complaintRemovedList = mutableListOf<Complaint>()
    private var allComplaintList = mutableListOf<String>()
    private val REQUEST_PERM_WRITE_STORAGE = 1
    private val cameraRequest = 1
    private var registeredCount = 0
    private var solvedCount = 0
    private var removedCount = 0
    private var upVoteCont = 0
    private var downVoteCount = 0
    private lateinit var textNoComplaintRegistered: TextView
    private lateinit var textNoComplaintSolved: TextView
    private lateinit var textNoComplaintRemoved: TextView
    private lateinit var cardComplaintRegisteredExpanded: CardView
    private lateinit var cardComplaintSolvedExpanded: CardView
    private lateinit var cardComplaintRemovedExpanded: CardView
    private lateinit var buttonRegisteredList: MaterialButton
    private lateinit var buttonRegisteredPost: MaterialButton
    private lateinit var buttonSolvedList: MaterialButton
    private lateinit var buttonSolvedPost: MaterialButton
    private lateinit var buttonRemovedList: MaterialButton
    private lateinit var buttonRemovedPost: MaterialButton
    private lateinit var imageArrowRight: ImageView
    private lateinit var imageArrowRight2: ImageView
    private lateinit var imageArrowRight3: ImageView
    private lateinit var textDownVoteCount: TextView
    private lateinit var imageMyComplaintInfo: ImageView
    private lateinit var imageUpVote: ImageView
    private lateinit var imageDownVote: ImageView
    private val entries = mutableListOf<BarEntry>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_my_complaint, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        cardAvatar = view.findViewById(R.id.card_avatar)
        imageViewAvatar = view.findViewById(R.id.image_view_avatar)
        textUserName = view.findViewById(R.id.text_user_name)
        textUpVoteCount = view.findViewById(R.id.text_upvote_count)
        textDownVoteCount = view.findViewById(R.id.text_down_vote_count)
        cardComplaintRegistered = view.findViewById(R.id.card_view_complaint_registered)
        cardComplaintSolved = view.findViewById(R.id.card_view_complaint_solved)
        cardComplaintRemoved = view.findViewById(R.id.card_view_complaint_removed)
        textRegisteredCount = view.findViewById(R.id.text_registered_count)
        textSolvedCount = view.findViewById(R.id.text_solved_count)
        textRemovedCount = view.findViewById(R.id.text_removed_count)
        imageVerifiedUser = view.findViewById(R.id.image_verified_user)
        buttonAddComplaint = view.findViewById(R.id.button_add_complaint)
        expandableLayout0 = view.findViewById(R.id.expandable_layout_0)
        expandableLayout1 = view.findViewById(R.id.expandable_layout_1)
        expandableLayout2 = view.findViewById(R.id.expandable_layout_2)
        cardComplaintRegisteredExpanded = view.findViewById(R.id.card_complaint_registered_expanded)
        cardComplaintSolvedExpanded = view.findViewById(R.id.card_complaint_solved_expanded)
        cardComplaintRemovedExpanded = view.findViewById(R.id.card_complaint_removed_expanded)
        buttonRegisteredList = view.findViewById(R.id.button_registered_list)
        buttonRegisteredPost = view.findViewById(R.id.button_registered_post)
        buttonSolvedList = view.findViewById(R.id.button_solved_list)
        buttonSolvedPost = view.findViewById(R.id.button_solved_post)
        buttonRemovedList = view.findViewById(R.id.button_removed_list)
        buttonRemovedPost = view.findViewById(R.id.button_removed_post)
        textNoComplaintRegistered = view.findViewById(R.id.text_no_complaint_registered)
        textNoComplaintSolved = view.findViewById(R.id.text_no_complaint_solved)
        textNoComplaintRemoved = view.findViewById(R.id.text_no_complaint_removed)
        imageArrowRight = view.findViewById(R.id.image_arrow_right)
        imageArrowRight2 = view.findViewById(R.id.image_arrow_right_2)
        imageArrowRight3 = view.findViewById(R.id.image_arrow_right_3)
        imageMyComplaintInfo = view.findViewById(R.id.image_my_complaint_info)
        imageUpVote = view.findViewById(R.id.image_upvote)
        imageDownVote = view.findViewById(R.id.image_downvote)

        Constants.sharedPref =
            view.context.getSharedPreferences("sharedPref", Context.MODE_PRIVATE)
        textUserName.text = Constants.userName

        database = Constants.database
        refreshApp()
        loadUserData()
        loadComplaint()

        imageMyComplaintInfo.setOnClickListener {
            Dialog(view.context).apply {
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                setCancelable(true)
                setContentView(R.layout.layout_my_complaint_info)
                val buttonOK = findViewById<Button>(R.id.button_ok)
                buttonOK.setOnClickListener {
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
        }

        textUpVoteCount.setOnClickListener {
            loadVoteChart()
        }
        textDownVoteCount.setOnClickListener {
            loadVoteChart()
        }
        imageDownVote.setOnClickListener {
            loadVoteChart()
        }
        imageUpVote.setOnClickListener {
            loadVoteChart()
        }

        cardAvatar.setOnClickListener {
            val intent = Intent(view.context, MyAccountActivity::class.java)
            view.context.startActivity(intent)
        }

        textUserName.setOnClickListener {
            val intent = Intent(view.context, MyAccountActivity::class.java)
            view.context.startActivity(intent)
        }

        imageVerifiedUser.setOnClickListener {
            val toast = Toast.makeText(view.context, "Verified User", Toast.LENGTH_SHORT)
            toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0)
            toast.show()
        }

        buttonRegisteredList.setOnClickListener {
            val intent = Intent(view.context, ComplaintListActivity::class.java)
            intent.putExtra("complaint_status", "Registered")
            view.context.startActivity(intent)
        }

        buttonSolvedList.setOnClickListener {
            val intent = Intent(view.context, ComplaintListActivity::class.java)
            intent.putExtra("complaint_status", "Solved")
            view.context.startActivity(intent)
        }

        buttonRemovedList.setOnClickListener {
            val intent = Intent(view.context, ComplaintListActivity::class.java)
            intent.putExtra("complaint_status", "Removed")
            view.context.startActivity(intent)
        }
        buttonRegisteredPost.setOnClickListener {
            val intent = Intent(view.context, ComplaintPostActivity::class.java)
            intent.putExtra("complaint_status", "Registered")
            view.context.startActivity(intent)
        }

        buttonSolvedPost.setOnClickListener {
            val intent = Intent(view.context, ComplaintPostActivity::class.java)
            intent.putExtra("complaint_status", "Solved")
            view.context.startActivity(intent)
        }

        buttonRemovedPost.setOnClickListener {
            val intent = Intent(view.context, ComplaintPostActivity::class.java)
            intent.putExtra("complaint_status", "Removed")
            view.context.startActivity(intent)
        }

        cardComplaintRegistered.setOnClickListener {
            if (expandableLayout0.isExpanded) {
                expandableLayout0.collapse()
                imageArrowRight.rotation = 0F
            } else {
                expandableLayout0.expand()
                expandableLayout1.collapse()
                expandableLayout2.collapse()

                imageArrowRight.rotation = 90F
                imageArrowRight2.rotation = 0F
                imageArrowRight3.rotation = 0F
            }
        }
        cardComplaintSolved.setOnClickListener {
            if (expandableLayout1.isExpanded) {
                expandableLayout1.collapse()
                imageArrowRight2.rotation = 0F
            } else {
                expandableLayout0.collapse()
                expandableLayout1.expand()
                expandableLayout2.collapse()

                imageArrowRight.rotation = 0F
                imageArrowRight2.rotation = 90F
                imageArrowRight3.rotation = 0F
            }
        }
        cardComplaintRemoved.setOnClickListener {
            if (expandableLayout2.isExpanded) {
                expandableLayout2.collapse()
                imageArrowRight3.rotation = 0F
            } else {
                expandableLayout0.collapse()
                expandableLayout1.collapse()
                expandableLayout2.expand()

                imageArrowRight.rotation = 0F
                imageArrowRight2.rotation = 0F
                imageArrowRight3.rotation = 90F
            }
        }
        buttonAddComplaint.setOnLongClickListener {
            it.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
            return@setOnLongClickListener false
        }
        buttonAddComplaint.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.CAMERA
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        requireActivity(),
                        arrayOf(Manifest.permission.CAMERA),
                        REQUEST_PERM_WRITE_STORAGE
                    )
                } else {
                    val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    photoFile = getOutputDirectory()
                    val fileProvider = FileProvider.getUriForFile(
                        requireContext(),
                        "com.cyx.onlinecomplaintregistration.fileprovider",
                        photoFile
                    )
                    cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileProvider)
                    cameraIntent.resolveActivity(view.context.packageManager)?.let {
                        startActivityForResult(cameraIntent, cameraRequest)
                    } ?: Toast.makeText(view.context, "Unable to open camera", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    private fun loadVoteChart() {
        Dialog(requireContext()).apply {
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
        entries.add(BarEntry(0f, upVoteCont.toFloat()))
        entries.add(BarEntry(1f, downVoteCount.toFloat()))

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

    private fun refreshApp() {
        swipeToRefresh.setOnRefreshListener {
            loadUserData()
            loadComplaint()
            Toast.makeText(requireContext(), "Page Refreshed.", Toast.LENGTH_SHORT).show()
            swipeToRefresh.isRefreshing = false
        }
    }

    private fun loadUserData() {
        val uid = Constants.userUID
        reference = database.getReference("User/$uid")
        reference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                //load avatar
                try {
                    Glide.with(imageViewAvatar.context)
                        .load(snapshot.child("user_avatar").value.toString())
                        .apply(Constants.requestOptions)
                        .placeholder(R.drawable.ic_avatar)
                        .error(R.drawable.errorimg)
                        .into(imageViewAvatar)
                }catch (e: Exception){
                    Log.d("Haha", e.message.toString())
                }

                if (snapshot.child("user_nric").value.toString() != "" && snapshot.child("user_phone_num").value.toString() != "") {
                    imageVerifiedUser.visibility = View.VISIBLE
                }else{
                    imageVerifiedUser.visibility = View.GONE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

    private fun loadUpVoteCount() {
        reference = database.getReference("UpVote")
        reference2 = database.getReference("DownVote")
        upVoteCont = 0
        downVoteCount = 0
        var i = 0
        for (complaint in allComplaintList) {
            reference.child(complaint).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    upVoteCont += snapshot.childrenCount.toInt()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        textUpVoteCount.text = CompactDecimalFormat.getInstance(Locale.US, CompactDecimalFormat.CompactStyle.SHORT).format(upVoteCont)
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }
            })
            reference2.child(complaint).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    downVoteCount += snapshot.childrenCount.toInt()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        textDownVoteCount.text = CompactDecimalFormat.getInstance(Locale.US, CompactDecimalFormat.CompactStyle.SHORT).format(downVoteCount)
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }
            })
        }
    }

    private fun loadComplaint() {
        val uid = Constants.userUID
        reference = database.getReference("Complaint/$uid")
        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                complaintRegisteredList.clear()
                complaintSolvedList.clear()
                complaintRemovedList.clear()
                allComplaintList.clear()
                registeredCount = 0
                solvedCount = 0
                removedCount = 0

                if (snapshot.exists()) {
                    for (data in snapshot.children) {
                        if (data.child("complaint_status").value.toString() == "Pending") {
                            val complaint = data.getValue(Complaint::class.java)
                            complaintRegisteredList.add(complaint!!)
                            registeredCount++
                        } else if (data.child("complaint_status").value.toString() == "Done") {
                            val complaint = data.getValue(Complaint::class.java)
                            complaintSolvedList.add(complaint!!)
                            solvedCount++
                        } else {
                            val complaint = data.getValue(Complaint::class.java)
                            complaintRemovedList.add(complaint!!)
                            removedCount++
                        }
                        allComplaintList.add(data.child("complaint_uid").value.toString())
                    }
                    loadUpVoteCount()
                }
                if (registeredCount > 0){
                    cardComplaintRegisteredExpanded.visibility = View.VISIBLE
                    textNoComplaintRegistered.visibility = View.GONE
                }else{
                    cardComplaintRegisteredExpanded.visibility = View.GONE
                    textNoComplaintRegistered.visibility = View.VISIBLE
                }
                if (solvedCount > 0){
                    cardComplaintSolvedExpanded.visibility = View.VISIBLE
                    textNoComplaintSolved.visibility = View.GONE
                }else{
                    cardComplaintSolvedExpanded.visibility = View.GONE
                    textNoComplaintSolved.visibility = View.VISIBLE
                }
                if (removedCount > 0){
                    cardComplaintRemovedExpanded.visibility = View.VISIBLE
                    textNoComplaintRemoved.visibility = View.GONE
                }else{
                    cardComplaintRemovedExpanded.visibility = View.GONE
                    textNoComplaintRemoved.visibility = View.VISIBLE
                }

                textRegisteredCount.text = registeredCount.toString()
                textSolvedCount.text = solvedCount.toString()
                textRemovedCount.text = removedCount.toString()
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

    private fun getOutputDirectory(): File {
        val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val fileName = SimpleDateFormat(
            Constants.FILE_NAME_FORMAT,
            Locale.getDefault()
        ).format(System.currentTimeMillis())
        return File.createTempFile(fileName, ".jpg", storageDir)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == cameraRequest && resultCode == Activity.RESULT_OK) {
            val intent = Intent(requireContext(), RegisteringComplaintActivity::class.java)
            intent.putExtra("imageUri", Uri.fromFile(photoFile.absoluteFile)!!)
            requireContext().startActivity(intent)
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
}