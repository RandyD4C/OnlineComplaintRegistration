package com.cyx.onlinecomplaintregistration.activities

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.ProgressBar
import com.cyx.onlinecomplaintregistration.R
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.isVisible
import com.cyx.onlinecomplaintregistration.classes.Constants
import com.cyx.onlinecomplaintregistration.classes.MyFirebaseMessagingService
import com.cyx.onlinecomplaintregistration.management.activities.ManagementMainActivity
import com.cyx.onlinecomplaintregistration.resident.activities.ResidentMainActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.android.synthetic.main.fragment_notification.*
import java.util.*

const val TOPIC = "/topics/myComplaint"

class LandingPageActivity : AppCompatActivity() {

    private lateinit var database: FirebaseDatabase
    private lateinit var reference: DatabaseReference
    private lateinit var progressBar: ProgressBar

    companion object {
        private const val LOCATION_REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_landing_page)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        progressBar = findViewById(R.id.progressBar)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        if (!isOnline(this)) {
            val builder = AlertDialog.Builder(this)
            builder.setMessage("No Internet Connection. Unable to start the application.")
            builder.setTitle("Network Offline")
            builder.setCancelable(true)
            builder.setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                finishAffinity()
            }
            val alert = builder.create()
            alert.show()
        } else {
            database = Constants.database
            verifyUser()
        }
    }

    fun isOnline(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager?.let { it ->
            val capabilities =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    it.getNetworkCapabilities(it.activeNetwork)
                } else {
                    return true
                }
            capabilities?.let {
                if (it.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_CELLULAR")
                    return true
                } else if (it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_WIFI")
                    return true
                } else if (it.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_ETHERNET")
                    return true
                }
            }
        }
        return false
    }

    private fun verifyUser() {
        progressBar.isVisible = true
        val uid = FirebaseAuth.getInstance().uid

        if (uid == null) {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)

        } else {
            reference = database.getReference("User/$uid")
            reference.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        if (snapshot.child("user_status").value.toString() != "Active") {
                            val intent = Intent(this@LandingPageActivity, LoginActivity::class.java)
                            intent.flags =
                                Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(intent)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }

            })

            reference = database.getReference("User")
            reference.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (i in snapshot.children) {
                            if (i.child("user_uid").value.toString() == uid) {
                                if (i.child("user_status").value.toString() == "Active") {
                                    setUserToken(uid)
                                    saveUserPreference(
                                        i.child("user_uid").value.toString(),
                                        i.child("user_name").value.toString(),
                                        i.child("user_role").value.toString()
                                    )
                                    if (i.child("user_role").value.toString() == "Resident") {
                                        val intent = Intent(
                                            this@LandingPageActivity,
                                            ResidentMainActivity::class.java
                                        )
                                        progressBar.isVisible = false
                                        intent.flags =
                                            Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        startActivity(intent)
                                    } else if (i.child("user_role").value.toString() == "Management") {
                                        val intent = Intent(
                                            this@LandingPageActivity,
                                            ManagementMainActivity::class.java
                                        )
                                        progressBar.isVisible = false
                                        intent.flags =
                                            Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        startActivity(intent)
                                    } else {
                                        val intent = Intent(
                                            this@LandingPageActivity,
                                            ManagementMainActivity::class.java
                                        )
                                        progressBar.isVisible = false
                                        intent.flags =
                                            Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        startActivity(intent)
                                    }
                                } else {
                                    val intent =
                                        Intent(this@LandingPageActivity, LoginActivity::class.java)
                                    intent.flags =
                                        Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    startActivity(intent)
                                }
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("cancel", error.toString())
                }
            })

        }
    }

    private fun saveUserPreference(userUID: String, userName: String, userRole: String) {
        Constants.sharedPref = getSharedPreferences("sharedPref", Context.MODE_PRIVATE)
        Constants.userUID = userUID
        Constants.userName = userName
        Constants.userRole = userRole
    }

    private fun setUserToken(uid: String) {
        MyFirebaseMessagingService.sharedPref =
            getSharedPreferences("sharedPref", Context.MODE_PRIVATE)
        FirebaseMessaging.getInstance().token.addOnSuccessListener {
            MyFirebaseMessagingService.token = it
            val updateToken = mapOf(
                "user_token" to it
            )
            reference = database.getReference("User/$uid")
            reference.updateChildren(updateToken)
        }
        FirebaseMessaging.getInstance().subscribeToTopic(TOPIC)
    }


}