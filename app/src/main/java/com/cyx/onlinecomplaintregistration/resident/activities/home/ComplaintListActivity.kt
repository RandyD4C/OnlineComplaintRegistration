package com.cyx.onlinecomplaintregistration.resident.activities.home

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.cyx.onlinecomplaintregistration.R
import com.cyx.onlinecomplaintregistration.classes.Complaint
import com.cyx.onlinecomplaintregistration.classes.Constants
import com.cyx.onlinecomplaintregistration.resident.adapters.ComplaintRegisteredListRecyclerAdapter
import com.cyx.onlinecomplaintregistration.resident.adapters.ComplaintRemovedListRecyclerAdapter
import com.cyx.onlinecomplaintregistration.resident.adapters.ComplaintSolvedListRecyclerAdapter
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.fragment_all_complaint_resident.*
import net.cachapa.expandablelayout.ExpandableLayout


class ComplaintListActivity : AppCompatActivity() {

    private lateinit var database: FirebaseDatabase
    private lateinit var reference: DatabaseReference
    private var complaintRegisteredList = mutableListOf<Complaint>()
    private var complaintSolvedList = mutableListOf<Complaint>()
    private var complaintRemovedList = mutableListOf<Complaint>()
    private var complaintPendingList = mutableListOf<Complaint>()
    private var complaintSortedPriorityList = mutableListOf<Complaint>()
    private var complaintSortedPostalCodeList = mutableListOf<Complaint>()
    private var complaintSortedCategoryList = mutableListOf<Complaint>()
    private lateinit var linearLayoutManager: LinearLayoutManager
    private var complaintStatus: String = ""
    private lateinit var complaintRegisteredAdapter: ComplaintRegisteredListRecyclerAdapter
    private lateinit var complaintSolvedAdapter: ComplaintSolvedListRecyclerAdapter
    private lateinit var complaintRemovedAdapter: ComplaintRemovedListRecyclerAdapter
    private lateinit var complaintPendingAdapter: ComplaintRegisteredListRecyclerAdapter
    private lateinit var imageRefresh: ImageView
    private lateinit var recyclerView: RecyclerView
    private lateinit var expandedGoToTop: ExpandableLayout
    private lateinit var expandedTopCard: ExpandableLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var textNoComplaint: TextView
    private lateinit var imageSortBy: ImageView
    private lateinit var imageFilter: ImageView
    private var checkBoxCategoryList = mutableListOf<CheckBox>()
    private var zipCodeKampar = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_complaint_registered_list)
        this.supportActionBar?.setDisplayHomeAsUpEnabled(true)

        Constants.sharedPref =
            getSharedPreferences("sharedPref", Context.MODE_PRIVATE)
        database = Constants.database
        complaintStatus = intent.getStringExtra("complaint_status")!!
        if (complaintStatus == "Registered") {
            recyclerView = findViewById(R.id.recycler_view_complaint_registered)
        } else if (complaintStatus == "Solved") {
            recyclerView = findViewById(R.id.recycler_view_complaint_solved)
        } else if (complaintStatus == "Removed") {
            recyclerView = findViewById(R.id.recycler_view_complaint_removed)
        }else if (complaintStatus == "Pending"){
            recyclerView = findViewById(R.id.recycler_view_complaint_pending)
        }

        imageRefresh = findViewById(R.id.image_refresh)
        expandedGoToTop = findViewById(R.id.expanded_go_to_top)
        expandedTopCard = findViewById(R.id.expanded_top_card)
        progressBar = findViewById(R.id.progress_bar)
        textNoComplaint = findViewById(R.id.text_no_complaint)
        imageSortBy = findViewById(R.id.image_sort_by)
        imageFilter = findViewById(R.id.image_filter)

        zipCodeKampar.apply {
            clear()
            add("31700")
            add("31900")
            add("31910")
            add("31950")
            add("35350")
        }

        loadComplaint()

        imageRefresh.setOnClickListener {
            progressBar.visibility = View.VISIBLE
            recyclerView.removeAllViewsInLayout()
            loadComplaint()
        }
        expandedGoToTop.setOnClickListener {
            recyclerView.smoothSnapToPosition(0)
            expandedGoToTop.collapse()
            expandedTopCard.expand()
        }
        imageFilter.setOnClickListener {
            Dialog(it.context).apply {
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                setCancelable(true)
                setContentView(R.layout.layout_filter)
                val rootContainer = findViewById<LinearLayout>(R.id.root_container)
                val layoutParams = WindowManager.LayoutParams()
                layoutParams.copyFrom(window?.attributes)
                layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
                layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
                window?.attributes = layoutParams
                window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                window?.setWindowAnimations(R.style.DialogAnimation)
                loadCategoryCheckBox(this, rootContainer)
                loadDialogFilter(this)
                show()
            }
        }
        imageSortBy.setOnClickListener {
            Dialog(it.context).apply {
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                setCancelable(true)
                setContentView(R.layout.layout_sort_by)
                val layoutParams = WindowManager.LayoutParams()
                layoutParams.copyFrom(window?.attributes)
                layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
                layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
                window?.attributes = layoutParams
                window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                window?.setWindowAnimations(R.style.DialogAnimation)
                loadDialogSortBy(this)
                show()
            }
        }
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 20) {
                    //Scrolling down
                    expandedGoToTop.expand()
                    expandedTopCard.collapse()
                } else if (dy < 0) {
                    //Scrolling up
                    expandedGoToTop.collapse()
                    expandedTopCard.expand()
                }
            }
        })
    }

    private fun loadDialogFilter(dialog: Dialog) {
        val buttonCancel: Button = dialog.findViewById(R.id.button_cancel)
        val buttonApply: Button = dialog.findViewById(R.id.button_apply)
        buttonApply.setOnClickListener {
            applyFilter(dialog)
            dialog.dismiss()
        }
        buttonCancel.setOnClickListener {
            dialog.dismiss()
        }
    }

    private fun applyFilter(dialog: Dialog) {
        val uid = Constants.userUID

        reference = database.getReference("Complaint")
        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                complaintRegisteredList.clear()
                complaintSolvedList.clear()
                complaintRemovedList.clear()
                complaintPendingList.clear()

                if (snapshot.exists()) {
                    for (i in snapshot.children){
                        for (data in i.children) {
                            if (complaintStatus == "Registered") {
                                if (data.child("complaint_status").value.toString() == "Pending" && data.child("complaint_post_by").value.toString() == uid) {
                                    val complaint = data.getValue(Complaint::class.java)
                                    complaintRegisteredList.add(complaint!!)
                                }
                            } else if (complaintStatus == "Solved") {
                                if (data.child("complaint_status").value.toString() == "Done" && data.child("complaint_post_by").value.toString() == uid) {
                                    val complaint = data.getValue(Complaint::class.java)
                                    complaintSolvedList.add(complaint!!)
                                }
                            } else if (complaintStatus == "Removed") {
                                if (data.child("complaint_status").value.toString() == "Removed" && data.child("complaint_post_by").value.toString() == uid) {
                                    val complaint = data.getValue(Complaint::class.java)
                                    complaintRemovedList.add(complaint!!)
                                }
                            } else if (complaintStatus == "Pending") {
                                if (data.child("complaint_status").value.toString() == "Pending" && data.child("complaint_managed_by").value.toString() == uid) {
                                    val complaint = data.getValue(Complaint::class.java)
                                    complaintPendingList.add(complaint!!)
                                }
                            }
                        }
                    }
                    try {
                        // Complaint Registered
                        if (complaintRegisteredList.size > 0 || complaintSolvedList.size > 0 || complaintRemovedList.size > 0 || complaintPendingList.size > 0) {
                            progressBar.visibility = View.GONE
                            if (complaintRegisteredList.size > 0 && complaintStatus == "Registered") {

                                filterComplaintList(complaintRegisteredList, dialog)

                                supportActionBar?.title =
                                    "Complaints Registered (${complaintSortedCategoryList.size})"
                                recyclerView.visibility = View.VISIBLE
                                linearLayoutManager = LinearLayoutManager(this@ComplaintListActivity)
                                recyclerView.layoutManager = linearLayoutManager
                                complaintSortedCategoryList.sortByDescending { it.complaint_date_time }
                                complaintRegisteredAdapter =
                                    ComplaintRegisteredListRecyclerAdapter(complaintSortedCategoryList)
                                complaintRegisteredAdapter.notifyDataSetChanged()
                                recyclerView.adapter = complaintRegisteredAdapter
                            }
                            if (complaintSolvedList.size > 0 && complaintStatus == "Solved") {

                                filterComplaintList(complaintSolvedList, dialog)

                                supportActionBar?.title =
                                    "Complaints Solved (${complaintSortedCategoryList.size})"
                                recyclerView.visibility = View.VISIBLE
                                linearLayoutManager = LinearLayoutManager(this@ComplaintListActivity)
                                recyclerView.layoutManager = linearLayoutManager
                                complaintSortedCategoryList.sortByDescending { it.complaint_date_time }
                                complaintSolvedAdapter =
                                    ComplaintSolvedListRecyclerAdapter(complaintSortedCategoryList)
                                complaintSolvedAdapter.notifyDataSetChanged()
                                recyclerView.adapter = complaintSolvedAdapter
                            }
                            if (complaintRemovedList.size > 0 && complaintStatus == "Removed") {

                                filterComplaintList(complaintRemovedList, dialog)

                                supportActionBar?.title =
                                    "Complaints Removed (${complaintSortedCategoryList.size})"
                                recyclerView.visibility = View.VISIBLE
                                linearLayoutManager = LinearLayoutManager(this@ComplaintListActivity)
                                recyclerView.layoutManager = linearLayoutManager
                                complaintSortedCategoryList.sortByDescending { it.complaint_date_time }
                                complaintRemovedAdapter =
                                    ComplaintRemovedListRecyclerAdapter(complaintSortedCategoryList)
                                complaintRemovedAdapter.notifyDataSetChanged()
                                recyclerView.adapter = complaintRemovedAdapter
                            }
                            if (complaintPendingList.size > 0 && complaintStatus == "Pending") {

                                filterComplaintList(complaintPendingList, dialog)

                                supportActionBar?.title =
                                    "Complaints Handled (${complaintSortedCategoryList.size})"
                                recyclerView.visibility = View.VISIBLE
                                linearLayoutManager = LinearLayoutManager(this@ComplaintListActivity)
                                recyclerView.layoutManager = linearLayoutManager
                                complaintSortedCategoryList.sortByDescending { it.complaint_date_time }
                                complaintPendingAdapter =
                                    ComplaintRegisteredListRecyclerAdapter(complaintSortedCategoryList)
                                complaintPendingAdapter.notifyDataSetChanged()
                                recyclerView.adapter = complaintPendingAdapter
                            }
                        }else{
                            progressBar.visibility = View.GONE
                            textNoComplaint.visibility = View.VISIBLE
                        }

                    } catch (e: IllegalStateException) {
                        Log.e("Nono", e.message.toString())
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

    private fun filterComplaintList(complaintList: MutableList<Complaint>, dialog: Dialog) {
        val checkBoxNeutral: CheckBox = dialog.findViewById(R.id.check_box_neutral)
        val checkBoxImportant: CheckBox = dialog.findViewById(R.id.check_box_important)
        val checkBoxUrgent: CheckBox = dialog.findViewById(R.id.check_box_urgent)
        val checkBoxKampar: CheckBox = dialog.findViewById(R.id.check_box_kampar)

        complaintSortedPriorityList.clear()
        complaintSortedPostalCodeList.clear()
        complaintSortedCategoryList.clear()

        if (!checkBoxNeutral.isChecked) {
            for (i in complaintList.indices) {
                if (checkBoxImportant.isChecked && checkBoxUrgent.isChecked) {
                    if (complaintList[i].complaint_priority == 1 || complaintList[i].complaint_priority == 2) {
                        complaintSortedPriorityList.add(complaintList[i])
                    }
                } else if (checkBoxImportant.isChecked) {
                    if (complaintList[i].complaint_priority == 1) {
                        complaintSortedPriorityList.add(complaintList[i])
                    }
                } else if (checkBoxUrgent.isChecked) {
                    if (complaintList[i].complaint_priority == 2) {
                        complaintSortedPriorityList.add(complaintList[i])
                    }
                }else{
                    complaintSortedPriorityList.add(complaintList[i])
                }
            }
        }
        if (!checkBoxImportant.isChecked) {
            for (i in complaintList.indices) {
                if (checkBoxNeutral.isChecked && checkBoxUrgent.isChecked) {
                    if (complaintList[i].complaint_priority == 0 || complaintList[i].complaint_priority == 2) {
                        complaintSortedPriorityList.add(complaintList[i])
                    }
                } else if (checkBoxNeutral.isChecked) {
                    if (complaintList[i].complaint_priority == 0) {
                        complaintSortedPriorityList.add(complaintList[i])
                    }
                } else if (checkBoxUrgent.isChecked) {
                    if (complaintList[i].complaint_priority == 2) {
                        complaintSortedPriorityList.add(complaintList[i])
                    }
                }else{
                    complaintSortedPriorityList.add(complaintList[i])
                }
            }
        }
        if (!checkBoxUrgent.isChecked) {
            for (i in complaintList.indices) {
                if (checkBoxNeutral.isChecked && checkBoxImportant.isChecked) {
                    if (complaintList[i].complaint_priority == 0 || complaintList[i].complaint_priority == 1) {
                        complaintSortedPriorityList.add(complaintList[i])
                    }
                } else if (checkBoxNeutral.isChecked) {
                    if (complaintList[i].complaint_priority == 0) {
                        complaintSortedPriorityList.add(complaintList[i])
                    }
                } else if (checkBoxImportant.isChecked) {
                    if (complaintList[i].complaint_priority == 1) {
                        complaintSortedPriorityList.add(complaintList[i])
                    }
                }else{
                    complaintSortedPriorityList.add(complaintList[i])
                }
            }
        }
        if (checkBoxKampar.isChecked) {
            for (zipCode in zipCodeKampar) {
                if (complaintSortedPriorityList.size > 0) {
                    for (i in complaintSortedPriorityList.indices) {
                        if (complaintSortedPriorityList[i].complaint_postal_code == zipCode) {
                            complaintSortedPostalCodeList.add(
                                complaintSortedPriorityList[i]
                            )
                        }
                    }
                }
//                else {
//                    for (i in complaintList.indices) {
//                        if (complaintList[i].complaint_postal_code == zipCode) {
//                            complaintSortedPostalCodeList.add(complaintList[i])
//                        }
//                    }
//                }
            }
        }
        else {
            if (complaintSortedPriorityList.size > 0) {
                for (i in complaintSortedPriorityList.indices) {
                    complaintSortedPostalCodeList.add(
                        complaintSortedPriorityList[i]
                    )
                }
            }
//            else {
//                for (i in complaintList.indices) {
//                    complaintSortedPostalCodeList.add(complaintList[i])
//                }
//            }
        }
        val isCheckedCategory = mutableListOf<CheckBox>()
        isCheckedCategory.clear()
        for (category in checkBoxCategoryList) {
            if (category.isChecked) {
                isCheckedCategory.add(category)
            }
        }
        if (isCheckedCategory.size > 0){
            for (category in isCheckedCategory){
                if (complaintSortedPostalCodeList.size > 0) {
                    for (i in complaintSortedPostalCodeList.indices) {
                        if (complaintSortedPostalCodeList[i].complaint_category == category.text.toString()) {
                            complaintSortedCategoryList.add(
                                complaintSortedPostalCodeList[i]
                            )
                        }
                    }
                }
                else {
                    if (complaintSortedPriorityList.size > 0) {
                        for (i in complaintSortedPriorityList.indices) {
                            if (complaintSortedPriorityList[i].complaint_category == category.text.toString()) {
                                complaintSortedCategoryList.add(
                                    complaintSortedPriorityList[i]
                                )
                            }
                        }
                    }
//                    else {
//                        for (i in complaintList.indices) {
//                            if (complaintList[i].complaint_category == category.text.toString()) {
//                                complaintSortedCategoryList.add(complaintList[i])
//                            }
//                        }
//                    }
                }
            }
        }else{
            if (complaintSortedPostalCodeList.size > 0){
                for (i in complaintSortedPostalCodeList.indices){
                    complaintSortedCategoryList.add(complaintSortedPostalCodeList[i])
                }
            }
//            else{
//                if (complaintSortedPriorityList.size > 0){
//                    for (i in complaintSortedPriorityList.indices){
//                        complaintSortedCategoryList.add(complaintSortedPriorityList[i])
//                    }
//                }
////                else{
////                    for (i in complaintList.indices){
////                        complaintSortedCategoryList.add(complaintList[i])
////                    }
////                }
//            }
        }

        val complaintFinalList = HashSet<Complaint>(complaintSortedCategoryList)
        complaintSortedCategoryList.clear()
        for (i in complaintFinalList) {
            complaintSortedCategoryList.add(i)
        }
    }

    private fun loadCategoryCheckBox(dialog: Dialog, rootContainer: LinearLayout?) {
        reference = database.getReference("Category")
        reference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var i = 0
                checkBoxCategoryList.clear()
                if (snapshot.exists()) {
                    for (data in snapshot.children) {
                        val checkBoxCategory = CheckBox(dialog.context).apply {
                            text = data.child("category_name").value.toString()
                            id = i++
                            layoutParams = LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            )
                            setPadding(0, 40, 0, 40)
                        }
                        checkBoxCategoryList.add(checkBoxCategory)
                        rootContainer!!.addView(checkBoxCategory)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

    private fun loadDialogSortBy(dialog: Dialog) {
        val buttonApply = dialog.findViewById<Button>(R.id.button_apply)
        val buttonCancel = dialog.findViewById<Button>(R.id.button_cancel)
        val radioGroup = dialog.findViewById<RadioGroup>(R.id.radioGroup)
        val radioGroup2 = dialog.findViewById<RadioGroup>(R.id.radioGroup2)

        if (radioGroup.checkedRadioButtonId != -1){
            buttonApply.setOnClickListener {
                val selectedRadio = radioGroup.checkedRadioButtonId
                val selectedRadio2 = radioGroup2.checkedRadioButtonId
                val radioButton = dialog.findViewById<RadioButton>(selectedRadio)
                val radioButton2 = dialog.findViewById<RadioButton>(selectedRadio2)
                val sortBy = radioButton.text.toString()
                val ordering = radioButton2.text.toString()

                applySortBy(sortBy, ordering)
                dialog.dismiss()
            }
        }
        buttonCancel.setOnClickListener {
            dialog.dismiss()
        }
    }

    private fun applySortBy(sortBy: String, ordering: String) {
        val uid = Constants.userUID
        reference = database.getReference("Complaint")
        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                complaintRegisteredList.clear()
                complaintSolvedList.clear()
                complaintRemovedList.clear()
                complaintPendingList.clear()

                if (snapshot.exists()) {
                    for (i in snapshot.children){
                        for (data in i.children) {
                            if (complaintStatus == "Registered") {
                                if (data.child("complaint_status").value.toString() == "Pending" && data.child("complaint_post_by").value.toString() == uid) {
                                    val complaint = data.getValue(Complaint::class.java)
                                    complaintRegisteredList.add(complaint!!)
                                }
                            } else if (complaintStatus == "Solved") {
                                if (data.child("complaint_status").value.toString() == "Done" && data.child("complaint_post_by").value.toString() == uid) {
                                    val complaint = data.getValue(Complaint::class.java)
                                    complaintSolvedList.add(complaint!!)
                                }
                            } else if (complaintStatus == "Removed") {
                                if (data.child("complaint_status").value.toString() == "Removed" && data.child("complaint_post_by").value.toString() == uid) {
                                    val complaint = data.getValue(Complaint::class.java)
                                    complaintRemovedList.add(complaint!!)
                                }
                            } else if (complaintStatus == "Pending") {
                                if (data.child("complaint_status").value.toString() == "Pending" && data.child("complaint_managed_by").value.toString() == uid) {
                                    val complaint = data.getValue(Complaint::class.java)
                                    complaintPendingList.add(complaint!!)
                                }
                            }
                        }
                    }
                    try {
                        // Complaint Registered
                        if (complaintRegisteredList.size > 0 || complaintSolvedList.size > 0 || complaintRemovedList.size > 0 || complaintPendingList.size > 0) {
                            progressBar.visibility = View.GONE
                            if (complaintRegisteredList.size > 0 && complaintStatus == "Registered") {
                                supportActionBar?.title =
                                    "Complaints Registered (${complaintRegisteredList.size})"
                                recyclerView.visibility = View.VISIBLE
                                linearLayoutManager = LinearLayoutManager(this@ComplaintListActivity)
                                recyclerView.layoutManager = linearLayoutManager
                                if (ordering == "Ascending") {
                                    if (sortBy == "Priority") complaintRegisteredList.sortBy { it.complaint_priority }
                                    if (sortBy == "Category") complaintRegisteredList.sortBy { it.complaint_category }
                                    if (sortBy == "Post Date Time") complaintRegisteredList.sortBy { it.complaint_date_time }
                                } else {
                                    if (sortBy == "Priority") complaintRegisteredList.sortByDescending { it.complaint_priority }
                                    if (sortBy == "Category") complaintRegisteredList.sortByDescending { it.complaint_category }
                                    if (sortBy == "Post Date Time") complaintRegisteredList.sortByDescending { it.complaint_date_time }
                                }
                                complaintRegisteredAdapter =
                                    ComplaintRegisteredListRecyclerAdapter(complaintRegisteredList)
                                complaintRegisteredAdapter.notifyDataSetChanged()
                                recyclerView.adapter = complaintRegisteredAdapter
                            }
                            if (complaintSolvedList.size > 0 && complaintStatus == "Solved") {
                                supportActionBar?.title =
                                    "Complaints Solved (${complaintSolvedList.size})"
                                recyclerView.visibility = View.VISIBLE
                                linearLayoutManager = LinearLayoutManager(this@ComplaintListActivity)
                                recyclerView.layoutManager = linearLayoutManager
                                if (ordering == "Ascending") {
                                    if (sortBy == "Priority") complaintSolvedList.sortBy { it.complaint_priority }
                                    if (sortBy == "Category") complaintSolvedList.sortBy { it.complaint_category }
                                    if (sortBy == "Post Date Time") complaintSolvedList.sortBy { it.complaint_date_time }
                                } else {
                                    if (sortBy == "Priority") complaintSolvedList.sortByDescending { it.complaint_priority }
                                    if (sortBy == "Category") complaintSolvedList.sortByDescending { it.complaint_category }
                                    if (sortBy == "Post Date Time") complaintSolvedList.sortByDescending { it.complaint_date_time }
                                }
                                complaintSolvedAdapter =
                                    ComplaintSolvedListRecyclerAdapter(complaintSolvedList)
                                complaintSolvedAdapter.notifyDataSetChanged()
                                recyclerView.adapter = complaintSolvedAdapter
                            }
                            if (complaintRemovedList.size > 0 && complaintStatus == "Removed") {
                                supportActionBar?.title =
                                    "Complaints Removed (${complaintRemovedList.size})"
                                recyclerView.visibility = View.VISIBLE
                                linearLayoutManager = LinearLayoutManager(this@ComplaintListActivity)
                                recyclerView.layoutManager = linearLayoutManager
                                if (ordering == "Ascending") {
                                    if (sortBy == "Priority") complaintRemovedList.sortBy { it.complaint_priority }
                                    if (sortBy == "Category") complaintRemovedList.sortBy { it.complaint_category }
                                    if (sortBy == "Post Date Time") complaintRemovedList.sortBy { it.complaint_date_time }
                                } else {
                                    if (sortBy == "Priority") complaintRemovedList.sortByDescending { it.complaint_priority }
                                    if (sortBy == "Category") complaintRemovedList.sortByDescending { it.complaint_category }
                                    if (sortBy == "Post Date Time") complaintRemovedList.sortByDescending { it.complaint_date_time }
                                }
                                complaintRemovedAdapter =
                                    ComplaintRemovedListRecyclerAdapter(complaintRemovedList)
                                complaintRemovedAdapter.notifyDataSetChanged()
                                recyclerView.adapter = complaintRemovedAdapter
                            }
                            if (complaintPendingList.size > 0 && complaintStatus == "Pending") {
                                supportActionBar?.title =
                                    "Complaints Handled (${complaintPendingList.size})"
                                recyclerView.visibility = View.VISIBLE
                                linearLayoutManager = LinearLayoutManager(this@ComplaintListActivity)
                                recyclerView.layoutManager = linearLayoutManager
                                if (ordering == "Ascending") {
                                    if (sortBy == "Priority") complaintPendingList.sortBy { it.complaint_priority }
                                    if (sortBy == "Category") complaintPendingList.sortBy { it.complaint_category }
                                    if (sortBy == "Post Date Time") complaintPendingList.sortBy { it.complaint_date_time }
                                } else {
                                    if (sortBy == "Priority") complaintPendingList.sortByDescending { it.complaint_priority }
                                    if (sortBy == "Category") complaintPendingList.sortByDescending { it.complaint_category }
                                    if (sortBy == "Post Date Time") complaintPendingList.sortByDescending { it.complaint_date_time }
                                }
                                complaintPendingAdapter =
                                    ComplaintRegisteredListRecyclerAdapter(complaintPendingList)
                                complaintPendingAdapter.notifyDataSetChanged()
                                recyclerView.adapter = complaintPendingAdapter
                            }
                            Toast.makeText(this@ComplaintListActivity, "All changes applied.", Toast.LENGTH_SHORT).show()
                        }else{
                            progressBar.visibility = View.GONE
                            textNoComplaint.visibility = View.VISIBLE
                        }

                    } catch (e: IllegalStateException) {
                        Log.e("Nono", e.message.toString())
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

    fun RecyclerView.smoothSnapToPosition(
        position: Int,
        snapMode: Int = LinearSmoothScroller.SNAP_TO_START
    ) {
        val smoothScroller = object : LinearSmoothScroller(this.context) {
            override fun getVerticalSnapPreference(): Int = snapMode
            override fun getHorizontalSnapPreference(): Int = snapMode
        }
        smoothScroller.targetPosition = position
        layoutManager?.startSmoothScroll(smoothScroller)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun loadComplaint() {
        val uid = Constants.userUID
        reference = database.getReference("Complaint")
        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                complaintRegisteredList.clear()
                complaintSolvedList.clear()
                complaintRemovedList.clear()
                complaintPendingList.clear()

                if (snapshot.exists()) {
                    for (i in snapshot.children){
                        for (data in i.children) {
                            if (complaintStatus == "Registered") {
                                if (data.child("complaint_status").value.toString() == "Pending" && data.child("complaint_post_by").value.toString() == uid) {
                                    val complaint = data.getValue(Complaint::class.java)
                                    complaintRegisteredList.add(complaint!!)
                                }
                            } else if (complaintStatus == "Solved") {
                                if (Constants.userRole == "Resident"){
                                    if (data.child("complaint_status").value.toString() == "Done" && data.child("complaint_post_by").value.toString() == uid) {
                                        val complaint = data.getValue(Complaint::class.java)
                                        complaintSolvedList.add(complaint!!)
                                    }
                                }else{
                                    if (data.child("complaint_status").value.toString() == "Done" && data.child("complaint_managed_by").value.toString() == uid) {
                                        val complaint = data.getValue(Complaint::class.java)
                                        complaintSolvedList.add(complaint!!)
                                    }
                                }

                            } else if (complaintStatus == "Removed") {
                                if (data.child("complaint_status").value.toString() == "Removed" && data.child("complaint_post_by").value.toString() == uid) {
                                    val complaint = data.getValue(Complaint::class.java)
                                    complaintRemovedList.add(complaint!!)
                                }
                            } else if (complaintStatus == "Pending") {
                                if (data.child("complaint_status").value.toString() == "Pending" && data.child("complaint_managed_by").value.toString() == uid) {
                                    val complaint = data.getValue(Complaint::class.java)
                                    complaintPendingList.add(complaint!!)
                                }
                            }
                        }
                    }
                    try {
                        // Complaint Registered
                        if (complaintRegisteredList.size > 0 || complaintSolvedList.size > 0 || complaintRemovedList.size > 0 || complaintPendingList.size > 0) {
                            progressBar.visibility = View.GONE
                            if (complaintRegisteredList.size > 0 && complaintStatus == "Registered") {
                                supportActionBar?.title =
                                    "Complaints Registered (${complaintRegisteredList.size})"
                                recyclerView.visibility = View.VISIBLE
                                linearLayoutManager = LinearLayoutManager(this@ComplaintListActivity)
                                recyclerView.layoutManager = linearLayoutManager
                                complaintRegisteredList.sortByDescending { it.complaint_date_time }
                                complaintRegisteredAdapter =
                                    ComplaintRegisteredListRecyclerAdapter(complaintRegisteredList)
                                complaintRegisteredAdapter.notifyDataSetChanged()
                                recyclerView.adapter = complaintRegisteredAdapter
                            }
                            if (complaintSolvedList.size > 0 && complaintStatus == "Solved") {
                                supportActionBar?.title =
                                    "Complaints Solved (${complaintSolvedList.size})"
                                recyclerView.visibility = View.VISIBLE
                                linearLayoutManager = LinearLayoutManager(this@ComplaintListActivity)
                                recyclerView.layoutManager = linearLayoutManager
                                complaintSolvedList.sortByDescending { it.complaint_solved_date }
                                complaintSolvedAdapter =
                                    ComplaintSolvedListRecyclerAdapter(complaintSolvedList)
                                complaintSolvedAdapter.notifyDataSetChanged()
                                recyclerView.adapter = complaintSolvedAdapter
                            }
                            if (complaintRemovedList.size > 0 && complaintStatus == "Removed") {
                                supportActionBar?.title =
                                    "Complaints Removed (${complaintRemovedList.size})"
                                recyclerView.visibility = View.VISIBLE
                                linearLayoutManager = LinearLayoutManager(this@ComplaintListActivity)
                                recyclerView.layoutManager = linearLayoutManager
                                complaintRemovedList.sortByDescending { it.complaint_date_time }
                                complaintRemovedAdapter =
                                    ComplaintRemovedListRecyclerAdapter(complaintRemovedList)
                                complaintRemovedAdapter.notifyDataSetChanged()
                                recyclerView.adapter = complaintRemovedAdapter
                            }
                            if (complaintPendingList.size > 0 && complaintStatus == "Pending") {
                                supportActionBar?.title =
                                    "Complaints Handled (${complaintPendingList.size})"
                                recyclerView.visibility = View.VISIBLE
                                linearLayoutManager = LinearLayoutManager(this@ComplaintListActivity)
                                recyclerView.layoutManager = linearLayoutManager
                                complaintPendingList.sortByDescending { it.complaint_date_time }
                                complaintPendingAdapter =
                                    ComplaintRegisteredListRecyclerAdapter(complaintPendingList)
                                complaintPendingAdapter.notifyDataSetChanged()
                                recyclerView.adapter = complaintPendingAdapter
                            }
                        }else{
                            progressBar.visibility = View.GONE
                            textNoComplaint.visibility = View.VISIBLE
                        }

                    } catch (e: IllegalStateException) {
                        Log.e("Nono", e.message.toString())
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }
}