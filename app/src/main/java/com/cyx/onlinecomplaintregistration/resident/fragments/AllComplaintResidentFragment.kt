package com.cyx.onlinecomplaintregistration.resident.fragments

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.cyx.onlinecomplaintregistration.R
import com.cyx.onlinecomplaintregistration.classes.Complaint
import com.cyx.onlinecomplaintregistration.classes.Constants
import com.cyx.onlinecomplaintregistration.management.adapters.ManagementComplaintRecyclerAdapter
import com.cyx.onlinecomplaintregistration.resident.adapters.ComplaintRecyclerAdapter
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.fragment_all_complaint_resident.*
import net.cachapa.expandablelayout.ExpandableLayout


class AllComplaintResidentFragment : Fragment() {

//    private val LIST_STATE = "listState"
//    private var mListState:Parcelable? = null

    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var database: FirebaseDatabase
    private lateinit var reference: DatabaseReference
    private var complaintList = mutableListOf<Complaint>()
    private var complaintSortedPriorityList = mutableListOf<Complaint>()
    private var complaintSortedPostalCodeList = mutableListOf<Complaint>()
    private var complaintSortedCategoryList = mutableListOf<Complaint>()
    private lateinit var complaintAdapter: ComplaintRecyclerAdapter
    private lateinit var managementComplaintAdapter: ManagementComplaintRecyclerAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var expandedGoToTop: ExpandableLayout
    private lateinit var expandedTopCard: ExpandableLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var imageRefresh: ImageView
    private lateinit var textNoComplaint: TextView
    private lateinit var cardNewChanges: CardView
    private var isNewChanges = false
    private lateinit var imageSortBy: ImageView
    private lateinit var imageFilter: ImageView
    private var checkBoxCategoryList = mutableListOf<CheckBox>()
    private var zipCodeKampar = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_all_complaint_resident, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Constants.sharedPref =
            view.context.getSharedPreferences("sharedPref", Context.MODE_PRIVATE)
        database = Constants.database
        swipeToRefresh.setColorSchemeColors(Color.BLUE)
        recyclerView = view.findViewById(R.id.rv_recyclerView)
        expandedGoToTop = view.findViewById(R.id.expanded_go_to_top)
        expandedTopCard = view.findViewById(R.id.expanded_top_card)
        progressBar = view.findViewById(R.id.progress_bar)
        imageRefresh = view.findViewById(R.id.image_refresh)
        textNoComplaint = view.findViewById(R.id.text_no_complaint)
        cardNewChanges = view.findViewById(R.id.card_new_changes)
        imageSortBy = view.findViewById(R.id.image_sort_by)
        imageFilter = view.findViewById(R.id.image_filter)

        zipCodeKampar.apply {
            clear()
            add("31700")
            add("31900")
            add("31910")
            add("31950")
            add("35350")
        }

        refreshApp(view)
        loadRecyclerView(view)
        loadNewChanges()
//        mListState = rv_recyclerView.layoutManager!!.onSaveInstanceState()
        expandedGoToTop.setOnClickListener {
            expandedGoToTop.collapse()
            expandedTopCard.expand()
            recyclerView.smoothSnapToPosition(0)
        }
        imageRefresh.setOnClickListener {
            progressBar.visibility = View.VISIBLE
            cardNewChanges.visibility = View.GONE
            recyclerView.removeAllViewsInLayout()
            loadRecyclerView(it)
        }
        cardNewChanges.setOnClickListener {
            cardNewChanges.visibility = View.GONE
            progressBar.visibility = View.VISIBLE
            recyclerView.removeAllViewsInLayout()
            isNewChanges = false
            loadRecyclerView(it)
        }
        imageFilter.setOnClickListener {
            Dialog(it.context).apply {
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                setCancelable(true)
                setContentView(R.layout.layout_filter)
                val rootContainer = findViewById<LinearLayout>(R.id.root_container)
                val layoutParams = WindowManager.LayoutParams()
                layoutParams.copyFrom(window?.attributes)
                layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
                layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
                window?.attributes = layoutParams
                window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                window?.setWindowAnimations(R.style.DialogAnimation)
                loadCategoryCheckBox(this, rootContainer)
                loadDialogFilter(this, view)
                show()
            }
        }
        imageSortBy.setOnClickListener {
            Dialog(it.context).apply {
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                setCancelable(true)
                setContentView(R.layout.layout_sort_by)
                val layoutParams = WindowManager.LayoutParams()
                layoutParams.copyFrom(window?.attributes)
                layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
                layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
                window?.attributes = layoutParams
                window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                window?.setWindowAnimations(R.style.DialogAnimation)
                loadDialogSortBy(this, view)
                show()
            }
        }
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 30) {
                    //Scrolling down
                    expandedGoToTop.expand()
                    expandedTopCard.collapse()
                } else if (dy < -50) {
                    //Scrolling up
                    expandedGoToTop.collapse()
                    expandedTopCard.expand()
                }
            }
        })
    }

    private fun loadDialogFilter(dialog: Dialog, view: View) {
        val buttonCancel: Button = dialog.findViewById(R.id.button_cancel)
        val buttonApply: Button = dialog.findViewById(R.id.button_apply)
        buttonApply.setOnClickListener {
            applyFilter(dialog, view)
            dialog.dismiss()
        }
        buttonCancel.setOnClickListener {
            dialog.dismiss()
        }
    }

    private fun applyFilter(dialog: Dialog, view: View) {
        val checkBoxNeutral: CheckBox = dialog.findViewById(R.id.check_box_neutral)
        val checkBoxImportant: CheckBox = dialog.findViewById(R.id.check_box_important)
        val checkBoxUrgent: CheckBox = dialog.findViewById(R.id.check_box_urgent)
        val checkBoxKampar: CheckBox = dialog.findViewById(R.id.check_box_kampar)

        reference = database.getReference("Complaint")
        reference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                complaintList.clear()
                complaintSortedPriorityList.clear()
                complaintSortedPostalCodeList.clear()
                complaintSortedCategoryList.clear()

                if (snapshot.exists()) {
                    for (data in snapshot.children) {
                        for (i in data.children) {
                            if (Constants.userRole == "Resident"){
                                if (i.child("complaint_status").value.toString() == "Pending") {
                                    val complaint = i.getValue(Complaint::class.java)
                                    complaintList.add(complaint!!)
                                }
                            } else if (Constants.userRole == "Management"){
                                if (i.child("complaint_status").value.toString() == "Pending" && i.child("complaint_managed_by").value.toString() == "") {
                                    val complaint = i.getValue(Complaint::class.java)
                                    complaintList.add(complaint!!)
                                }
                            }
                        }
                    }
                    try {
                        if (complaintList.size > 0) {
                            if (!checkBoxNeutral.isChecked) {
                                for (i in complaintList.indices) {
                                    if (checkBoxImportant.isChecked && checkBoxUrgent.isChecked) {
                                        if (complaintList[i].complaint_priority == 1 || complaintList[i].complaint_priority == 2) {
                                            complaintSortedPriorityList.add(complaintList[i])
                                        }
                                    } else if (checkBoxImportant.isChecked) {
                                        if (complaintList[i].complaint_priority == 1) {
                                            complaintSortedPriorityList.add(complaintList[i])
                                        }
                                    } else if (checkBoxUrgent.isChecked) {
                                        if (complaintList[i].complaint_priority == 2) {
                                            complaintSortedPriorityList.add(complaintList[i])
                                        }
                                    }else{
                                        complaintSortedPriorityList.add(complaintList[i])
                                    }
                                }
                            }
                            if (!checkBoxImportant.isChecked) {
                                for (i in complaintList.indices) {
                                    if (checkBoxNeutral.isChecked && checkBoxUrgent.isChecked) {
                                        if (complaintList[i].complaint_priority == 0 || complaintList[i].complaint_priority == 2) {
                                            complaintSortedPriorityList.add(complaintList[i])
                                        }
                                    } else if (checkBoxNeutral.isChecked) {
                                        if (complaintList[i].complaint_priority == 0) {
                                            complaintSortedPriorityList.add(complaintList[i])
                                        }
                                    } else if (checkBoxUrgent.isChecked) {
                                        if (complaintList[i].complaint_priority == 2) {
                                            complaintSortedPriorityList.add(complaintList[i])
                                        }
                                    }else{
                                        complaintSortedPriorityList.add(complaintList[i])
                                    }
                                }
                            }
                            if (!checkBoxUrgent.isChecked) {
                                for (i in complaintList.indices) {
                                    if (checkBoxNeutral.isChecked && checkBoxImportant.isChecked) {
                                        if (complaintList[i].complaint_priority == 0 || complaintList[i].complaint_priority == 1) {
                                            complaintSortedPriorityList.add(complaintList[i])
                                        }
                                    } else if (checkBoxNeutral.isChecked) {
                                        if (complaintList[i].complaint_priority == 0) {
                                            complaintSortedPriorityList.add(complaintList[i])
                                        }
                                    } else if (checkBoxImportant.isChecked) {
                                        if (complaintList[i].complaint_priority == 1) {
                                            complaintSortedPriorityList.add(complaintList[i])
                                        }
                                    }else{
                                        complaintSortedPriorityList.add(complaintList[i])
                                    }
                                }
                            }
                            if (checkBoxKampar.isChecked) {
                                for (zipCode in zipCodeKampar) {
                                    if (complaintSortedPriorityList.size > 0) {
                                        for (i in complaintSortedPriorityList.indices) {
                                            if (complaintSortedPriorityList[i].complaint_postal_code == zipCode) {
                                                Log.d("test zip", complaintSortedPriorityList[i].complaint_postal_code)
                                                complaintSortedPostalCodeList.add(
                                                    complaintSortedPriorityList[i]
                                                )
                                            }
                                        }
                                    }
//                                    else {
//                                        for (i in complaintList.indices) {
//                                            if (complaintList[i].complaint_postal_code == zipCode) {
//                                                complaintSortedPostalCodeList.add(complaintList[i])
//                                            }
//                                        }
//                                    }
                                }
                            } else {
                                if (complaintSortedPriorityList.size > 0) {
                                    for (i in complaintSortedPriorityList.indices) {
                                        complaintSortedPostalCodeList.add(
                                            complaintSortedPriorityList[i]
                                        )
                                    }
                                }
//                                else {
//                                    for (i in complaintList.indices) {
//                                        complaintSortedPostalCodeList.add(complaintList[i])
//                                    }
//                                }
                            }
                            val isCheckedCategory = mutableListOf<CheckBox>()
                            isCheckedCategory.clear()
                            for (category in checkBoxCategoryList) {
                                if (category.isChecked) {
                                    isCheckedCategory.add(category)
                                }
                            }
                            if (isCheckedCategory.size > 0){
                                for (category in isCheckedCategory){
                                    if (complaintSortedPostalCodeList.size > 0) {
                                        for (i in complaintSortedPostalCodeList.indices) {
                                            if (complaintSortedPostalCodeList[i].complaint_category == category.text.toString()) {
                                                complaintSortedCategoryList.add(
                                                    complaintSortedPostalCodeList[i]
                                                )
                                            }
                                        }
                                    } else {
                                        if (complaintSortedPriorityList.size > 0) {
                                            for (i in complaintSortedPriorityList.indices) {
                                                if (complaintSortedPriorityList[i].complaint_category == category.text.toString()) {
                                                    complaintSortedCategoryList.add(
                                                        complaintSortedPriorityList[i]
                                                    )
                                                }
                                            }
                                        }
//                                        else {
//                                            for (i in complaintList.indices) {
//                                                if (complaintList[i].complaint_category == category.text.toString()) {
//                                                    complaintSortedCategoryList.add(complaintList[i])
//                                                }
//                                            }
//                                        }
                                    }
                                }
                            }else{
                                if (complaintSortedPostalCodeList.size > 0){
                                    for (i in complaintSortedPostalCodeList.indices){
                                        complaintSortedCategoryList.add(complaintSortedPostalCodeList[i])
                                    }
                                }
//                                else{
//                                    if (complaintSortedPriorityList.size > 0){
//                                        for (i in complaintSortedPriorityList.indices){
//                                            complaintSortedCategoryList.add(complaintSortedPriorityList[i])
//                                        }
//                                    }else{
//                                        for (i in complaintList.indices){
//                                            complaintSortedCategoryList.add(complaintList[i])
//                                        }
//                                    }
//                                }
                            }

                            val complaintFinalList = HashSet<Complaint>(complaintSortedCategoryList)
                            complaintSortedCategoryList.clear()
                            for (i in complaintFinalList) {
                                complaintSortedCategoryList.add(i)
                            }

                            progressBar.visibility = View.GONE
                            linearLayoutManager = LinearLayoutManager(view.context)
//                        linearLayoutManager.reverseLayout = true
//                        linearLayoutManager.stackFromEnd = true
                            recyclerView.layoutManager = linearLayoutManager
                            complaintSortedCategoryList.sortByDescending { it.complaint_date_time }
                            if (Constants.userRole == "Resident"){
                                complaintAdapter = ComplaintRecyclerAdapter(complaintSortedCategoryList)
                                complaintAdapter.notifyDataSetChanged()
                                recyclerView.adapter = complaintAdapter
                            }else if (Constants.userRole == "Management"){
                                managementComplaintAdapter = ManagementComplaintRecyclerAdapter(complaintSortedCategoryList)
                                managementComplaintAdapter.notifyDataSetChanged()
                                recyclerView.adapter = managementComplaintAdapter
                            }

                            if (complaintSortedCategoryList.size == 0){
                                progressBar.visibility = View.GONE
                                textNoComplaint.visibility = View.VISIBLE
                            }

                        } else {
                            progressBar.visibility = View.GONE
                            textNoComplaint.visibility = View.VISIBLE
                        }

                    } catch (e: IllegalStateException) {
                        Log.e("Nono", e.message.toString())
                    }
                } else {
                    progressBar.visibility = View.GONE
                    textNoComplaint.visibility = View.VISIBLE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

    private fun loadCategoryCheckBox(dialog: Dialog, rootContainer: LinearLayout) {
        reference = database.getReference("Category")
        reference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var i = 0
                checkBoxCategoryList.clear()
                if (snapshot.exists()) {
                    for (data in snapshot.children) {
                        val checkBoxCategory = CheckBox(dialog.context).apply {
                            text = data.child("category_name").value.toString()
                            id = i++
                            layoutParams = LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            )
                            setPadding(0, 40, 0, 40)
                        }
                        checkBoxCategoryList.add(checkBoxCategory)
                        rootContainer.addView(checkBoxCategory)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

    private fun loadDialogSortBy(dialog: Dialog, view: View) {
        val buttonApply = dialog.findViewById<Button>(R.id.button_apply)
        val buttonCancel = dialog.findViewById<Button>(R.id.button_cancel)
        val radioGroup = dialog.findViewById<RadioGroup>(R.id.radioGroup)
        val radioGroup2 = dialog.findViewById<RadioGroup>(R.id.radioGroup2)

        if (radioGroup.checkedRadioButtonId != -1) {
            buttonApply.setOnClickListener {
                val selectedRadio = radioGroup.checkedRadioButtonId
                val selectedRadio2 = radioGroup2.checkedRadioButtonId
                val radioButton = dialog.findViewById<RadioButton>(selectedRadio)
                val radioButton2 = dialog.findViewById<RadioButton>(selectedRadio2)
                val sortBy = radioButton.text.toString()
                val ordering = radioButton2.text.toString()

                applySortBy(sortBy, ordering, view)
                dialog.dismiss()
            }
        }
        buttonCancel.setOnClickListener {
            dialog.dismiss()
        }
    }

    private fun applySortBy(sortBy: String, ordering: String, view: View) {
        reference = database.getReference("Complaint")
        reference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                complaintList.clear()
                if (snapshot.exists()) {
                    for (data in snapshot.children) {
                        for (i in data.children) {
                            if (Constants.userRole == "Resident"){
                                if (i.child("complaint_status").value.toString() == "Pending") {
                                    val complaint = i.getValue(Complaint::class.java)
                                    complaintList.add(complaint!!)
                                }
                            } else if (Constants.userRole == "Management"){
                                if (i.child("complaint_status").value.toString() == "Pending" && i.child("complaint_managed_by").value.toString() == "") {
                                    val complaint = i.getValue(Complaint::class.java)
                                    complaintList.add(complaint!!)
                                }
                            }
                        }// for i
                    }// for data
                    try {
                        if (complaintList.size > 0) {
                            progressBar.visibility = View.GONE
                            linearLayoutManager = LinearLayoutManager(view.context)
//                        linearLayoutManager.reverseLayout = true
//                        linearLayoutManager.stackFromEnd = true
                            recyclerView.layoutManager = linearLayoutManager
                            if (ordering == "Ascending") {
                                if (sortBy == "Priority") complaintList.sortBy { it.complaint_priority }
                                if (sortBy == "Category") complaintList.sortBy { it.complaint_category }
                                if (sortBy == "Post Date Time") complaintList.sortBy { it.complaint_date_time }
                            } else {
                                if (sortBy == "Priority") complaintList.sortByDescending { it.complaint_priority }
                                if (sortBy == "Category") complaintList.sortByDescending { it.complaint_category }
                                if (sortBy == "Post Date Time") complaintList.sortByDescending { it.complaint_date_time }
                            }
                            if (Constants.userRole == "Resident"){
                                complaintAdapter = ComplaintRecyclerAdapter(complaintList)
                                complaintAdapter.notifyDataSetChanged()
                                recyclerView.adapter = complaintAdapter
                            }else if (Constants.userRole == "Management"){
                                managementComplaintAdapter = ManagementComplaintRecyclerAdapter(complaintList)
                                managementComplaintAdapter.notifyDataSetChanged()
                                recyclerView.adapter = managementComplaintAdapter
                            }
                            Toast.makeText(view.context, "All changes applied.", Toast.LENGTH_SHORT)
                                .show()
                        } else {
                            progressBar.visibility = View.GONE
                            textNoComplaint.visibility = View.VISIBLE
                        }

                    } catch (e: IllegalStateException) {
                        Log.e("Nono", e.message.toString())
                    }
                } else {
                    progressBar.visibility = View.GONE
                    textNoComplaint.visibility = View.VISIBLE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

    private fun loadNewChanges() {
        reference = database.getReference("Complaint")
        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (isNewChanges) {
                    cardNewChanges.visibility = View.VISIBLE
                } else {
                    cardNewChanges.visibility = View.GONE
                }
                isNewChanges = true
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

    fun RecyclerView.smoothSnapToPosition(
        position: Int,
        snapMode: Int = LinearSmoothScroller.SNAP_TO_START
    ) {
        val smoothScroller = object : LinearSmoothScroller(this.context) {
            override fun getVerticalSnapPreference(): Int = snapMode
            override fun getHorizontalSnapPreference(): Int = snapMode
        }
        smoothScroller.targetPosition = position
        layoutManager?.startSmoothScroll(smoothScroller)
    }

    private fun loadRecyclerView(view: View) {
        reference = database.getReference("Complaint")
        reference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                complaintList.clear()
                if (snapshot.exists()) {
                    for (data in snapshot.children) {
                        for (i in data.children) {
                            if (Constants.userRole == "Resident"){
                                if (i.child("complaint_status").value.toString() == "Pending") {
                                    val complaint = i.getValue(Complaint::class.java)
                                    complaintList.add(complaint!!)
                                }
                            } else if (Constants.userRole == "Management"){
                                if (i.child("complaint_status").value.toString() == "Pending" && i.child("complaint_managed_by").value.toString() == "") {
                                    val complaint = i.getValue(Complaint::class.java)
                                    complaintList.add(complaint!!)
                                }
                            }
                        }// for i
                    }// for data
                    try {
                        if (complaintList.size > 0) {
                            progressBar.visibility = View.GONE
                            textNoComplaint.visibility = View.GONE
                            linearLayoutManager = LinearLayoutManager(view.context)
                            recyclerView.layoutManager = linearLayoutManager
                            complaintList.sortByDescending { it.complaint_date_time }
                            if (Constants.userRole == "Resident"){
                                complaintAdapter = ComplaintRecyclerAdapter(complaintList)
                                complaintAdapter.notifyDataSetChanged()
                                recyclerView.adapter = complaintAdapter
                            }else if (Constants.userRole == "Management"){
                                managementComplaintAdapter = ManagementComplaintRecyclerAdapter(complaintList)
                                managementComplaintAdapter.notifyDataSetChanged()
                                recyclerView.adapter = managementComplaintAdapter
                            }
                        } else {
                            progressBar.visibility = View.GONE
                            textNoComplaint.visibility = View.VISIBLE
                        }
                    } catch (e: IllegalStateException) {
                        Log.e("Nono", e.message.toString())
                    }
                } else {
                    progressBar.visibility = View.GONE
                    textNoComplaint.visibility = View.VISIBLE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

    private fun refreshApp(view: View) {
        swipeToRefresh.setOnRefreshListener {
            recyclerView.removeAllViewsInLayout()
            cardNewChanges.visibility = View.GONE
            progressBar.visibility = View.VISIBLE
            loadRecyclerView(view)
            swipeToRefresh.isRefreshing = false
        }
    }

//    override fun onResume() {
//        super.onResume()
//        if (mListState != null){
//            rv_recyclerView.layoutManager!!.onRestoreInstanceState(mListState)
//        }
//    }
//
//    override fun onViewStateRestored(savedInstanceState: Bundle?) {
//        super.onViewStateRestored(savedInstanceState)
//        if (savedInstanceState != null){
//            mListState = savedInstanceState.getParcelable(LIST_STATE)!!
//        }
//    }
//    override fun onSaveInstanceState(outState: Bundle) {
//        super.onSaveInstanceState(outState)
//        val rv_recyclerView  = requireView().findViewById<RecyclerView>(R.id.rv_recyclerView)
//        rv_recyclerView.layoutManager = LinearLayoutManager(requireContext())
//        mListState = rv_recyclerView.layoutManager!!.onSaveInstanceState()!!
//        outState.putParcelable(LIST_STATE, mListState)
//    }

}