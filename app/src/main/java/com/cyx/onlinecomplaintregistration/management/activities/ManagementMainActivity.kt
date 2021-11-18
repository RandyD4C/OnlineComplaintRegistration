package com.cyx.onlinecomplaintregistration.management.activities

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Window
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.cyx.onlinecomplaintregistration.R
import com.cyx.onlinecomplaintregistration.classes.Constants
import com.cyx.onlinecomplaintregistration.management.fragments.ManagementHomeFragment
import com.cyx.onlinecomplaintregistration.resident.fragments.NotificationFragment
import com.cyx.onlinecomplaintregistration.resident.fragments.MapsFragment
import com.cyx.onlinecomplaintregistration.resident.fragments.UserFragment
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase


class ManagementMainActivity : AppCompatActivity() {
    companion object {
        private const val LOCATION_REQUEST_CODE = 1
    }
    private lateinit var database: FirebaseDatabase
    private lateinit var reference: DatabaseReference
    private var isRegistered = false
    private var isSavedDepartment = false
    private var departmentList = mutableListOf<String>()
    private lateinit var badge: BadgeDrawable
    private var notificationCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_management)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        checkLocationPermission()
        Constants.sharedPref =
            getSharedPreferences("sharedPref", Context.MODE_PRIVATE)
        database = Constants.database
        checkRegistrationStatus()

        val notificationFragment = NotificationFragment()
        val homeFragment = ManagementHomeFragment()
        val mapsFragment = MapsFragment()
        val userProfileFragment = UserFragment()
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        makeCurrentFragment(homeFragment)
        badge = bottomNavigation.getOrCreateBadge(R.id.ic_notification)
        loadBadge()
        bottomNavigation.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.ic_notification -> {
                    makeCurrentFragment(notificationFragment)
                }
                R.id.ic_home -> {
                    makeCurrentFragment(homeFragment)
                }
                R.id.ic_maps -> {
                    makeCurrentFragment(mapsFragment)
                }
                R.id.ic_user -> {
                    makeCurrentFragment(userProfileFragment)
                }
            }
            true
        }
    }

    private fun loadBadge() {
        val uid = Constants.userUID
        reference = database.getReference("Notification/$uid")
        reference.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                notificationCount = 0
                if (snapshot.exists()){
                    for (data in snapshot.children){
                        if (data.child("notification_status").value.toString() == "Unread"){
                            notificationCount++
                        }
                    }
                }
                if (notificationCount > 0){
                    badge.isVisible = true
                    badge.number = notificationCount
                    badge.backgroundColor = Color.parseColor("#F44336")
                }else{
                    badge.isVisible = false
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

    private fun checkRegistrationStatus() {
        val uid = Constants.userUID
        reference = database.getReference("RegisterManagement/$uid")
        reference.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()){
                    if (snapshot.child("register_status").value.toString() == "Accepted"){
                        isRegistered = true
                        Dialog(this@ManagementMainActivity).apply {
                            requestWindowFeature(Window.FEATURE_NO_TITLE)
                            setCancelable(false)
                            setContentView(R.layout.layout_registeration_accepted)
                            val layoutParams = WindowManager.LayoutParams()
                            layoutParams.copyFrom(window?.attributes)
                            layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT
                            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
                            window?.attributes = layoutParams
                            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                            window?.setWindowAnimations(R.style.DialogAnimation)
                            loadDepartment(this)
                            val buttonOK: Button = findViewById(R.id.button_ok)
                            buttonOK.setOnClickListener {
                                saveDepartmentToFirebase(this)
                            }
                            show()
                        }
                    }
                }

            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

    private fun saveDepartmentToFirebase(dialog: Dialog) {
        val uid = Constants.userUID ?:""
        val spinner: Spinner = dialog.findViewById(R.id.spinner_department)
        val updateDepartment = mapOf<String, String>(
            "user_department" to spinner.selectedItem.toString()
        )
        reference = database.getReference("User/$uid")
        reference.updateChildren(updateDepartment).addOnCompleteListener {
            if (it.isSuccessful){
                isSavedDepartment = true
                if (isRegistered && isSavedDepartment) removeRegistrationStatus(uid)
                dialog.dismiss()
            }else{
                dialog.dismiss()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    Snackbar.make(requireViewById(R.id.management_main_layout),"Error occurred.", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadDepartment(dialog: Dialog) {
        reference = database.getReference("Department")
        reference.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                departmentList.clear()
                if (snapshot.exists()){
                    for (data in snapshot.children){
                        if (data.child("department_status").value.toString() == "Available"){
                            departmentList.add(data.child("department_name").value.toString())
                        }
                    }
                }
                if (departmentList.size > 0){
                    val adapter = ArrayAdapter(
                        this@ManagementMainActivity,
                        android.R.layout.simple_list_item_1, departmentList
                    )
                    adapter.setDropDownViewResource(android.R.layout.simple_list_item_1)
                    val spinnerDepartment: Spinner = dialog.findViewById(R.id.spinner_department)
                    spinnerDepartment.adapter = adapter
                }

            }
            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
    }

    private fun removeRegistrationStatus(uid: String) {
        reference = database.getReference("RegisterManagement/$uid")
        reference.removeValue()
    }

    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_REQUEST_CODE
            )
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_REQUEST_CODE
            )
            return
        }
    }


    private fun makeCurrentFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fl_wrapper, fragment).commit()
    }

    private var doubleBackToExitPressedOnce = false
    override fun onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            finishAffinity()
            super.onBackPressed()
            return
        }

        this.doubleBackToExitPressedOnce = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Snackbar.make(requireViewById(R.id.management_main_layout),"Click BACK again to exit", Snackbar.LENGTH_SHORT).show()
        }

        Handler(Looper.getMainLooper()).postDelayed({
            doubleBackToExitPressedOnce = false
        }, 2000)
    }
}