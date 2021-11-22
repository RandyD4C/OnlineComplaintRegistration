package com.cyx.onlinecomplaintregistration.resident.activities.user

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.*
import com.cyx.onlinecomplaintregistration.R
import com.cyx.onlinecomplaintregistration.activities.LoginActivity
import com.cyx.onlinecomplaintregistration.classes.*
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.layout_change_email.*
import kotlinx.android.synthetic.main.layout_change_email.button_cancel
import kotlinx.android.synthetic.main.layout_change_email.button_update
import kotlinx.android.synthetic.main.layout_change_password.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class UserProfileViewMoreActivity : AppCompatActivity() {

    private lateinit var cardRegisterAsManagement: FrameLayout
    private lateinit var cardDeleteAccount: FrameLayout
    private lateinit var cardChangeEmail: FrameLayout
    private lateinit var cardChangePassword: FrameLayout
    private lateinit var cardDepartment: FrameLayout
    private lateinit var textEmail: TextView
    private lateinit var textDepartment: TextView
    private var departmentList = mutableListOf<String>()
    private var isLegitUser = false
    private var isRegistered = false
    private lateinit var database: FirebaseDatabase
    private lateinit var reference: DatabaseReference
    private var adminUID = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile_view_more)
        this.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "More Options"

        cardRegisterAsManagement = findViewById(R.id.card_register_as_management)
        cardDeleteAccount = findViewById(R.id.card_delete_account)
        cardChangeEmail = findViewById(R.id.card_change_email)
        cardChangePassword = findViewById(R.id.card_change_password)
        cardDepartment = findViewById(R.id.frame_department)
        textEmail = findViewById(R.id.text_email)
        textDepartment = findViewById(R.id.text_department)

        Constants.sharedPref =
            getSharedPreferences("sharedPref", Context.MODE_PRIVATE)

        database = Constants.database
        loadUserData()
        loadIsRegistered()

        cardChangeEmail.setOnClickListener {
            Dialog(this).apply {
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                setCancelable(true)
                setContentView(R.layout.layout_change_email)
                val layoutParams = WindowManager.LayoutParams()
                layoutParams.copyFrom(window?.attributes)
                layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
                layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
                window?.attributes = layoutParams
                window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                window?.setWindowAnimations(R.style.DialogAnimation)
                val buttonUpdate: Button = findViewById(R.id.button_update)
                val buttonCancel: Button = findViewById(R.id.button_cancel)
                val editTextEmail: EditText = findViewById(R.id.edit_text_email)
                val editTextPassword: EditText = findViewById(R.id.edit_text_password)
                editTextEmail.setText(textEmail.text.toString())
                buttonUpdate.setOnClickListener {
                    if (editTextEmail.text.trim().toString().isNullOrEmpty()){
                        editTextEmail.error = "Please enter your email."
                        editTextEmail.requestFocus()
                        return@setOnClickListener
                    }
                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(editTextEmail.text.toString()).matches()) {
                        editTextEmail.error = "Invalid email address format."
                        editTextEmail.requestFocus()
                        return@setOnClickListener
                    }
                    if (editTextPassword.text.trim().toString().isNullOrEmpty()){
                        editTextPassword.error = "Please enter your current password before updating your email."
                        editTextPassword.requestFocus()
                        return@setOnClickListener
                    }
                    updateEmail(editTextEmail.text.toString(), editTextPassword.text.toString(), this)
                }
                buttonCancel.setOnClickListener {
                    dismiss()
                }
                show()
            }
        }
        cardChangePassword.setOnClickListener {
            Dialog(this).apply {
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                setCancelable(true)
                setContentView(R.layout.layout_change_password)
                val layoutParams = WindowManager.LayoutParams()
                layoutParams.copyFrom(window?.attributes)
                layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
                layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
                window?.attributes = layoutParams
                window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                window?.setWindowAnimations(R.style.DialogAnimation)
                val buttonUpdate: Button = findViewById(R.id.button_update)
                val buttonCancel: Button = findViewById(R.id.button_cancel)
                val editTextOldPassword: EditText = findViewById(R.id.edit_text_old_password)
                val editTextNewPassword: EditText = findViewById(R.id.edit_text_new_password)
                buttonUpdate.setOnClickListener {
                    if (editTextOldPassword.text.toString().isNullOrEmpty()) {
                        editTextOldPassword.error = "Please enter your old password"
                        editTextOldPassword.requestFocus()
                        return@setOnClickListener
                    }
                    if (editTextNewPassword.text.toString().isNullOrEmpty()) {
                        editTextNewPassword.error = "Please enter your new password"
                        editTextNewPassword.requestFocus()
                        return@setOnClickListener
                    }
                    if (!isValidPassword(editTextNewPassword.text.toString())) {
                        editTextNewPassword.error = "Invalid password format"
                        editTextNewPassword.requestFocus()
                        return@setOnClickListener
                    }
                    if (editTextNewPassword.text.toString().length < 8) {
                        editTextNewPassword.error = "Password length cannot less than 8 characters"
                        editTextNewPassword.requestFocus()
                        return@setOnClickListener
                    }
                    updatePassword(editTextNewPassword.text.toString(), editTextOldPassword.text.toString(), this)
                }
                buttonCancel.setOnClickListener {
                    dismiss()
                }
                show()
            }
        }
        cardDepartment.setOnClickListener {
            Dialog(this).apply {
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                setCancelable(true)
                setContentView(R.layout.layout_change_department)
                val layoutParams = WindowManager.LayoutParams()
                layoutParams.copyFrom(window?.attributes)
                layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT
                layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
                window?.attributes = layoutParams
                window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                window?.setWindowAnimations(R.style.DialogAnimation)
                val buttonSave: Button = findViewById(R.id.button_save)
                val buttonCancel: Button = findViewById(R.id.button_cancel)
                val spinnerDepartment: Spinner = findViewById(R.id.spinner_department)
                loadDepartment(spinnerDepartment)
                buttonSave.setOnClickListener {
                    updateDepartment(spinnerDepartment.selectedItem.toString(), this)
                }
                buttonCancel.setOnClickListener {
                    dismiss()
                }
                show()
            }
        }
        cardRegisterAsManagement.setOnClickListener {
            loadIsRegistered()
            if (!isLegitUser){
                Toast.makeText(this, "Please update your Phone Number & NRIC in order to register as a Management.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            if (isRegistered){
                Toast.makeText(this, "Your registration's already made and it's still pending.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Register as Management")
            builder.setMessage("Are you sure you want to register as a Management?")
            builder.setCancelable(true)
            builder.setPositiveButton("Yes") { dialog, _ ->
                dialog.dismiss()
                registerAsManagement()
            }
            builder.setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            val alert = builder.create()
            alert.show()
        }

        cardDeleteAccount.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Are you sure?")
            builder.setMessage("Deleting this account will result in completely removing your account from MyComplaint system and you won't able to access the app.")
            builder.setCancelable(true)
            builder.setPositiveButton("Yes") { dialog, _ ->
                dialog.dismiss()
                deleteAccount()
            }
            builder.setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            val alert = builder.create()
            alert.show()
        }
    }

    private fun updateDepartment(department: String, dialog: Dialog) {
        val userUID = Constants.userUID
        val updateDepart = mapOf(
            "user_department" to department
        )
        reference = database.getReference("User/$userUID")
        reference.updateChildren(updateDepart).addOnCompleteListener {
            if (it.isSuccessful){
                dialog.dismiss()
                Toast.makeText(this, "Department updated successfully", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadDepartment(spinnerDepartment: Spinner) {
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
                        this@UserProfileViewMoreActivity,
                        android.R.layout.simple_list_item_1, departmentList
                    )
                    adapter.setDropDownViewResource(android.R.layout.simple_list_item_1)
                    spinnerDepartment.adapter = adapter
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

    private fun updatePassword(newPassword: String, oldPassword: String, dialog: Dialog) {
        dialog.button_cancel.isEnabled = false
        dialog.button_update.isEnabled = false
        val user = FirebaseAuth.getInstance().currentUser
        val credential = EmailAuthProvider.getCredential(user!!.email!!, oldPassword)
        user.reauthenticate(credential).addOnCompleteListener {
            if (it.isSuccessful){
                user.updatePassword(newPassword).addOnCompleteListener { task ->
                    if (task.isSuccessful){
                        dialog.dismiss()
                        Toast.makeText(this, "Password updated successfully", Toast.LENGTH_SHORT).show()
                    }
                }.addOnFailureListener { task ->
                    Toast.makeText(this, task.message.toString(), Toast.LENGTH_LONG).show()
                    dialog.button_update.isEnabled = true
                    dialog.button_cancel.isEnabled = true
                }
            }else{
                Toast.makeText(this, it.exception.toString(), Toast.LENGTH_LONG).show()
                dialog.button_update.isEnabled = true
                dialog.button_cancel.isEnabled = true
            }
        }
    }

    fun isValidPassword(password: String?): Boolean {
        password?.let {
            val passwordPattern = "^(?=.*[0-9])(?=.*[a-z])(?=\\S+$).{4,}$"
            val passwordMatcher = Regex(passwordPattern)

            return passwordMatcher.find(password) != null
        } ?: return false
    }

    private fun updateEmail(email: String, password: String, dialog: Dialog) {
        dialog.button_update.isEnabled = false
        dialog.button_cancel.isEnabled = false
        val user = FirebaseAuth.getInstance().currentUser
        val credential = EmailAuthProvider.getCredential(user!!.email!!, password)
        user.reauthenticate(credential).addOnCompleteListener {
            if (it.isSuccessful){
                user.updateEmail(email).addOnCompleteListener { task ->
                    if (task.isSuccessful){
                        updateEmailToFirebase(email, dialog)
                    }
                }.addOnFailureListener { task ->
                    Toast.makeText(this, task.message.toString(), Toast.LENGTH_LONG).show()
                    dialog.button_update.isEnabled = true
                    dialog.button_cancel.isEnabled = true
                }
            }else{
                Toast.makeText(this, it.exception.toString(), Toast.LENGTH_LONG).show()
                dialog.button_update.isEnabled = true
                dialog.button_cancel.isEnabled = true
            }
        }
    }

    private fun updateEmailToFirebase(email: String, dialog: Dialog) {
        val uid = Constants.userUID
        reference = database.getReference("User/$uid")
        val updateEmail = mapOf(
            "user_email" to email
        )
        reference.updateChildren(updateEmail).addOnCompleteListener {
            if (it.isSuccessful){
                dialog.dismiss()
                Toast.makeText(this, "Email updated successfully", Toast.LENGTH_SHORT).show()
            }else{
                Toast.makeText(this, it.exception.toString(), Toast.LENGTH_LONG).show()
                dialog.button_update.isEnabled = true
                dialog.button_cancel.isEnabled = true
            }
        }
    }

    private fun loadIsRegistered() {
        val uid = Constants.userUID
        reference = database.getReference("RegisterManagement/$uid")
        reference.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()){
                    isRegistered = snapshot.child("register_status").value.toString() == "Pending"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

    private fun loadUserData() {
        val uid = Constants.userUID
        reference = database.getReference("User/$uid")
        reference.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()){
                    isLegitUser = snapshot.child("user_nric").value.toString() != "" && snapshot.child("user_phone_num").value.toString() != ""
                    textEmail.text = snapshot.child("user_email").value.toString()

                    if (snapshot.child("user_role").value.toString() == "Resident"){
                        cardDepartment.visibility = View.GONE
                        cardRegisterAsManagement.visibility = View.VISIBLE
                    }else if (snapshot.child("user_role").value.toString() == "Management"){
                        textDepartment.text = snapshot.child("user_department").value.toString()
                        cardDepartment.visibility = View.VISIBLE
                        cardRegisterAsManagement.visibility = View.GONE
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

    private fun deleteAccount() {
        val user = FirebaseAuth.getInstance().currentUser
        user!!.delete().addOnCompleteListener {
            if (it.isSuccessful){
                removeUserToken()
            }
        }
    }

    private fun removeUserToken() {
        val uid = Constants.userUID
        val removeToken = mapOf(
            "user_token" to "",
            "user_email" to ""
        )
        reference = database.getReference("User/$uid")
        reference.updateChildren(removeToken).addOnCompleteListener{
            Constants.userUID = ""
            Constants.userName = ""
            Constants.userRole = ""
            Dialog(this).apply {
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                setCancelable(false)
                setContentView(R.layout.layout_account_deleted)
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
                    val intent = Intent(this@UserProfileViewMoreActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                }
                show()
            }
        }
    }

    private fun registerAsManagement() {
        val uid = Constants.userUID ?:""
        val registerStatus = "Pending"
        reference = database.getReference("RegisterManagement")
        val registerManagement = RegisterManagement(uid, registerStatus)
        reference.child(uid).setValue(registerManagement).addOnCompleteListener {
            if (it.isSuccessful){
                isRegistered = true
                notifyAdmin()
                Dialog(this).apply {
                    requestWindowFeature(Window.FEATURE_NO_TITLE)
                    setCancelable(true)
                    setContentView(R.layout.layout_register_management_successful)
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
    }

    private fun notifyAdmin() {
        val title = "Management Registration"
        val message = "Someone has requested to register as a Management."

        reference = database.getReference("User")
        reference.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                adminUID.clear()
                if (snapshot.exists()){
                    for (data in snapshot.children){
                        if (data.child("user_role").value.toString() == "Admin" && data.child("user_token").value.toString() != ""){
                            adminUID.add(data.child("user_uid").value.toString())
                            PushNotification(
                                NotificationData(
                                    title, message
                                ), data.child("user_token").value.toString()
                            ).also {
                                sendNotification(it)
                            }
                        }
                    }
                }
                for (userUID in adminUID){
                    addNewNotification(userUID, title, message)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

    private fun sendNotification(notification: PushNotification) = CoroutineScope(Dispatchers.IO).launch {
        try {
            val response = RetrofitInstance.api.postNotification(notification)
            if (response.isSuccessful){
                Log.d("Success", "Notification sent")
            }else{
                Log.d("Failed", "Notification unsent")
            }
        }catch (e: Exception){
            Log.e("Error", e.toString())
        }
    }

    private fun addNewNotification(userUID: String, title: String, message: String) {
        reference = database.getReference("Notification/$userUID")
        val notificationUID = reference.push().key?:""
        val currentDate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocalDateTime.now()
        } else {
            TODO("VERSION.SDK_INT < O")
        }
        val notificationCategory = 3 // Register Management
        val notificationStatus = "Unread"
        val newNotification = Notification(notificationUID, title, message, currentDate.toString(), notificationCategory, notificationStatus, userUID)
        reference.child(notificationUID).setValue(newNotification)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}