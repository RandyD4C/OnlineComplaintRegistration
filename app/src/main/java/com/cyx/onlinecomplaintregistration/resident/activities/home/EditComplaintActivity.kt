package com.cyx.onlinecomplaintregistration.resident.activities.home

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.cyx.onlinecomplaintregistration.R
import com.cyx.onlinecomplaintregistration.classes.Constants
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_edit_complaint.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class EditComplaintActivity : AppCompatActivity() {

    private lateinit var spinnerComplaintPriority: Spinner
    private lateinit var spinnerComplaintCategory: Spinner
    private lateinit var editTextDescription: EditText
    private lateinit var imageComplaint: ImageView
    private lateinit var textLocation: TextView
    private lateinit var buttonReset: Button
    private lateinit var buttonSetCurrentLocation: Button
    private lateinit var progressBar: ProgressBar
    private var complaintUID: String = ""
    private lateinit var database: FirebaseDatabase
    private lateinit var reference: DatabaseReference
    private lateinit var progressDialog: ProgressDialog
    private var complaintCategoryAdapter = mutableListOf<String>()
    private var latitude = 0.0
    private var longitude = 0.0
    private var postalCode = ""
    private lateinit var geocoder: Geocoder
    private lateinit var lastLocation: Location
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var addresses = mutableListOf<Address>().toMutableList()
    private var address: String? = null
    private var imageCapture: Uri? = null
    private lateinit var photoFile: File
    private val cameraRequest = 1
    private val REQUEST_PERM_WRITE_STORAGE = 1

    companion object {
        private const val LOCATION_REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_complaint)
        this.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Edit Complaint"

        spinnerComplaintPriority = findViewById(R.id.spinner_complaint_priority)
        spinnerComplaintCategory = findViewById(R.id.spinner_complaint_category)
        editTextDescription = findViewById(R.id.edit_text_complaint_desc)
        imageComplaint = findViewById(R.id.image_complaint)
        textLocation = findViewById(R.id.text_location)
        buttonReset = findViewById(R.id.button_reset)
        buttonSetCurrentLocation = findViewById(R.id.button_set_current_location)
        progressBar = findViewById(R.id.progress_bar)

        complaintUID = intent.getStringExtra("complaint_uid")!!

        Constants.sharedPref =
            getSharedPreferences("sharedPref", Context.MODE_PRIVATE)
        database = Constants.database
        loadComplaintData()

        buttonReset.setOnClickListener {
            loadComplaintData()
        }
        buttonSetCurrentLocation.setOnClickListener {
            getUserLocation()
        }

        imageComplaint.setOnClickListener {
            Toast.makeText(this, "Long press to change photo", Toast.LENGTH_SHORT).show()
        }

        imageComplaint.setOnLongClickListener {
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

    private fun getUserLocation() {
        progressBar.visibility = View.VISIBLE
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
                    geocoder.getFromLocation(latitude, longitude, 1)!!
                address = addresses[0].getAddressLine(0)
                postalCode = addresses[0].postalCode

                textLocation.text = address
                progressBar.visibility = View.GONE
            }
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

    private fun loadComplaintData() {
        val uid = Constants.userUID
        reference = database.getReference("Complaint/$uid/$complaintUID")
        reference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    if (snapshot.child("complaint_priority").value == 0) spinnerComplaintPriority.setSelection(
                        0
                    )
                    if (snapshot.child("complaint_priority").value == 1) spinnerComplaintPriority.setSelection(
                        1
                    )
                    if (snapshot.child("complaint_priority").value == 2) spinnerComplaintPriority.setSelection(
                        2
                    )

                    editTextDescription.setText(snapshot.child("complaint_description").value.toString())
                    try {
                        Glide.with(imageComplaint.context)
                            .load(snapshot.child("complaint_photo").value.toString())
                            .apply(Constants.requestOptions)
                            .placeholder(R.drawable.loadingimg)
                            .error(R.drawable.errorimg)
                            .into(imageComplaint)
                    } catch (e: Exception) {
                        Log.e("Haha", e.message.toString())
                    }

                    loadSpinnerCategory(snapshot.child("complaint_category").value.toString())
                    loadLocation(
                        snapshot.child("complaint_latitude").value.toString().toDouble(),
                        snapshot.child("complaint_longitude").value.toString().toDouble()
                    )
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

    private fun loadLocation(lat: Double, long: Double) {
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
        geocoder = Geocoder(this, Locale.getDefault())
        latitude = lat
        longitude = long
        addresses =
            geocoder.getFromLocation(latitude, longitude, 1)!!
        address = addresses[0].getAddressLine(0)
        postalCode = addresses[0].postalCode
        textLocation.text = address
    }

    private fun loadSpinnerCategory(complaintCategory: String) {
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
                    this@EditComplaintActivity,
                    android.R.layout.simple_list_item_1, complaintCategoryAdapter
                )
                adapter.setDropDownViewResource(android.R.layout.simple_list_item_1)
                spinnerComplaintCategory.adapter = adapter
                spinnerComplaintCategory.setSelection(
                    getIndex(
                        spinnerComplaintCategory,
                        complaintCategory
                    )
                )
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

    private fun getIndex(spinner: Spinner, myString: String): Int {
        for (i in 0 until spinner.count) {
            if (spinner.getItemAtPosition(i).toString().equals(myString, ignoreCase = true)) {
                return i
            }
        }
        return 0
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.register_complaint, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_confirm -> {
                progressDialog = ProgressDialog(this)
                progressDialog.setCancelable(false)
                progressDialog.setMessage("Updating Complaint...")
                progressDialog.show()
                val uid = FirebaseAuth.getInstance().uid ?: ""
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Update Complaint")
                builder.setMessage("Update This Complaint?")
                builder.setCancelable(true)
                builder.setPositiveButton("Yes") { dialog, _ ->
                    dialog.dismiss()
                    imageCapture?.let {
                        saveToFirebaseStorage()
                    } ?: registerComplaint("", uid)
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
            imageComplaint.setImageURI(Uri.fromFile(photoFile.absoluteFile))
            imageCapture = Uri.fromFile(photoFile.absoluteFile)
        } else {
            super.onActivityResult(requestCode, resultCode, data)
            Toast.makeText(this, "Something went wrong", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveToFirebaseStorage() {
        val fileName = UUID.randomUUID().toString()
        val userUID = Constants.userUID ?:""
        val storageReference =
            FirebaseStorage.getInstance().getReference("Complaint/$userUID/$fileName")
        storageReference.putFile(imageCapture!!).addOnSuccessListener {
            storageReference.downloadUrl.addOnSuccessListener {
                registerComplaint(it.toString(), userUID)
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to upload image.", Toast.LENGTH_SHORT)
                .show()
            if (progressDialog.isShowing) progressDialog.dismiss()
        }.addOnProgressListener {
            val progress: Long = (100 * it.bytesTransferred) / it.totalByteCount
            progressDialog.setMessage("Updating Complaint: $progress %")
        }
    }

    private fun registerComplaint(imageUri: String, userUID: String) {
        reference = database.getReference("Complaint")
        val updateComplaint = if (imageUri != "") mapOf<String, Any>(
            "complaint_category" to spinnerComplaintCategory.selectedItem,
            "complaint_priority" to spinnerComplaintPriority.selectedItemPosition,
            "complaint_description" to editTextDescription.text.toString(),
            "complaint_photo" to imageUri,
            "complaint_latitude" to latitude,
            "complaint_longitude" to longitude,
            "complaint_postal_code" to postalCode
        ) else mapOf<String, Any>(
            "complaint_category" to spinnerComplaintCategory.selectedItem,
            "complaint_priority" to spinnerComplaintPriority.selectedItemPosition,
            "complaint_description" to editTextDescription.text.toString(),
            "complaint_location" to textLocation.text,
            "complaint_latitude" to latitude,
            "complaint_longitude" to longitude,
            "complaint_postal_code" to postalCode
        )
        reference.child("$userUID/$complaintUID").updateChildren(updateComplaint)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    if (progressDialog.isShowing) progressDialog.dismiss()
                    val builder = AlertDialog.Builder(this)
                    builder.setTitle("Update Successfully")
                    builder.setMessage("Your complaint has been UPDATED!!")
                    builder.setCancelable(false)
                    builder.setPositiveButton("OK") { dialog, _ ->
                        dialog.dismiss()
                        finish()
                    }
                    val alert = builder.create()
                    alert.show()
                } else {
                    if (progressDialog.isShowing) progressDialog.dismiss()
                    val builder = AlertDialog.Builder(this)
                    builder.setTitle("Error")
                    builder.setMessage("Failed to update complaint.")
                    builder.setCancelable(true)
                    builder.setPositiveButton("OK") { dialog, _ ->
                        dialog.dismiss()
                    }
                    val alert = builder.create()
                    alert.show()
                }
            }
    }
}