package com.cyx.onlinecomplaintregistration.activities

import android.app.ProgressDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import com.cyx.onlinecomplaintregistration.R
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var email: EditText
    private lateinit var buttonResetPassword: Button
    private lateinit var buttonBack: Button
    private lateinit var progressBar: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN)

        email = findViewById(R.id.edit_text_email)
        buttonResetPassword = findViewById(R.id.button_reset_password)
        buttonBack = findViewById(R.id.button_back)

        buttonResetPassword.setOnClickListener {
            passwordRecovery()
        }
        buttonBack.setOnClickListener {
            finish()
        }
    }

    private fun passwordRecovery() {
        progressBar = ProgressDialog(this)
        progressBar.setMessage("Verifying...")
        progressBar.setCancelable(false)
        progressBar.show()

        val auth = FirebaseAuth.getInstance()
        if (email.text.toString().trim().isNullOrEmpty()){
            email.error = "Please enter your email before proceed!"
            email.requestFocus()
            if(progressBar.isShowing) progressBar.dismiss()
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.text.toString()).matches()) {
            email.error = "Please enter valid email address"
            email.requestFocus()
            if(progressBar.isShowing) progressBar.dismiss()
            return
        }

        auth.sendPasswordResetEmail(email.text.toString()).addOnCompleteListener {
            if(it.isSuccessful){
                if(progressBar.isShowing) progressBar.dismiss()
                val builder = AlertDialog.Builder(this)
                builder.setMessage("An recovery link has sent to your email. Please check it out.")
                builder.setTitle("Password Recovery")
                builder.setCancelable(false)
                builder.setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                    finish()
                }
                val alert = builder.create()
                alert.show()
            }else{
                if(progressBar.isShowing) progressBar.dismiss()
                email.error = "Invalid Email!"
                email.requestFocus()
            }
        }
    }
}