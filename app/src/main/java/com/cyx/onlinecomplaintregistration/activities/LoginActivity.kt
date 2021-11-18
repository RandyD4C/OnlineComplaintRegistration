package com.cyx.onlinecomplaintregistration.activities

import android.app.Dialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.cyx.onlinecomplaintregistration.R
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.cyx.onlinecomplaintregistration.classes.Constants
import com.cyx.onlinecomplaintregistration.classes.MyFirebaseMessagingService
import com.cyx.onlinecomplaintregistration.classes.User
import com.cyx.onlinecomplaintregistration.management.activities.ManagementMainActivity
import com.cyx.onlinecomplaintregistration.resident.activities.ResidentMainActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging


class LoginActivity : AppCompatActivity() {

    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var buttonLogin: Button
    private lateinit var buttonSignUp: Button
    private lateinit var buttonForgotPassword: Button
    private lateinit var database: FirebaseDatabase
    private lateinit var reference: DatabaseReference
    private lateinit var progressBar: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        database = Constants.database
        email = findViewById(R.id.edit_text_email)
        password = findViewById(R.id.edit_text_password)
        buttonLogin = findViewById(R.id.button_login)
        buttonSignUp = findViewById(R.id.button_sign_up)
        buttonForgotPassword = findViewById(R.id.button_forgot_password)

        buttonLogin.setOnClickListener {
            if (email.text.toString().trim().isNullOrEmpty()) {
                email.error = "Email cannot be blank"
                email.requestFocus()
                return@setOnClickListener
            }
            if (password.text.toString().trim().isNullOrEmpty()) {
                password.error = "Password cannot be blank"
                password.requestFocus()
                return@setOnClickListener
            }
            performLogin()
        }

        buttonSignUp.setOnClickListener {
            val intent = Intent(this, RegistrationActivity::class.java)
            startActivity(intent)
        }

        buttonForgotPassword.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }
    }

    private fun performLogin() {

        progressBar = ProgressDialog(this)
        progressBar.setMessage("Logging in...")
        progressBar.setCancelable(false)
        progressBar.show()

        FirebaseAuth.getInstance()
            .signInWithEmailAndPassword(email.text.toString(), password.text.toString())
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    reference = database.getReference("User")
                    reference.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.exists()) {
                                for (i in snapshot.children) {
                                    if (i.child("user_email").value.toString() == email.text.toString()) {
                                        if (i.child("user_status").value.toString() == "Active") {
                                            if (progressBar.isShowing) progressBar.dismiss()
                                            setUserToken(i.child("user_uid").value.toString())
                                            saveUserPreference(
                                                i.child("user_uid").value.toString(),
                                                i.child("user_name").value.toString(),
                                                i.child("user_role").value.toString()
                                            )
                                            if (i.child("user_role").value.toString() == "Resident") {
                                                val intent = Intent(
                                                    this@LoginActivity,
                                                    ResidentMainActivity::class.java
                                                )
                                                intent.flags =
                                                    Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                startActivity(intent)
                                            } else if (i.child("user_role").value.toString() == "Management") {
                                                val intent = Intent(
                                                    this@LoginActivity,
                                                    ManagementMainActivity::class.java
                                                )
                                                startActivity(intent)
                                                finish()
                                            } else {
                                                val intent = Intent(
                                                    this@LoginActivity,
                                                    ManagementMainActivity::class.java
                                                )
                                                startActivity(intent)
                                                finish()
                                            }
                                        } else {
                                            if (progressBar.isShowing) progressBar.dismiss()
                                            Dialog(this@LoginActivity).apply {
                                                requestWindowFeature(Window.FEATURE_NO_TITLE)
                                                setCancelable(true)
                                                setContentView(R.layout.layout_user_status_error)
                                                val layoutParams = WindowManager.LayoutParams()
                                                layoutParams.copyFrom(window?.attributes)
                                                layoutParams.width =
                                                    WindowManager.LayoutParams.WRAP_CONTENT
                                                layoutParams.height =
                                                    WindowManager.LayoutParams.WRAP_CONTENT
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
                                }
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            if (progressBar.isShowing) progressBar.dismiss()
                            Log.e("cancel", error.toString())
                        }

                    })
                }
            }.addOnFailureListener {
                if (progressBar.isShowing) progressBar.dismiss()
                val builder = AlertDialog.Builder(this)
                builder.setMessage("${it.message}")
                builder.setTitle("Error")
                builder.setCancelable(false)
                builder.setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                }
                val alert = builder.create()
                alert.show()
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