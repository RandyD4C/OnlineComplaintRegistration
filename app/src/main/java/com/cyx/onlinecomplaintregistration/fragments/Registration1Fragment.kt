package com.cyx.onlinecomplaintregistration.fragments

import android.app.Activity.RESULT_OK
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.cyx.onlinecomplaintregistration.R
import com.cyx.onlinecomplaintregistration.classes.Constants
import com.cyx.onlinecomplaintregistration.interfaces.Communicator
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import java.util.regex.Matcher
import java.util.regex.Pattern

class Registration1Fragment : Fragment() {

    private lateinit var imageViewAvatar: ImageView
    private lateinit var editTextFullName: EditText
    private lateinit var editTextUserName: EditText
    private lateinit var editTextEmail: EditText
    private lateinit var buttonBack: Button
    private lateinit var buttonNext: Button
    private lateinit var cardView: CardView
    private var fullName: String? = null
    private var userName: String? = null
    private var email: String? = null
    private var avatar: Uri? = null
    private var password: String? = null
    private lateinit var communicator: Communicator
    private lateinit var database: FirebaseDatabase
    private lateinit var reference: DatabaseReference
    private lateinit var progressBar: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_registration1, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        email = arguments?.getString("email")
        password = arguments?.getString("password")
        fullName = arguments?.getString("fullname")
        userName = arguments?.getString("username")
        avatar = arguments?.getString("avatar")?.toUri()

        imageViewAvatar = view.findViewById(R.id.image_view_avatar)
        editTextFullName = view.findViewById(R.id.edit_text_full_name)
        editTextUserName = view.findViewById(R.id.edit_text_user_name)
        editTextEmail = view.findViewById(R.id.edit_text_email)
        buttonBack = view.findViewById(R.id.button_back)
        buttonNext = view.findViewById(R.id.button_next)
        cardView = view.findViewById(R.id.cardView2)

        database = Constants.database

        if (avatar.toString() != "null"){
            imageViewAvatar.setImageURI(avatar)
        }else{
            avatar = Uri.parse("android.resource://com.cyx.onlinecomplaintregistration/${R.drawable.useravatar}")
            imageViewAvatar.setImageURI(avatar)
        }
        if (!fullName.isNullOrEmpty()) editTextFullName.setText(fullName)
        if (!userName.isNullOrEmpty()) editTextUserName.setText(userName)
        if (!email.isNullOrEmpty()) editTextEmail.setText(email)

        communicator = activity as Communicator

        cardView.setOnClickListener {
            browseGallery()
        }

        buttonBack.setOnClickListener {
            activity?.finish()
        }

        buttonNext.setOnClickListener {
            proceedNextPage()
        }
    }

    private fun proceedNextPage() {

        val fullName = editTextFullName.text.toString().uppercase()
        val digit: Pattern = Pattern.compile("[0-9]")
        val special: Pattern = Pattern.compile("[!@#$%&*()_+=,.`|<>?{}\\[\\]~-]")

        val hasDigit: Matcher = digit.matcher(fullName)
        val hasSpecial: Matcher = special.matcher(fullName)
        val validUserName: Matcher = special.matcher(editTextUserName.text.toString())

        if (fullName.isNullOrEmpty()) {
            editTextFullName.error = "Your full name cannot be blank"
            editTextFullName.requestFocus()
            return
        }
        if (hasDigit.find()) {
            editTextFullName.error = "Full Name cannot contain numbers!"
            editTextFullName.requestFocus()
            return
        }
        if (hasSpecial.find()) {
            editTextFullName.error = "Full Name cannot contain special characters!"
            editTextFullName.requestFocus()
            return
        }
        if (editTextUserName.text.toString().isNullOrEmpty()) {
            editTextUserName.error = "Your username cannot be blank"
            editTextUserName.requestFocus()
            return
        }
        if (validUserName.find()) {
            editTextUserName.error = "Username cannot contain special characters."
            editTextUserName.requestFocus()
            return
        }
        if (editTextEmail.text.toString().isNullOrEmpty()) {
            editTextEmail.error = "Your email cannot be blank"
            editTextEmail.requestFocus()
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(editTextEmail.text.toString()).matches()) {
            editTextEmail.error = "Please enter valid email address"
            editTextEmail.requestFocus()
            return
        }
        if (password.isNullOrEmpty()){
            communicator.passDataCom(
                editTextFullName.text.toString(),
                editTextUserName.text.toString(),
                editTextEmail.text.toString(),
                "",
                avatar
            )
        }else{
            communicator.passDataCom(
                editTextFullName.text.toString(),
                editTextUserName.text.toString(),
                editTextEmail.text.toString(),
                password,
                avatar
            )
        }
    }

    private fun browseGallery() {
        CropImage.activity()
            .setGuidelines(CropImageView.Guidelines.ON)
            .start(requireContext(),this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            var result = CropImage.getActivityResult(data)
            if (resultCode === RESULT_OK) {
                avatar = result.uri
                imageViewAvatar.setImageURI(avatar)
            } else if (resultCode === CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Toast.makeText(requireContext(),result.error.toString(),Toast.LENGTH_LONG).show()
            }

        }
    }

}