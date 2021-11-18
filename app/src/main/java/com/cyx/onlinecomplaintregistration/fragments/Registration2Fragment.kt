package com.cyx.onlinecomplaintregistration.fragments

import android.app.Activity
import android.app.Dialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings.Global.putString
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.cyx.onlinecomplaintregistration.R
import com.cyx.onlinecomplaintregistration.activities.LoginActivity
import com.cyx.onlinecomplaintregistration.classes.Constants
import com.cyx.onlinecomplaintregistration.classes.User
import com.cyx.onlinecomplaintregistration.interfaces.Communicator
import com.cyx.onlinecomplaintregistration.resident.activities.ResidentMainActivity
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import java.util.*


class Registration2Fragment : Fragment() {

    private lateinit var imageViewAvatar: ImageView
    private lateinit var editTextPassword: EditText
    private lateinit var editTextReEnterPassword: EditText
    private lateinit var buttonBack: Button
    private lateinit var buttonConfirm: Button
    private lateinit var cardView: CardView
    private lateinit var imageViewCheck1: ImageView
    private lateinit var imageViewCheck2: ImageView
    private var fullName: String? = null
    private var userName: String? = null
    private var email: String? = null
    private var avatar: Uri? = null
    private var password: String? = null
    private lateinit var communicator: Communicator
    private var isAtLeastEight: Boolean = false
    private var hasNumber: Boolean = false
    private lateinit var database: FirebaseDatabase
    private lateinit var progressBar: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_registration2, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        email = arguments?.getString("email")
        password = arguments?.getString("password")
        fullName = arguments?.getString("fullname")
        userName = arguments?.getString("username")
        avatar = arguments?.getString("avatar")?.toUri()

        imageViewAvatar = view.findViewById(R.id.image_view_avatar)
        editTextPassword = view.findViewById(R.id.edit_text_password)
        editTextReEnterPassword = view.findViewById(R.id.edit_text_reenter_password)
        buttonBack = view.findViewById(R.id.button_back)
        buttonConfirm = view.findViewById(R.id.button_confirm)
        cardView = view.findViewById(R.id.cardView2)
        imageViewCheck1 = view.findViewById(R.id.image_view_check_1)
        imageViewCheck2 = view.findViewById(R.id.image_view_check_2)

        if (avatar.toString() != "null") {
            imageViewAvatar.setImageURI(avatar)
        } else {
            avatar = Uri.parse("android.resource://com.cyx.onlinecomplaintregistration/${R.drawable.useravatar}")
            imageViewAvatar.setImageURI(avatar)
        }
        if (!password.isNullOrEmpty()) editTextPassword.setText(password)
        database = Constants.database

        checkPassword()

        communicator = activity as Communicator

        cardView.setOnClickListener {
            browseGallery()
        }

        buttonBack.setOnClickListener {
            communicator.goBackFragment1(
                fullName,
                userName,
                email.toString(),
                editTextPassword.text.toString(),
                avatar
            )
        }

        buttonConfirm.setOnClickListener {
            validatePassword(view)
        }

        editTextPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
//                checkPassword()
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                checkPassword()
            }

            override fun afterTextChanged(p0: Editable?) {
//                TODO("Not yet implemented")
            }

        })
    }

    private fun validatePassword(view: View) {
        if (editTextPassword.text.toString().isNullOrEmpty()) {
            editTextPassword.error = "Please provide your password"
            editTextPassword.requestFocus()
            return
        }
        if (editTextReEnterPassword.text.toString().isNullOrEmpty()) {
            editTextReEnterPassword.error = "Please re-enter your password"
            editTextReEnterPassword.requestFocus()
            return
        }
        if (!isValidPassword(editTextPassword.text.toString())) {
            editTextPassword.error = "Invalid password format"
            editTextPassword.requestFocus()
            return
        }
        if (editTextPassword.text.toString().length < 8) {
            editTextPassword.error = "Password length cannot less than 8 characters"
            editTextPassword.requestFocus()
            return
        }
        if (editTextPassword.text.toString() != editTextReEnterPassword.text.toString()) {
            editTextReEnterPassword.error = "Re-enter password must be the same with above"
            editTextReEnterPassword.requestFocus()
            return
        }
        saveToAuthentication(view)
    }

    private fun checkPassword() {
        if (editTextPassword.text.toString().length >= 8) {
            isAtLeastEight = true
            imageViewCheck2.setColorFilter(
                Color.parseColor("#4CAF50")
            )
        } else {
            isAtLeastEight = false
            imageViewCheck2.setColorFilter(
                Color.parseColor("#b3b3b3")
            )
        }

        if (editTextPassword.text.toString().matches("(.*[0-9].*)".toRegex())) {
            hasNumber = true
            imageViewCheck1.setColorFilter(
                Color.parseColor("#4CAF50")
            )
        } else {
            hasNumber = false
            imageViewCheck1.setColorFilter(
                Color.parseColor("#b3b3b3")
            )
        }
    }

    fun isValidPassword(password: String?): Boolean {
        password?.let {
            val passwordPattern = "^(?=.*[0-9])(?=.*[a-z])(?=\\S+$).{4,}$"
            val passwordMatcher = Regex(passwordPattern)

            return passwordMatcher.find(password) != null
        } ?: return false
    }

    private fun saveToFirebaseStorage(view: View) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let{
            val uid = FirebaseAuth.getInstance().uid
            val fileName = UUID.randomUUID().toString()
            val storageReference = FirebaseStorage.getInstance().getReference("UserAvatar/$uid/$fileName")
            storageReference.putFile(avatar!!).addOnSuccessListener {
                storageReference.downloadUrl.addOnSuccessListener {
                    Log.d("Register 3","Here")
                    saveUserToFirebaseDatabase(it.toString(), view)
                }
            }.addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to upload image.", Toast.LENGTH_SHORT)
                    .show()
                if (progressBar.isShowing) progressBar.dismiss()
            }.addOnProgressListener {
                val progress: Long = (100 * it.bytesTransferred) / it.totalByteCount
                progressBar.setMessage("Creating account: $progress %")
            }
        }
    }

    private fun saveToAuthentication(view: View) {
        progressBar = ProgressDialog(view.context)
        progressBar.setMessage("Creating account...")
        progressBar.setCancelable(false)
        progressBar.show()
        FirebaseAuth.getInstance()
            .createUserWithEmailAndPassword(email!!, editTextPassword.text.toString()!!)
            .addOnCompleteListener {
                saveToFirebaseStorage(view)
            }.addOnFailureListener {
                if (progressBar.isShowing) progressBar.dismiss()
                val snackbar = Snackbar.make(
                    requireView(), "Failed to register account: ${it.message}",
                    Snackbar.LENGTH_LONG
                ).setAction("Action", null)
                snackbar.show()
            }
    }

    private fun saveUserToFirebaseDatabase(url: String, view: View) {
        val uid = FirebaseAuth.getInstance().uid ?: ""
        val ref = database.getReference("User/$uid")
        val phoneNum = ""
        val department = ""
        val nric = ""
        val userRole = "Resident"
        val userStatus = "Active"

        val user = User(
            uid,
            fullName!!.uppercase(Locale.getDefault()),
            userName!!,
            nric,
            email!!,
            phoneNum,
            url,
            department,
            userRole,
            userStatus
        )

        ref.setValue(user).addOnSuccessListener {
            if (progressBar.isShowing) progressBar.dismiss()
            Dialog(view.context).apply {
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                setCancelable(false)
                setContentView(R.layout.layout_registration_successful)
                val buttonYes = findViewById<Button>(R.id.button_yes)
                buttonYes.setOnClickListener {
                    dismiss()
                    FirebaseAuth.getInstance().signOut()
                    val intent = Intent(requireContext(), LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                }
                val layoutParams = WindowManager.LayoutParams()
                layoutParams.copyFrom(window?.attributes)
                layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT
                layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
                window?.attributes = layoutParams
                window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                window?.setWindowAnimations(R.style.DialogAnimation)
                show()
            }
        }.addOnFailureListener {
            if (progressBar.isShowing) progressBar.dismiss()
            val builder = AlertDialog.Builder(view.context)
            builder.setMessage("Failed to register account")
            builder.setTitle("Error")
            builder.setCancelable(false)
            builder.setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            val alert = builder.create()
            alert.show()
        }
    }

    private fun browseGallery() {
        CropImage.activity()
            .setGuidelines(CropImageView.Guidelines.ON)
            .start(requireContext(),this);
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            var result = CropImage.getActivityResult(data)
            if (resultCode === Activity.RESULT_OK) {
                avatar = result.uri
                imageViewAvatar.setImageURI(avatar)
            } else if (resultCode === CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Toast.makeText(requireContext(),result.error.toString(),Toast.LENGTH_LONG).show()
            }

        }
    }
}