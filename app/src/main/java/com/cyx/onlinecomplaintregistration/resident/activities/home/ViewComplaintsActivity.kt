package com.cyx.onlinecomplaintregistration.resident.activities.home

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cyx.onlinecomplaintregistration.R
import com.cyx.onlinecomplaintregistration.classes.Complaint
import com.cyx.onlinecomplaintregistration.classes.Constants
import com.cyx.onlinecomplaintregistration.management.adapters.ManagementComplaintPostRecyclerAdapter
import com.cyx.onlinecomplaintregistration.management.adapters.ManagementComplaintRecyclerAdapter
import com.cyx.onlinecomplaintregistration.resident.adapters.ComplaintPostNoButtonRecyclerAdapter
import com.cyx.onlinecomplaintregistration.resident.adapters.ComplaintPostRecyclerAdapter
import com.cyx.onlinecomplaintregistration.resident.adapters.ComplaintRecyclerAdapter
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.fragment_all_complaint_resident.*
import net.cachapa.expandablelayout.ExpandableLayout

class ViewComplaintsActivity : AppCompatActivity() {

    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var database: FirebaseDatabase
    private lateinit var reference: DatabaseReference
    private var complaintList = mutableListOf<Complaint>()
    private lateinit var complaintAdapter: ComplaintRecyclerAdapter
    private lateinit var managementComplaintAdapter: ManagementComplaintRecyclerAdapter
    private lateinit var managementComplaintPostAdapter: ManagementComplaintPostRecyclerAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var textNoComplaint: TextView
    private var userUID = ""
    private var complaintUID = ""
    private var complaintCategory = ""
    private var complaintPriority = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_complaints)
        this.supportActionBar?.setDisplayHomeAsUpEnabled(true)

        recyclerView = findViewById(R.id.rv_recyclerView)
        textNoComplaint = findViewById(R.id.text_no_complaint)
        progressBar = findViewById(R.id.progress_bar)

        Constants.sharedPref =
            getSharedPreferences("sharedPref", Context.MODE_PRIVATE)
        database = Constants.database
        userUID = intent.getStringExtra("user_uid") ?: ""
        complaintUID = intent.getStringExtra("complaint_uid") ?: ""
        complaintCategory = intent.getStringExtra("complaint_category") ?: ""
        complaintPriority = intent.getStringExtra("complaint_priority") ?: ""

        loadRecyclerView()
        refreshApp()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun refreshApp() {
        swipeToRefresh.setOnRefreshListener {
            recyclerView.removeAllViewsInLayout()
            loadRecyclerView()
            swipeToRefresh.isRefreshing = false
        }
    }

    private fun loadRecyclerView() {
        reference = database.getReference("Complaint")
        reference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                complaintList.clear()
                if (snapshot.exists()) {
                    for (data in snapshot.children) {
                        for (i in data.children) {
                            if (i.child("complaint_status").value.toString() == "Pending"){
                                if (userUID != "" && i.child("complaint_post_by").value.toString() == userUID) {
                                    val complaint = i.getValue(Complaint::class.java)
                                    complaintList.add(complaint!!)
                                    supportActionBar?.title = i.child("user_name").value.toString()
                                }
                                if (complaintUID != "" && i.child("complaint_uid").value.toString() == complaintUID){
                                    val complaint = i.getValue(Complaint::class.java)
                                    complaintList.add(complaint!!)
                                    supportActionBar?.title = i.child("user_name").value.toString()
                                }
                                if (complaintCategory != "" && i.child("complaint_category").value.toString() == complaintCategory) {
                                    val complaint = i.getValue(Complaint::class.java)
                                    complaintList.add(complaint!!)
                                    supportActionBar?.title = i.child("complaint_category").value.toString()
                                }
                                if (complaintPriority != "" && i.child("complaint_priority").value.toString().toInt() == complaintPriority.toInt()) {
                                    val complaint = i.getValue(Complaint::class.java)
                                    complaintList.add(complaint!!)
                                    if (i.child("complaint_priority").value.toString().toInt() == 1){
                                        supportActionBar?.title = "Important"
                                    }else{
                                        supportActionBar?.title = "Urgent"
                                    }
                                }
                            }
                        }
                    }
                    try {
                        if (complaintList.size > 0) {
                            progressBar.visibility = View.GONE
                            textNoComplaint.visibility = View.GONE
                            linearLayoutManager = LinearLayoutManager(this@ViewComplaintsActivity)
                            recyclerView.layoutManager = linearLayoutManager
                            complaintList.sortByDescending { it.complaint_date_time }

                            if (Constants.userRole == "Resident"){
                                complaintAdapter = ComplaintRecyclerAdapter(complaintList)
                                complaintAdapter.notifyDataSetChanged()
                                recyclerView.adapter = complaintAdapter
                            }else{
                                if (complaintList[0].complaint_managed_by == "" || complaintCategory != "" || complaintPriority != ""){
                                    managementComplaintAdapter = ManagementComplaintRecyclerAdapter(complaintList)
                                    managementComplaintAdapter.notifyDataSetChanged()
                                    recyclerView.adapter = managementComplaintAdapter
                                }else{
                                    managementComplaintPostAdapter = ManagementComplaintPostRecyclerAdapter(complaintList)
                                    managementComplaintPostAdapter.notifyDataSetChanged()
                                    recyclerView.adapter = managementComplaintPostAdapter
                                }

                            }
                        }else{
                            progressBar.visibility = View.GONE
                            textNoComplaint.visibility = View.VISIBLE
                        }
                    } catch (e: Exception) {
                        Log.d("Error", e.message.toString())
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

}