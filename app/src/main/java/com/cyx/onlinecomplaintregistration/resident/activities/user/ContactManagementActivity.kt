package com.cyx.onlinecomplaintregistration.resident.activities.user

import android.content.Context
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cyx.onlinecomplaintregistration.R
import com.cyx.onlinecomplaintregistration.classes.Constants
import com.cyx.onlinecomplaintregistration.classes.User
import com.cyx.onlinecomplaintregistration.resident.adapters.ContactManagementRecyclerAdapter
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.fragment_all_complaint_resident.*
import net.cachapa.expandablelayout.ExpandableLayout

class ContactManagementActivity : AppCompatActivity() {

    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var database: FirebaseDatabase
    private lateinit var reference: DatabaseReference
    private lateinit var userAdapter: ContactManagementRecyclerAdapter
    private lateinit var recyclerView: RecyclerView
    private var userList = mutableListOf<User>()
    private lateinit var progressBar: ProgressBar
    private lateinit var textNoManagement: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact_management)
        this.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Contact Management"

        Constants.sharedPref =
            getSharedPreferences("sharedPref", Context.MODE_PRIVATE)
        database = Constants.database
        swipeToRefresh.setColorSchemeColors(Color.BLUE)
        recyclerView = findViewById(R.id.rv_recyclerView)
        progressBar = findViewById(R.id.progress_bar)
        textNoManagement = findViewById(R.id.text_no_user)

        loadManagement()
        refreshApp()

    }

    private fun loadManagement() {
        reference = database.getReference("User")
        reference.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                userList.clear()
                if (snapshot.exists()){
                    for (data in snapshot.children){
//                        Change this to Management
                        if (data.child("user_role").value.toString() == "Management"){
                            userList.add(data.getValue(User::class.java)!!)
                        }
                    }
                }else{
                    progressBar.visibility = View.GONE
                    textNoManagement.visibility = View.VISIBLE
                }

                if (userList.size > 0){
                    try {
                        progressBar.visibility = View.GONE
                        linearLayoutManager = LinearLayoutManager(this@ContactManagementActivity)
                        recyclerView.layoutManager = linearLayoutManager
                        userAdapter = ContactManagementRecyclerAdapter(userList)
                        userAdapter.notifyDataSetChanged()
                        recyclerView.adapter = userAdapter
                    }catch (e: IllegalStateException){
                        Log.e("Error", e.message.toString())
                    }
                }else{
                    progressBar.visibility = View.GONE
                    textNoManagement.visibility = View.VISIBLE
                }
                swipeToRefresh.isRefreshing = false
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

    private fun refreshApp() {
        swipeToRefresh.setOnRefreshListener {
            recyclerView.removeAllViewsInLayout()
            loadManagement()
        }
    }

}