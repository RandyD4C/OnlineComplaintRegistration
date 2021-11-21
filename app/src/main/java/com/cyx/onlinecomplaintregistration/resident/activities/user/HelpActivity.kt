package com.cyx.onlinecomplaintregistration.resident.activities.user

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cyx.onlinecomplaintregistration.R
import com.cyx.onlinecomplaintregistration.classes.Constants
import com.cyx.onlinecomplaintregistration.classes.FAQ
import com.cyx.onlinecomplaintregistration.resident.adapters.ComplaintRecyclerAdapter
import com.cyx.onlinecomplaintregistration.resident.adapters.FAQRecyclerAdapter
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.*

class HelpActivity : AppCompatActivity() {

    private lateinit var database: FirebaseDatabase
    private lateinit var reference: DatabaseReference
    private lateinit var recyclerView: RecyclerView
    private var faqList = mutableListOf<FAQ>()
    private lateinit var faqAdapter: FAQRecyclerAdapter
    private lateinit var linearLayoutManager: LinearLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)
        this.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        this.supportActionBar?.title = "Help"

        database = Constants.database
        recyclerView = findViewById(R.id.rv_recyclerView)

        loadRecyclerView()
    }

    private fun loadRecyclerView() {
        reference = database.getReference("FAQ")
        reference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                faqList.clear()
                if (snapshot.exists()) {
                    for (data in snapshot.children){
                        data.getValue(FAQ::class.java)?.let { faqList.add(it) }
                    }
                }

                if (faqList.size > 0) {
                    linearLayoutManager = LinearLayoutManager(this@HelpActivity)
                    recyclerView.layoutManager = linearLayoutManager
                    faqAdapter = FAQRecyclerAdapter(faqList)
                    faqAdapter.notifyDataSetChanged()
                    recyclerView.adapter = faqAdapter
                } else {
                    Toast.makeText(this@HelpActivity, "No FAQ found...", Toast.LENGTH_LONG).show()
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
}