package com.cyx.onlinecomplaintregistration.resident.activities.home

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatSpinner
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.util.*
import com.cyx.onlinecomplaintregistration.R
import com.cyx.onlinecomplaintregistration.classes.*
import com.cyx.onlinecomplaintregistration.classes.Notification
import com.cyx.onlinecomplaintregistration.classes.Constants
import com.cyx.onlinecomplaintregistration.resident.activities.ResidentMainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDateTime

class RegisteringComplaintActivity : AppCompatActivity() {

    private lateinit var textLocation: TextView
    private lateinit var spinnerComplaintPriority: AppCompatSpinner
    private lateinit var spinnerComplaintCategory: AppCompatSpinner
    private lateinit var editTextDescription: EditText
    private lateinit var imageViewComplaint: ImageView
    private lateinit var database: FirebaseDatabase
    private lateinit var reference: DatabaseReference
    private lateinit var lastLocation: Location
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var geocoder: Geocoder
    private var addresses = mutableListOf<Address>()
    private var address: String? = null
    private var city: String? = null
    private var state: String? = null
    private var country: String? = null
    private var postalCode: String? = null
    private var knownName: String? = null
    private var userLocation: String? = null
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var imageCapture: Uri? = null
    private lateinit var photoFile: File
    private val cameraRequest = 1
    private val REQUEST_PERM_WRITE_STORAGE = 1
    private lateinit var progressBar: ProgressDialog
    private var userName: String = ""
    private var userAvatar: String = ""
    private var complaintCategoryAdapter = mutableListOf<String>()
    private var managementUID = mutableListOf<String>()

    companion object {
        private const val LOCATION_REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registering_complaint)
        this.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Post Complaint"

        textLocation = findViewById(R.id.text_location)
        spinnerComplaintPriority = findViewById(R.id.spinner_complaint_priority)
        spinnerComplaintCategory = findViewById(R.id.spinner_complaint_category)
        editTextDescription = findViewById(R.id.edit_text_complaint_desc)
        imageViewComplaint = findViewById(R.id.image_complaint)

        imageCapture = intent.getParcelableExtra("imageUri")
        imageViewComplaint.setImageURI(imageCapture)

        Constants.sharedPref =
            getSharedPreferences("sharedPref", Context.MODE_PRIVATE)
        database = Constants.database
        getUserLocation()
        getUserData()
        loadSpinnerCategory()

        imageViewComplaint.setOnClickListener {
            Toast.makeText(this, "Long press to change photo", Toast.LENGTH_SHORT).show()
        }

        imageViewComplaint.setOnLongClickListener {
            it.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Change Photo")
            builder.setMessage("Re-take photo?")
            builder.setCancelable(true)
            builder.setPositiveButton("Yes") { dialog, _ ->
                dialog.dismiss()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.CAMERA
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(Manifest.permission.CAMERA),
                            REQUEST_PERM_WRITE_STORAGE
                        )
                    } else {
                        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                        photoFile = getOutputDirectory()
                        val fileProvider = FileProvider.getUriForFile(
                            this,
                            "com.cyx.onlinecomplaintregistration.fileprovider",
                            photoFile
                        )
                        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileProvider)
                        cameraIntent.resolveActivity(this.packageManager)?.let {
                            startActivityForResult(cameraIntent, cameraRequest)
                        } ?: Toast.makeText(this, "Unable to open camera", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
            builder.setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            val alert = builder.create()
            alert.show()
            return@setOnLongClickListener false
        }
    }

    private fun loadSpinnerCategory() {
        reference = database.getReference("Category")
        reference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                complaintCategoryAdapter.clear()
                if (snapshot.exists()) {
                    for (data in snapshot.children) {
                        complaintCategoryAdapter.add(data.child("category_name").value.toString())
                    }
                }
                val adapter = ArrayAdapter(
                    this@RegisteringComplaintActivity,
                    android.R.layout.simple_list_item_1, complaintCategoryAdapter
                )
                adapter.setDropDownViewResource(android.R.layout.simple_list_item_1)
                spinnerComplaintCategory.adapter = adapter
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

    private fun getUserData() {
        val uid = Constants.userUID
        reference = database.getReference("User/$uid")
        reference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    userName = snapshot.child("user_name").value.toString()
                    userAvatar = snapshot.child("user_avatar").value.toString()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_confirm -> {
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Register Complaint")
                builder.setMessage("Register This Complaint?")
                builder.setCancelable(true)
                builder.setPositiveButton("Yes") { dialog, _ ->
                    dialog.dismiss()
                    saveToFirebaseStorage()
                }
                builder.setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                }
                val alert = builder.create()
                alert.show()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == cameraRequest && resultCode == Activity.RESULT_OK) {
            imageViewComplaint.setImageURI(Uri.fromFile(photoFile.absoluteFile))
        } else {
            super.onActivityResult(requestCode, resultCode, data)
            Toast.makeText(this, "Something went wrong", Toast.LENGTH_SHORT).show()
        }
    }


    private fun getOutputDirectory(): File {
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val fileName = SimpleDateFormat(
            Constants.FILE_NAME_FORMAT,
            Locale.getDefault()
        ).format(System.currentTimeMillis())
        return File.createTempFile(fileName, ".jpg", storageDir)
    }

    private fun saveToFirebaseStorage() {
        progressBar = ProgressDialog(this)
        progressBar.setCancelable(false)
        progressBar.show()
        val fileName = UUID.randomUUID().toString()
        val userUID = Constants.userUID ?: ""
        val storageReference =
            FirebaseStorage.getInstance().getReference("Complaint/$userUID/$fileName")
        storageReference.putFile(imageCapture!!).addOnSuccessListener {
            storageReference.downloadUrl.addOnSuccessListener {
                registerComplaint(it.toString(), userUID)
            }
        }.addOnFailureListener {
            progressBar.dismiss()
            Toast.makeText(this, "Failed to upload image.", Toast.LENGTH_SHORT)
                .show()
        }.addOnProgressListener {
            val progress: Long = (100 * it.bytesTransferred) / it.totalByteCount
            progressBar.setMessage("Uploading to Cloud: $progress %")
        }
    }

    private fun registerComplaint(imageUri: String, userUID: String) {
        val currentDate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocalDateTime.now()
        } else {
            TODO("VERSION.SDK_INT < O")
        }
        reference = database.getReference("Complaint")
        val complaintUID = reference.push().key ?: ""
        val complaint = Complaint(
            complaintUID,
            spinnerComplaintPriority.selectedItemPosition,
            spinnerComplaintCategory.selectedItem.toString(),
            editTextDescription.text.toString(),
            imageUri,
            "",
            latitude,
            longitude,
            postalCode!!,
            currentDate.toString(),
            userUID,
            "",
            "",
            "Pending",
            userAvatar,
            userName
        )
        reference.child("$userUID/$complaintUID").setValue(complaint).addOnCompleteListener {
            if (it.isSuccessful) {
                if (spinnerComplaintPriority.selectedItemPosition == 2){
                    loadManagementToken()
                }

                if (progressBar.isShowing) progressBar.dismiss()
                Dialog(this).apply {
                    requestWindowFeature(Window.FEATURE_NO_TITLE)
                    setCancelable(false)
                    setContentView(R.layout.layout_complaint_successful)
                    val buttonYes = findViewById<Button>(R.id.button_yes)
                    buttonYes.setOnClickListener {
                        dismiss()
                        val intent = Intent(this@RegisteringComplaintActivity, ResidentMainActivity::class.java)
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

            } else {
                if (progressBar.isShowing) progressBar.dismiss()
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Error")
                builder.setMessage("Failed to register complaint.")
                builder.setCancelable(true)
                builder.setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                }
                val alert = builder.create()
                alert.show()
            }
        }
    }

    private fun loadManagementToken() {
        val title = "Urgent Complaint"
        val message = "An urgent complaint has been registered. Please check it out."

        reference = database.getReference("User")
        reference.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                managementUID.clear()
                if (snapshot.exists()){
                    for (data in snapshot.children){
                        if (data.child("user_role").value.toString() == "Management"){
                            managementUID.add(data.child("user_uid").value.toString())
                            if (data.child("user_token").value.toString() != ""){
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
                }
                for (userUID in managementUID){
                    addNewNotification(userUID, title, message)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })

    }

    private fun addNewNotification(userUID: String, title: String, message: String) {
        reference = database.getReference("Notification/$userUID")
        val notificationUID = reference.push().key?:""
        val currentDate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocalDateTime.now()
        } else {
            TODO("VERSION.SDK_INT < O")
        }
        val notificationCategory = 6 // Urgent Complaint
        val notificationStatus = "Unread"
        val newNotification = Notification(notificationUID, title, message, currentDate.toString(), notificationCategory, notificationStatus, userUID)
        reference.child(notificationUID).setValue(newNotification)
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.register_complaint, menu)
        return true
    }

    private fun getUserLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_REQUEST_CODE
            )
            ActivityCompat.requestPermissions(
                this, arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_REQUEST_CODE
            )
            return
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.lastLocation.addOnSuccessListener(this) { location ->
            location?.let {
                lastLocation = it
                val currentLatLong = LatLng(it.latitude, it.longitude)
                latitude = currentLatLong.latitude
                longitude = currentLatLong.longitude
                geocoder = Geocoder(this, Locale.getDefault())
                addresses =
                    geocoder.getFromLocation(latitude, longitude, 1)
                address = addresses[0].getAddressLine(0)
                city = addresses[0].locality
                state = addresses[0].adminArea
                country = addresses[0].countryName
                postalCode = addresses[0].postalCode
                knownName = addresses[0].featureName

                userLocation = "$address"
                textLocation.text = userLocation
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onBackPressed() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Back")
        builder.setMessage("Are you sure you want to exit? All your progress will be discarded.")
        builder.setCancelable(true)
        builder.setPositiveButton("Yes") { dialog, _ ->
            dialog.dismiss()
            super.onBackPressed()
        }
        builder.setNegativeButton("No") { dialog, _ ->
            dialog.dismiss()
        }
        val alert = builder.create()
        alert.show()
    }
}