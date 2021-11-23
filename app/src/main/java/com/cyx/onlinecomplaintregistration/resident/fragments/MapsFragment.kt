package com.cyx.onlinecomplaintregistration.resident.fragments

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.cyx.onlinecomplaintregistration.R
import com.cyx.onlinecomplaintregistration.classes.Complaint
import com.cyx.onlinecomplaintregistration.classes.Constants
import com.cyx.onlinecomplaintregistration.classes.MapTheme
import com.cyx.onlinecomplaintregistration.resident.activities.home.ViewComplaintsActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.fragment_maps.*
import net.cachapa.expandablelayout.ExpandableLayout
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*


class MapsFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private lateinit var googleMap: GoogleMap
    private lateinit var lastLocation: Location
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var database: FirebaseDatabase
    private lateinit var reference: DatabaseReference
    private var latitude = 0.0
    private var longitude = 0.0
    private lateinit var cardMapTheme: CardView
    private lateinit var textMapTheme: TextView
    private var mapTheme = ""
    private lateinit var progressBar: ProgressBar
    private var complaintList = mutableListOf<Complaint>()
    private lateinit var expandedLayout: ExpandableLayout
    private lateinit var textPostDate: TextView
    private lateinit var textUserName: TextView
    private lateinit var textLocation: TextView
    private lateinit var checkBoxKampar: CheckBox
    private lateinit var geocoder: Geocoder
    private var addresses = mutableListOf<Address>()
    private var address: String? = null
    private lateinit var textImportant: TextView
    private lateinit var textUrgent: TextView
    private var userUID: String? = null
    private var complaintUID: String? = null
    private var ownUserRole: String = ""
    private var zipCodeKampar = mutableListOf<String>()

    companion object {
        private const val LOCATION_REQUEST_CODE = 1
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        map_view.onCreate(savedInstanceState)
        map_view.onResume()
        map_view.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_maps, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cardMapTheme = view.findViewById(R.id.card_map_theme)
        textMapTheme = view.findViewById(R.id.text_map_theme)
        progressBar = view.findViewById(R.id.progress_bar)
        expandedLayout = view.findViewById(R.id.expanded_layout)
        textPostDate = view.findViewById(R.id.text_post_date)
        textUserName = view.findViewById(R.id.text_user_name)
        textLocation = view.findViewById(R.id.text_complaint_location)
        textImportant = view.findViewById(R.id.text_important)
        textUrgent = view.findViewById(R.id.text_urgent)
        checkBoxKampar = view.findViewById(R.id.check_box_kampar)
        database = Constants.database

        zipCodeKampar.apply {
            clear()
            add("31700")
            add("31900")
            add("31910")
            add("31950")
            add("35350")
        }

        Constants.sharedPref =
            view.context.getSharedPreferences("sharedPref", Context.MODE_PRIVATE)

        textUserName.setOnClickListener {
            progressBar.visibility = View.VISIBLE
            userUID?.let {
                loadUserData(it)
            }
        }

        checkBoxKampar.setOnCheckedChangeListener { _, isChecked ->
            googleMap.clear()
            loadDataSet(isChecked)
        }

        cardMapTheme.setOnClickListener {
            Dialog(it.context).apply {
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                setCancelable(true)
                setContentView(R.layout.layout_map_theme)
                val layoutParams = WindowManager.LayoutParams()
                layoutParams.copyFrom(window?.attributes)
                layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
                layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
                window?.attributes = layoutParams
                window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                window?.setWindowAnimations(R.style.MapThemeAnimation)

                val radioGroupTheme: RadioGroup = findViewById(R.id.radioGroupTheme)
                val buttonApply: Button = findViewById(R.id.button_apply)
                val buttonCancel: Button = findViewById(R.id.button_cancel)
                val buttonBack: ImageView = findViewById(R.id.image_back)

                loadRadioButtonChecked(radioGroupTheme)

                buttonApply.setOnClickListener {
                    changeMapTheme(radioGroupTheme, this)
                    dismiss()
                }
                buttonCancel.setOnClickListener {
                    dismiss()
                }
                buttonBack.setOnClickListener {
                    dismiss()
                }
                show()
            }
        }

        loadOwnUserRole()
    }

    private fun loadOwnUserRole() {
        val uid = Constants.userUID
        reference = database.getReference("User/$uid")
        reference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    ownUserRole = snapshot.child("user_role").value.toString()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

    private fun loadUserData(userUID: String) {
        reference = database.getReference("User/$userUID")
        reference.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()){
                    Dialog(requireContext()).apply {
                        requestWindowFeature(Window.FEATURE_NO_TITLE)
                        setCancelable(true)
                        setContentView(R.layout.layout_view_profile)
                        val layoutParams = WindowManager.LayoutParams()
                        layoutParams.copyFrom(window?.attributes)
                        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
                        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
                        window?.attributes = layoutParams
                        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                        window?.setWindowAnimations(R.style.ViewProfileAnimation)

                        val textUserName = findViewById<TextView>(R.id.text_user_name)
                        val textFullName = findViewById<TextView>(R.id.text_full_name)
                        val textEmail = findViewById<TextView>(R.id.text_email)
                        val textNric = findViewById<TextView>(R.id.text_nric)
                        val textPhoneNum = findViewById<TextView>(R.id.text_phone_num)
                        val imageAvatar = findViewById<ImageView>(R.id.image_avatar)
                        val imageVerifiedUser = findViewById<ImageView>(R.id.image_verified_user)
                        val imageNric = findViewById<ImageView>(R.id.image_nric)
                        val imagePhone = findViewById<ImageView>(R.id.image_phone)

                        textUserName.text = snapshot.child("user_name").value.toString()
                        textFullName.text = snapshot.child("user_full_name").value.toString()
                        textEmail.text = snapshot.child("user_email").value.toString()
                        textNric.text = if (snapshot.child("user_nric").value.toString() != "") snapshot.child("user_nric").value.toString() else "-"
                        textPhoneNum.text = if (snapshot.child("user_phone_num").value.toString() != "") snapshot.child("user_phone_num").value.toString() else "-"

                        Glide.with(imageAvatar.context)
                            .load(snapshot.child("user_avatar").value.toString())
                            .apply(Constants.requestOptions)
                            .placeholder(R.drawable.useravatar)
                            .error(R.drawable.errorimg)
                            .into(imageAvatar)

                        if (snapshot.exists()) {
                            if (snapshot.child("user_nric").value.toString() != "" && snapshot.child("user_phone_num").value.toString() != "") {
                                imageVerifiedUser.visibility = View.VISIBLE
                            } else {
                                imageVerifiedUser.visibility = View.GONE
                            }
                        }

                        if (ownUserRole != "Resident") {
                            textNric.visibility = View.VISIBLE
                            textPhoneNum.visibility = View.VISIBLE
                            imageNric.visibility = View.VISIBLE
                            imagePhone.visibility = View.VISIBLE
                        } else {
                            textNric.visibility = View.GONE
                            textPhoneNum.visibility = View.GONE
                            imageNric.visibility = View.GONE
                            imagePhone.visibility = View.GONE
                        }

                        val buttonBack = findViewById<ImageView>(R.id.image_back)
                        val buttonViewComplaint = findViewById<Button>(R.id.button_view_complaint)
                        buttonBack.setOnClickListener {
                            dismiss()
                        }
                        buttonViewComplaint.setOnClickListener {
                            Intent(requireContext(), ViewComplaintsActivity::class.java).apply {
                                putExtra("complaint_uid", complaintUID)
                                this@MapsFragment.startActivity(this)
                            }
                        }
                        progressBar.visibility = View.GONE
                        show()
                    }
                }else{
                    Toast.makeText(this@MapsFragment.requireContext(), "No User Found.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

    private fun loadRadioButtonChecked(radioGroupTheme: RadioGroup) {
        when (mapTheme) {
            "Standard" -> {
                radioGroupTheme.check(R.id.radio_standard)
            }
            "Silver" -> {
                radioGroupTheme.check(R.id.radio_silver)
            }
            "Retro" -> {
                radioGroupTheme.check(R.id.radio_retro)
            }
            "Night" -> {
                radioGroupTheme.check(R.id.radio_night)
            }
            "Dark" -> {
                radioGroupTheme.check(R.id.radio_dark)
            }
            "Aubergine" -> {
                radioGroupTheme.check(R.id.radio_aubergine)
            }
            else -> {
                radioGroupTheme.check(R.id.radio_standard)
            }
        }
    }

    private fun changeMapTheme(radioGroupTheme: RadioGroup, dialog: Dialog) {
        val checkedRadioButton: RadioButton =
            dialog.findViewById(radioGroupTheme.checkedRadioButtonId)
        val uid = FirebaseAuth.getInstance().uid ?: ""
        val updateTheme = MapTheme(checkedRadioButton.text.toString(), uid)
        reference = database.getReference("MapTheme/$uid")
        reference.setValue(updateTheme).addOnCompleteListener {
            if (it.isSuccessful) {
                when (checkedRadioButton.text.toString()) {
                    "Standard" -> {
                        googleMap.setMapStyle(
                            MapStyleOptions.loadRawResourceStyle(
                                requireView().context, R.raw.standard
                            )
                        )
                    }
                    "Silver" -> {
                        googleMap.setMapStyle(
                            MapStyleOptions.loadRawResourceStyle(
                                requireView().context, R.raw.silver
                            )
                        )
                    }
                    "Retro" -> {
                        googleMap.setMapStyle(
                            MapStyleOptions.loadRawResourceStyle(
                                requireView().context, R.raw.retro
                            )
                        )
                    }
                    "Night" -> {
                        googleMap.setMapStyle(
                            MapStyleOptions.loadRawResourceStyle(
                                requireView().context, R.raw.night
                            )
                        )
                    }
                    "Dark" -> {
                        googleMap.setMapStyle(
                            MapStyleOptions.loadRawResourceStyle(
                                requireView().context, R.raw.dark
                            )
                        )
                    }
                    "Aubergine" -> {
                        googleMap.setMapStyle(
                            MapStyleOptions.loadRawResourceStyle(
                                requireView().context, R.raw.aubergine
                            )
                        )
                    }
                }
                mapTheme = checkedRadioButton.text.toString()
                textMapTheme.text = checkedRadioButton.text.toString()
            }
        }.addOnFailureListener {
            Toast.makeText(dialog.context, "Failed to load theme.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onMapReady(map: GoogleMap) {
        map?.let {
            googleMap = it
        }
        try {
            // Customise the styling of the base map using a JSON object defined
            // in a raw resource file.
            loadMapTheme()
        } catch (e: Resources.NotFoundException) {
            Log.e("TAG", "Can't find style. Error: ", e)
        }
//        googleMap.uiSettings.isCompassEnabled = true
//        googleMap.uiSettings.isZoomControlsEnabled = true
//        googleMap.uiSettings.isMyLocationButtonEnabled = true
        googleMap.setOnMarkerClickListener(this)
        googleMap.setOnMapClickListener {
            expandedLayout.collapse()
        }
        googleMap.uiSettings.setAllGesturesEnabled(true)
        googleMap.uiSettings.isRotateGesturesEnabled = false
        googleMap.uiSettings.isCompassEnabled = false
        googleMap.uiSettings.isTiltGesturesEnabled = false
        googleMap.isBuildingsEnabled = false
        progressBar.visibility = View.GONE
        setUpMap()
        loadDataSet(checkBoxKampar.isChecked)
    }

    private fun loadMapTheme() {
        val uid = Constants.userUID
        reference = database.getReference("MapTheme/$uid")
        reference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    when {
                        snapshot.child("map_theme").value.toString() == "Standard" -> {
                            googleMap.setMapStyle(
                                MapStyleOptions.loadRawResourceStyle(
                                    requireView().context, R.raw.standard
                                )
                            )
                        }
                        snapshot.child("map_theme").value.toString() == "Silver" -> {
                            googleMap.setMapStyle(
                                MapStyleOptions.loadRawResourceStyle(
                                    requireView().context, R.raw.silver
                                )
                            )
                        }
                        snapshot.child("map_theme").value.toString() == "Retro" -> {
                            googleMap.setMapStyle(
                                MapStyleOptions.loadRawResourceStyle(
                                    requireView().context, R.raw.retro
                                )
                            )
                        }
                        snapshot.child("map_theme").value.toString() == "Night" -> {
                            googleMap.setMapStyle(
                                MapStyleOptions.loadRawResourceStyle(
                                    requireView().context, R.raw.night
                                )
                            )
                        }
                        snapshot.child("map_theme").value.toString() == "Dark" -> {
                            googleMap.setMapStyle(
                                MapStyleOptions.loadRawResourceStyle(
                                    requireView().context, R.raw.dark
                                )
                            )
                        }
                        snapshot.child("map_theme").value.toString() == "Aubergine" -> {
                            googleMap.setMapStyle(
                                MapStyleOptions.loadRawResourceStyle(
                                    requireView().context, R.raw.aubergine
                                )
                            )
                        }
                        else -> {
                            googleMap.setMapStyle(
                                MapStyleOptions.loadRawResourceStyle(
                                    requireView().context, R.raw.standard
                                )
                            )
                        }
                    }
                    textMapTheme.text = snapshot.child("map_theme").value.toString()
                    mapTheme = snapshot.child("map_theme").value.toString()
                } else {
                    googleMap.setMapStyle(
                        MapStyleOptions.loadRawResourceStyle(
                            requireView().context, R.raw.standard
                        )
                    )
                    mapTheme = "Standard"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

    private fun loadDataSet(isChecked: Boolean) {
        reference = database.getReference("Complaint")
        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                complaintList.clear()
                if (snapshot.exists()) {
                    for (data in snapshot.children) {
                        for (i in data.children) {
                            if (i.child("complaint_status").value.toString() == "Pending") {
                                if (isChecked){
                                    if (i.child("complaint_postal_code").value.toString() in zipCodeKampar){
                                        complaintList.add(i.getValue(Complaint::class.java)!!)
                                        placeMarkerOnMap(
                                            i.child("complaint_latitude").value.toString().toDouble(),
                                            i.child("complaint_longitude").value.toString().toDouble(),
                                            i.child("complaint_description").value.toString(),
                                            i.child("complaint_priority").value.toString(),
                                            i.child("complaint_category").value.toString()
                                        )
                                    }
                                }else{
                                    complaintList.add(i.getValue(Complaint::class.java)!!)
                                    placeMarkerOnMap(
                                        i.child("complaint_latitude").value.toString().toDouble(),
                                        i.child("complaint_longitude").value.toString().toDouble(),
                                        i.child("complaint_description").value.toString(),
                                        i.child("complaint_priority").value.toString(),
                                        i.child("complaint_category").value.toString()
                                    )
                                }
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

    private fun setUpMap() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_REQUEST_CODE
            )
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_REQUEST_CODE
            )
            return
        }
        googleMap.isMyLocationEnabled = true
        fusedLocationClient.lastLocation.addOnSuccessListener(requireActivity()) { location ->
            location?.let {
                lastLocation = it
                val currentLatLong = LatLng(it.latitude, it.longitude)
//                placeMarkerOnMap(currentLatLong)
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLong, 13f))
            }
        }
    }

    private fun placeMarkerOnMap(
        latitude: Double,
        longitude: Double,
        complaintDesc: String,
        priority: String,
        category: String
    ) {
        val currentLatLong = LatLng(latitude, longitude)
        val markerOptions = MarkerOptions().position(currentLatLong)
        markerOptions.title(category)
        markerOptions.snippet(complaintDesc)
        if (priority == "0") {
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
        } else if (priority == "1") {
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
        } else {
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
        }
        googleMap.addMarker(markerOptions)
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        for (data in complaintList) {
            if (marker.position.latitude == data.complaint_latitude && marker.position.longitude == data.complaint_longitude) {
                loadAddress(data)
                expandedLayout.expand()
                break
            }
        }
        return false
    }

    private fun loadAddress(data: Complaint) {
        userUID = data.complaint_post_by
        complaintUID = data.complaint_uid
        geocoder = Geocoder(requireContext(), Locale.getDefault())
        latitude = data.complaint_latitude
        longitude = data.complaint_longitude
        addresses =
            geocoder.getFromLocation(latitude, longitude, 1)
        address = addresses[0].getAddressLine(0)

        val dateTime = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocalDateTime.parse(data.complaint_date_time)
        } else {
            TODO("VERSION.SDK_INT < O")
        }
        val formatter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
        } else {
            TODO("VERSION.SDK_INT < O")
        }
        val formatted = dateTime.format(formatter)
        textPostDate.text = formatted
        textUserName.text = data.user_name
        textLocation.text = address

        when (data.complaint_priority) {
            1 -> {
                textImportant.visibility = View.VISIBLE
                textUrgent.visibility = View.GONE
            }
            2 -> {
                textUrgent.visibility = View.VISIBLE
                textImportant.visibility = View.GONE
            }
            else -> {
                textImportant.visibility = View.GONE
                textUrgent.visibility = View.GONE
            }
        }
    }
}
