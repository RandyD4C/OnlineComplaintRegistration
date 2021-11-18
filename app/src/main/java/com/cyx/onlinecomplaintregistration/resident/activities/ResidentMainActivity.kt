package com.cyx.onlinecomplaintregistration.resident.activities

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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.cyx.onlinecomplaintregistration.R
import com.cyx.onlinecomplaintregistration.classes.Constants
import com.cyx.onlinecomplaintregistration.resident.fragments.NotificationFragment
import com.cyx.onlinecomplaintregistration.resident.fragments.HomeFragment
import com.cyx.onlinecomplaintregistration.resident.fragments.MapsFragment
import com.cyx.onlinecomplaintregistration.resident.fragments.PhotoFragment
import com.cyx.onlinecomplaintregistration.resident.fragments.UserFragment
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase


class ResidentMainActivity : AppCompatActivity() {
    companion object {
        private const val LOCATION_REQUEST_CODE = 1
    }
    private lateinit var database: FirebaseDatabase
    private lateinit var reference: DatabaseReference
    private var isRegistered = false
    private lateinit var badge: BadgeDrawable
    private var notificationCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_resident)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        checkLocationPermission()

        Constants.sharedPref =
            getSharedPreferences("sharedPref", Context.MODE_PRIVATE)

        database = Constants.database
        checkRegistrationStatus()

        val notificationFragment = NotificationFragment()
        val homeFragment = HomeFragment()
        val photoFragment = PhotoFragment()
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
//                R.id.ic_photo -> {
//                    makeCurrentFragment(photoFragment)
//                }
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
        val uid = Constants.userUID?:""
        reference = database.getReference("RegisterManagement/$uid")
        reference.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()){
                    if (snapshot.child("register_status").value.toString() == "Rejected"){
                        isRegistered = true
                        Dialog(this@ResidentMainActivity).apply {
                            requestWindowFeature(Window.FEATURE_NO_TITLE)
                            setCancelable(true)
                            setContentView(R.layout.layout_registeration_rejected)
                            val layoutParams = WindowManager.LayoutParams()
                            layoutParams.copyFrom(window?.attributes)
                            layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT
                            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
                            window?.attributes = layoutParams
                            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                            window?.setWindowAnimations(R.style.DialogAnimation)
                            val buttonOK: Button = findViewById(R.id.button_ok)
                            buttonOK.setOnClickListener {
                                dismiss()
                            }
                            show()
                        }
                    }
                }
                if (isRegistered) removeRegistrationStatus(uid)
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

//    private fun setWindowFlag(bits: Int, on: Boolean) {
//        val win = window
//        val winParams = win.attributes
//        if (on) {
//            winParams.flags = winParams.flags or bits
//        } else {
//            winParams.flags = winParams.flags and bits.inv()
//        }
//        win.attributes = winParams
//    }

//    override fun onSaveInstanceState(outState: Bundle) {
//        super.onSaveInstanceState(outState)
//
//    }
//
//    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
//        super.onRestoreInstanceState(savedInstanceState)
//    }

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
            Snackbar.make(requireViewById(R.id.resident_main_layout),"Click BACK again to exit", Snackbar.LENGTH_SHORT).show()
        }

        Handler(Looper.getMainLooper()).postDelayed(Runnable {
            doubleBackToExitPressedOnce = false
        }, 2000)
    }
}