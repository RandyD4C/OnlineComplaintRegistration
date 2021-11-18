package com.cyx.onlinecomplaintregistration.management.fragments

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.cyx.onlinecomplaintregistration.R
import com.cyx.onlinecomplaintregistration.classes.Complaint
import com.cyx.onlinecomplaintregistration.classes.Constants
import com.cyx.onlinecomplaintregistration.resident.activities.home.ComplaintListActivity
import com.cyx.onlinecomplaintregistration.resident.activities.home.ComplaintPostActivity
import com.cyx.onlinecomplaintregistration.resident.activities.user.MyAccountActivity
import com.cyx.onlinecomplaintregistration.resident.activities.user.UserProfileViewMoreActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.fragment_all_complaint_resident.*
import net.cachapa.expandablelayout.ExpandableLayout


class MyWorkshopFragment : Fragment() {

    private lateinit var cardAvatar: MaterialCardView
    private lateinit var imageViewAvatar: ImageView
    private lateinit var textUserName: TextView
    private lateinit var textDepartment: TextView
    private lateinit var cardComplaintHandled: CardView
    private lateinit var cardComplaintSolved: CardView
    private lateinit var textHandledCount: TextView
    private lateinit var textSolvedCount: TextView
    private lateinit var imageVerifiedUser: ImageView
    private lateinit var expandableLayout0: ExpandableLayout
    private lateinit var expandableLayout1: ExpandableLayout
    private lateinit var database: FirebaseDatabase
    private lateinit var reference: DatabaseReference
    private var complaintRegisteredList = mutableListOf<Complaint>()
    private var complaintSolvedList = mutableListOf<Complaint>()
    private var handledCount = 0
    private var solvedCount = 0
    private lateinit var textNoComplaintHandled: TextView
    private lateinit var textNoComplaintSolved: TextView
    private lateinit var cardComplaintHandledExpanded: CardView
    private lateinit var cardComplaintSolvedExpanded: CardView
    private lateinit var buttonHandledList: MaterialButton
    private lateinit var buttonHandledPost: MaterialButton
    private lateinit var buttonSolvedList: MaterialButton
    private lateinit var buttonSolvedPost: MaterialButton
    private lateinit var imageArrowRight: ImageView
    private lateinit var imageArrowRight2: ImageView
    private lateinit var imageMyComplaintInfo: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_my_workshop, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cardAvatar = view.findViewById(R.id.card_avatar)
        imageViewAvatar = view.findViewById(R.id.image_view_avatar)
        textUserName = view.findViewById(R.id.text_user_name)
        textDepartment = view.findViewById(R.id.text_department)
        cardComplaintHandled = view.findViewById(R.id.card_view_complaint_handled)
        cardComplaintSolved = view.findViewById(R.id.card_view_complaint_solved)
        textHandledCount = view.findViewById(R.id.text_handled_count)
        textSolvedCount = view.findViewById(R.id.text_solved_count)
        imageVerifiedUser = view.findViewById(R.id.image_verified_user)
        expandableLayout0 = view.findViewById(R.id.expandable_layout_0)
        expandableLayout1 = view.findViewById(R.id.expandable_layout_1)
        cardComplaintHandledExpanded = view.findViewById(R.id.card_complaint_handled_expanded)
        cardComplaintSolvedExpanded = view.findViewById(R.id.card_complaint_solved_expanded)
        buttonHandledList = view.findViewById(R.id.button_handled_list)
        buttonHandledPost = view.findViewById(R.id.button_handled_post)
        buttonSolvedList = view.findViewById(R.id.button_solved_list)
        buttonSolvedPost = view.findViewById(R.id.button_solved_post)
        textNoComplaintHandled = view.findViewById(R.id.text_no_complaint_handled)
        textNoComplaintSolved = view.findViewById(R.id.text_no_complaint_solved)
        imageArrowRight = view.findViewById(R.id.image_arrow_right)
        imageArrowRight2 = view.findViewById(R.id.image_arrow_right_2)
        imageMyComplaintInfo = view.findViewById(R.id.image_my_complaint_info)

        Constants.sharedPref =
            view.context.getSharedPreferences("sharedPref", Context.MODE_PRIVATE)
        textUserName.text = Constants.userName

        database = Constants.database
        refreshApp()
        loadUserData()
        loadComplaint()

        imageMyComplaintInfo.setOnClickListener {
            Dialog(view.context).apply {
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                setCancelable(true)
                setContentView(R.layout.layout_my_workshop_info)
                val buttonOK = findViewById<Button>(R.id.button_ok)
                buttonOK.setOnClickListener {
                    dismiss()
                }
                val layoutParams = WindowManager.LayoutParams()
                layoutParams.copyFrom(window?.attributes)
                layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
                layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
                window?.attributes = layoutParams
                window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                window?.setWindowAnimations(R.style.DialogAnimation)
                show()
            }
        }

        cardAvatar.setOnClickListener {
            val intent = Intent(view.context, MyAccountActivity::class.java)
            view.context.startActivity(intent)
        }

        textUserName.setOnClickListener {
            val intent = Intent(view.context, MyAccountActivity::class.java)
            view.context.startActivity(intent)
        }

        textDepartment.setOnClickListener {
            val intent = Intent(view.context, MyAccountActivity::class.java)
            view.context.startActivity(intent)
        }

        imageVerifiedUser.setOnClickListener {
            val toast = Toast.makeText(view.context, "Verified User", Toast.LENGTH_SHORT)
            toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0)
            toast.show()
        }

        buttonHandledList.setOnClickListener {
            val intent = Intent(view.context, ComplaintListActivity::class.java)
            intent.putExtra("complaint_status", "Pending")
            view.context.startActivity(intent)
        }

        buttonSolvedList.setOnClickListener {
            val intent = Intent(view.context, ComplaintListActivity::class.java)
            intent.putExtra("complaint_status", "Solved")
            view.context.startActivity(intent)
        }

        buttonHandledPost.setOnClickListener {
            val intent = Intent(view.context, ComplaintPostActivity::class.java)
            intent.putExtra("complaint_status", "Pending")
            view.context.startActivity(intent)
        }

        buttonSolvedPost.setOnClickListener {
            val intent = Intent(view.context, ComplaintPostActivity::class.java)
            intent.putExtra("complaint_status", "Solved")
            view.context.startActivity(intent)
        }

        cardComplaintHandled.setOnClickListener {
            if (expandableLayout0.isExpanded) {
                expandableLayout0.collapse()
                imageArrowRight.rotation = 0F
            } else {
                expandableLayout0.expand()
                expandableLayout1.collapse()

                imageArrowRight.rotation = 90F
                imageArrowRight2.rotation = 0F
            }
        }
        cardComplaintSolved.setOnClickListener {
            if (expandableLayout1.isExpanded) {
                expandableLayout1.collapse()
                imageArrowRight2.rotation = 0F
            } else {
                expandableLayout0.collapse()
                expandableLayout1.expand()

                imageArrowRight.rotation = 0F
                imageArrowRight2.rotation = 90F
            }
        }
    }

    private fun refreshApp() {
        swipeToRefresh.setOnRefreshListener {
            loadUserData()
            loadComplaint()
            Toast.makeText(requireContext(), "Page Refreshed.", Toast.LENGTH_SHORT).show()
            swipeToRefresh.isRefreshing = false
        }
    }

    private fun loadUserData() {
        val uid = Constants.userUID
        reference = database.getReference("User/$uid")
        reference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                //load avatar
                try {
                    Glide.with(imageViewAvatar.context)
                        .load(snapshot.child("user_avatar").value.toString())
                        .apply(Constants.requestOptions)
                        .placeholder(R.drawable.ic_avatar)
                        .error(R.drawable.errorimg)
                        .into(imageViewAvatar)
                }catch (e: Exception){
                    Log.d("Haha", e.message.toString())
                }

                textDepartment.text = snapshot.child("user_department").value.toString()

                if (snapshot.child("user_nric").value.toString() != "" && snapshot.child("user_phone_num").value.toString() != "") {
                    imageVerifiedUser.visibility = View.VISIBLE
                }else{
                    imageVerifiedUser.visibility = View.GONE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }


    private fun loadComplaint() {
        val uid = Constants.userUID
        reference = database.getReference("Complaint")
        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                complaintRegisteredList.clear()
                complaintSolvedList.clear()
                handledCount = 0
                solvedCount = 0

                if (snapshot.exists()) {
                    for (data in snapshot.children) {
                        for (i in data.children){
                            if (i.child("complaint_managed_by").value.toString() == uid){
                                if (i.child("complaint_status").value.toString() == "Pending") {
                                    val complaint = i.getValue(Complaint::class.java)
                                    complaintRegisteredList.add(complaint!!)
                                    handledCount++
                                } else if (i.child("complaint_status").value.toString() == "Done") {
                                    val complaint = i.getValue(Complaint::class.java)
                                    complaintSolvedList.add(complaint!!)
                                    solvedCount++
                                }
                            }
                        }
                    }
                }
                if (handledCount > 0){
                    cardComplaintHandledExpanded.visibility = View.VISIBLE
                    textNoComplaintHandled.visibility = View.GONE
                }else{
                    cardComplaintHandledExpanded.visibility = View.GONE
                    textNoComplaintHandled.visibility = View.VISIBLE
                }
                if (solvedCount > 0){
                    cardComplaintSolvedExpanded.visibility = View.VISIBLE
                    textNoComplaintSolved.visibility = View.GONE
                }else{
                    cardComplaintSolvedExpanded.visibility = View.GONE
                    textNoComplaintSolved.visibility = View.VISIBLE
                }

                textHandledCount.text = handledCount.toString()
                textSolvedCount.text = solvedCount.toString()
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }
}