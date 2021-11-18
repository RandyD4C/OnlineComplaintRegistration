package com.cyx.onlinecomplaintregistration.resident.activities.user

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.cardview.widget.CardView
import com.bumptech.glide.Glide
import com.cyx.onlinecomplaintregistration.R
import com.cyx.onlinecomplaintregistration.classes.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE
import com.theartofdev.edmodo.cropper.CropImage.getActivityResult
import com.theartofdev.edmodo.cropper.CropImageView
import kotlinx.android.synthetic.main.activity_edit_complaint.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern

class MyAccountActivity : AppCompatActivity() {

    private lateinit var imageViewAvatar: ImageView
    private lateinit var editTextFullName: EditText
    private lateinit var editTextUserName: EditText
    private lateinit var editTextIcNumber: EditText
    private lateinit var editTextPhoneNumber: EditText
    private lateinit var cardView: CardView
    private lateinit var database: FirebaseDatabase
    private lateinit var reference: DatabaseReference
    private lateinit var textViewMore: TextView
    private lateinit var progressDialog: ProgressDialog
    private var nricList = mutableListOf<String>()
    private var phoneNumList = mutableListOf<String>()
    private var selectedAvatar: Uri? = null
    private var avatarUrl: String? = null
    private var complaintUID = mutableListOf<String>()
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_account_resident)
        this.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "My Profile"
        supportActionBar?.elevation = 0F

        imageViewAvatar = findViewById(R.id.image_view_avatar)
        editTextFullName = findViewById(R.id.edit_text_full_name)
        editTextUserName = findViewById(R.id.edit_text_user_name)
        editTextIcNumber = findViewById(R.id.edit_text_nric)
        editTextPhoneNumber = findViewById(R.id.edit_text_phone_num)
        textViewMore = findViewById(R.id.text_view_more)
        cardView = findViewById(R.id.cardView2)
        progressBar = findViewById(R.id.progress_bar)

        Constants.sharedPref =
            getSharedPreferences("sharedPref", Context.MODE_PRIVATE)

        database = Constants.database
        loadUserData()
        loadNricAndPhoneNum()
        loadComplaintCount()

        cardView.setOnClickListener {
            browseGallery()
        }

        textViewMore.setOnClickListener {
            val intent = Intent(this, UserProfileViewMoreActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadComplaintCount() {
        val uid = Constants.userUID
        reference = database.getReference("Complaint/$uid")
        reference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                complaintUID.clear()
                if (snapshot.exists()) {
                    for (data in snapshot.children) {
                        complaintUID.add(data.child("complaint_uid").value.toString())
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

    private fun loadNricAndPhoneNum() {
        val uid = Constants.userUID
        reference = database.getReference("User")
        reference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                nricList.clear()
                phoneNumList.clear()
                if (snapshot.exists()) {
                    for (data in snapshot.children) {
                        if (data.child("user_uid").value.toString() != uid) {
                            if (data.child("user_nric").value.toString() != "") {
                                nricList.add(data.child("user_nric").value.toString())
                            }
                            if (data.child("user_phone_num").value.toString() != "") {
                                phoneNumList.add(data.child("user_phone_num").value.toString())
                            }
                        }
                    }
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
        reference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    //load avatar
                    try {
                        Glide.with(imageViewAvatar.context)
                            .load(snapshot.child("user_avatar").value.toString())
                            .apply(Constants.requestOptions)
                            .placeholder(R.drawable.ic_avatar)
                            .error(R.drawable.errorimg)
                            .into(imageViewAvatar)
                    } catch (e: Exception) {
                        Log.d("Exception", e.message.toString())
                    }
                    avatarUrl = snapshot.child("user_avatar").value.toString()

                    //load full name
                    editTextFullName.setText(snapshot.child("user_full_name").value.toString())

                    //load user name
                    editTextUserName.setText(snapshot.child("user_name").value.toString())

                    //load NRIC
                    editTextIcNumber.setText(snapshot.child("user_nric").value.toString())

                    //load phone number
                    editTextPhoneNumber.setText(snapshot.child("user_phone_num").value.toString())

                    //disable progress bar
                    progressBar.visibility = View.GONE
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_confirm -> {
                validateUserInput()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun validateUserInput() {
        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Updating Profile...")
        progressDialog.setCancelable(false)
        progressDialog.show()

        val fullName = editTextFullName.text.toString().uppercase(Locale.getDefault())
        val digit: Pattern = Pattern.compile("[0-9]")
        val special: Pattern = Pattern.compile("[!@#$%&*()_+=,.`|<>?{}\\[\\]~-]")
        val nric: Pattern =
            Pattern.compile("(([0-9]{2})(0[1-9]|1[0-2])(0[1-9]|[12][0-9]|3[01]))-([0-9]{2})-([0-9]{4})")

        val hasDigit: Matcher = digit.matcher(fullName)
        val hasSpecial: Matcher = special.matcher(fullName)
        val isValidNric: Matcher = nric.matcher(editTextIcNumber.text.toString())
        val validUserName: Matcher = special.matcher(editTextUserName.text.toString())

        if (fullName.isNullOrEmpty()) {
            editTextFullName.error = "Your full name cannot be blank"
            editTextFullName.requestFocus()
            if (progressDialog.isShowing) progressDialog.dismiss()
            return
        }
        if (hasDigit.find()) {
            editTextFullName.error = "Full Name cannot contain numbers!"
            editTextFullName.requestFocus()
            if (progressDialog.isShowing) progressDialog.dismiss()
            return
        }
        if (hasSpecial.find()) {
            editTextFullName.error = "Full Name cannot contain special characters!"
            editTextFullName.requestFocus()
            if (progressDialog.isShowing) progressDialog.dismiss()
            return
        }
        if (editTextUserName.text.toString().isNullOrEmpty()) {
            editTextUserName.error = "Your username cannot be blank"
            editTextUserName.requestFocus()
            if (progressDialog.isShowing) progressDialog.dismiss()
            return
        }
        if (validUserName.find()) {
            editTextUserName.error = "Username cannot contain special characters."
            editTextUserName.requestFocus()
            return
        }
        if (!editTextIcNumber.text.toString().isNullOrEmpty()) {
            if (!isValidNric.find()) {
                editTextIcNumber.error = "Invalid NRIC format. NRIC must contain '-'."
                editTextIcNumber.requestFocus()
                if (progressDialog.isShowing) progressDialog.dismiss()
                return
            }
        }
        if (editTextIcNumber.text.toString() in nricList) {
            editTextIcNumber.error = "This NRIC already existed."
            editTextIcNumber.requestFocus()
            if (progressDialog.isShowing) progressDialog.dismiss()
            return
        }
        if (!editTextPhoneNumber.text.toString().isNullOrEmpty()) {
            if (!isValidPhoneNum(editTextPhoneNumber.text.toString())) {
                editTextPhoneNumber.error = "Invalid phone number format!"
                editTextPhoneNumber.requestFocus()
                if (progressDialog.isShowing) progressDialog.dismiss()
                return
            }
        }
        if (editTextPhoneNumber.text.toString().replace("-", "") in phoneNumList) {
            editTextPhoneNumber.error = "This phone number already existed."
            editTextPhoneNumber.requestFocus()
            if (progressDialog.isShowing) progressDialog.dismiss()
            return
        }

        if (Constants.userRole == "Management") {
            if (editTextIcNumber.text.toString().isNullOrEmpty()){
                editTextIcNumber.error = "Your NRIC number cannot be blank"
                editTextIcNumber.requestFocus()
                if (progressDialog.isShowing) progressDialog.dismiss()
                return
            }
            if (editTextPhoneNumber.text.toString().isNullOrEmpty()){
                editTextPhoneNumber.error = "Your phone number cannot be blank"
                editTextPhoneNumber.requestFocus()
                if (progressDialog.isShowing) progressDialog.dismiss()
                return
            }
        }
        selectedAvatar?.let {
            saveToFirebaseStorage()
        } ?: saveUserData()
    }

    private fun saveToFirebaseStorage() = CoroutineScope(Dispatchers.IO).launch {
        val uid = Constants.userUID
        val fileName = UUID.randomUUID().toString()
        val data = async { deleteOldAvatar(uid!!) }
        data.await()
        val storageReference =
            FirebaseStorage.getInstance().getReference("UserAvatar/$uid/$fileName")
        storageReference.putFile(selectedAvatar!!).addOnSuccessListener {
            storageReference.downloadUrl.addOnSuccessListener {
                avatarUrl = it.toString()
                saveUserData()
            }
        }.addOnFailureListener {
            Toast.makeText(this@MyAccountActivity, "Failed to upload image.", Toast.LENGTH_SHORT)
                .show()
            if (progressDialog.isShowing) progressDialog.dismiss()
        }.addOnProgressListener {
            val progress: Long = (100 * it.bytesTransferred) / it.totalByteCount
            progressDialog.setMessage("Updating Profile: $progress %")
        }
    }

    private fun deleteOldAvatar(uid: String): String {
        try {
            val oldProfilePic = FirebaseStorage.getInstance().getReference("UserAvatar/$uid")
            oldProfilePic.listAll().addOnCompleteListener {
                for (item in it.result.items) {
                    item.delete()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this@MyAccountActivity, e.message, Toast.LENGTH_SHORT).show()
        }
        return ""
    }

    private fun saveUserData() {
        val uid = Constants.userUID
        val updateUserDetails = if (selectedAvatar == null) mapOf(
            "user_full_name" to editTextFullName.text.toString().uppercase(Locale.getDefault()),
            "user_name" to editTextUserName.text.toString(),
            "user_nric" to editTextIcNumber.text.toString(),
            "user_phone_num" to editTextPhoneNumber.text.toString()
        ) else mapOf(
            "user_avatar" to avatarUrl!!,
            "user_full_name" to editTextFullName.text.toString().uppercase(Locale.getDefault()),
            "user_name" to editTextUserName.text.toString(),
            "user_nric" to editTextIcNumber.text.toString(),
            "user_phone_num" to editTextPhoneNumber.text.toString()
        )
        reference = database.getReference("User")
        reference.child(uid!!).updateChildren(updateUserDetails).addOnSuccessListener {
            if (complaintUID.size > 0) {
                updateComplaintAvatar(uid)
                if (progressDialog.isShowing) progressDialog.dismiss()
                val toast = Toast.makeText(applicationContext, "Profile Saved", Toast.LENGTH_SHORT)
                toast.show()

            } else {
                if (progressDialog.isShowing) progressDialog.dismiss()
                val toast = Toast.makeText(applicationContext, "Profile Saved", Toast.LENGTH_SHORT)
                toast.show()
            }
        }
    }

    private fun updateComplaintAvatar(uid: String) {
        val updateUserDetails = mapOf<String, String>(
            "user_avatar" to avatarUrl!!,
            "user_name" to editTextUserName.text.toString()
        )
        reference = database.getReference("Complaint")
        for (data in complaintUID) {
            reference.child("$uid/$data").updateChildren(updateUserDetails)
        }
    }

    fun isValidPhoneNum(phone: String?): Boolean {
        phone?.let {
            val phonePattern = "^(\\+?6?01)[0-46-9]-*[0-9]{7,8}\$"
            val phoneMatcher = Regex(phonePattern)

            return phoneMatcher.find(phone) != null
        } ?: return false
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.register_complaint, menu)
        return true
    }

    private fun browseGallery() {
        CropImage
            .activity()
            .setGuidelines(CropImageView.Guidelines.ON)
            .start(this)
//        startActivityForResult(CropImage.getPickImageChooserIntent(this),200)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            var result = getActivityResult(data)
            if (resultCode === RESULT_OK) {
                imageViewAvatar.setImageURI(result.uri)
                selectedAvatar = result.uri
            } else if (resultCode === CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Toast.makeText(this, result.error.toString(), Toast.LENGTH_LONG).show()
            }
        }
    }
}
