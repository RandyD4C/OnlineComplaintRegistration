package com.cyx.onlinecomplaintregistration.resident.fragments

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.cyx.onlinecomplaintregistration.R
import com.cyx.onlinecomplaintregistration.classes.Constants
import com.cyx.onlinecomplaintregistration.resident.activities.home.RegisteringComplaintActivity
import com.google.firebase.database.FirebaseDatabase
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class PhotoFragment : Fragment() {

    private val cameraRequest = 1
    lateinit var imageView: ImageView
    private lateinit var photoFile: File
    private lateinit var database: FirebaseDatabase
    private val REQUEST_PERM_WRITE_STORAGE = 1

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_photo, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val buttonTakePhoto = view.findViewById<Button>(R.id.button_take_photo)
        database = Constants.database

        buttonTakePhoto.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.CAMERA
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        requireActivity(),
                        arrayOf(Manifest.permission.CAMERA),
                        REQUEST_PERM_WRITE_STORAGE
                    )
                }else {
                    val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    photoFile = getOutputDirectory()
                    val fileProvider = FileProvider.getUriForFile(
                        requireContext(),
                        "com.cyx.onlinecomplaintregistration.fileprovider",
                        photoFile
                    )
                    cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileProvider)
                    cameraIntent.resolveActivity(requireActivity().packageManager)?.let {
                        startActivityForResult(cameraIntent, cameraRequest)
                    } ?: Toast.makeText(requireContext(), "Unable to open camera", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == cameraRequest && resultCode == Activity.RESULT_OK) {
            val intent = Intent(requireContext(), RegisteringComplaintActivity::class.java)
            intent.putExtra("imageUri", Uri.fromFile(photoFile.absoluteFile)!!)
            requireContext().startActivity(intent)
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }


    private fun getOutputDirectory(): File {
        val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val fileName = SimpleDateFormat(
            Constants.FILE_NAME_FORMAT,
            Locale.getDefault()
        ).format(System.currentTimeMillis())
        return File.createTempFile(fileName, ".jpg", storageDir)
    }
}
