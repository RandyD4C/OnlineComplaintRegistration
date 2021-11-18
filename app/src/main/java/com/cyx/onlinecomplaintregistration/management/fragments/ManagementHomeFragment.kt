package com.cyx.onlinecomplaintregistration.management.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager.widget.ViewPager
import com.cyx.onlinecomplaintregistration.R
import com.cyx.onlinecomplaintregistration.classes.Complaint
import com.cyx.onlinecomplaintregistration.resident.adapters.ComplaintRecyclerAdapter
import com.cyx.onlinecomplaintregistration.resident.adapters.ViewPagerAdapter
import com.cyx.onlinecomplaintregistration.resident.fragments.AllComplaintResidentFragment
import com.google.android.material.tabs.TabLayout
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class ManagementHomeFragment : Fragment() {

    private lateinit var progressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_management_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        progressBar = view.findViewById(R.id.progress_bar)
        setUpTabs()
    }

    private fun setUpTabs() {
        try {
            val adapter = ViewPagerAdapter(this.childFragmentManager)
            adapter.addFragment(MyWorkshopFragment(), "My Workshop")
            adapter.addFragment(AllComplaintResidentFragment(), "All Complaints")
            val viewPager = requireView().findViewById<ViewPager>(R.id.view_pager)
            viewPager.adapter = adapter
            val tabs = requireView().findViewById<TabLayout>(R.id.tabs)
            tabs.setupWithViewPager(viewPager)
            progressBar.visibility = View.GONE
        }catch (e: IllegalStateException){
            Log.d("Hoho", e.message.toString())
        }
    }
}