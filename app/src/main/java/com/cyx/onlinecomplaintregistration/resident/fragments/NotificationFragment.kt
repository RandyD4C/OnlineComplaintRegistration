package com.cyx.onlinecomplaintregistration.resident.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.ImageView
import androidx.fragment.app.Fragment
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cyx.onlinecomplaintregistration.R
import com.cyx.onlinecomplaintregistration.adapters.NotificationRecyclerAdapter
import com.cyx.onlinecomplaintregistration.classes.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.fragment_all_complaint_resident.*

class NotificationFragment : Fragment() {

    private lateinit var database: FirebaseDatabase
    private lateinit var reference: DatabaseReference
    private var notificationList = mutableListOf<Notification>()
    private lateinit var progressBar: ProgressBar
    private lateinit var textNoNotification: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var notificationAdapter: NotificationRecyclerAdapter
    private lateinit var imageInfo: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_notification, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progressBar = view.findViewById(R.id.progress_bar)
        textNoNotification = view.findViewById(R.id.text_no_notification)
        recyclerView = view.findViewById(R.id.rv_recyclerView)
        imageInfo = view.findViewById(R.id.image_info)

        database = Constants.database
        loadNotification(view)

        imageInfo.setOnClickListener {
            Dialog(it.context).apply {
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                setCancelable(true)
                setContentView(R.layout.layout_notification_info)
                val layoutParams = WindowManager.LayoutParams()
                layoutParams.copyFrom(window?.attributes)
                layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
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

//        MyFirebaseMessagingService.sharedPref = view.context.getSharedPreferences("sharedPref", Context.MODE_PRIVATE)
//        FirebaseMessaging.getInstance().token.addOnSuccessListener {
//            MyFirebaseMessagingService.token = it
//            edit_text_token.setText(it)
//        }
////        FirebaseMessaging.getInstance().subscribeToTopic(TOPIC)
//
//        button_send.setOnClickListener {
//            val title = edit_text_title.text.toString()
//            val message = edit_text_message.text.toString()
//            val recipientToken = edit_text_token.text.toString()
//            if (title.isNotEmpty() && message.isNotEmpty() && recipientToken.isNotEmpty()){
//                PushNotification(
//                    NotificationData(
//                        title, message
//                    ), recipientToken
//                ).also {
//                    sendNotification(it)
//                }
//            }
//        }
    }

    private fun loadNotification(view: View) {
        val uid = Constants.userUID
        reference = database.getReference("Notification/$uid")
        reference.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                notificationList.clear()
                if (snapshot.exists()){
                    for (data in snapshot.children){
                        notificationList.add(data.getValue(Notification::class.java)!!)
                    }
                }else{
                    progressBar.visibility = View.GONE
                    textNoNotification.visibility = View.VISIBLE
                }

                if (notificationList.size > 0){
                    progressBar.visibility = View.GONE
                    textNoNotification.visibility = View.GONE
                    linearLayoutManager = LinearLayoutManager(view.context)
                    recyclerView.layoutManager = linearLayoutManager
                    notificationList.sortByDescending { it.notification_date_time }
                    notificationAdapter = NotificationRecyclerAdapter(notificationList)
                    val swipeGesture = object : SwipeGesture(view.context){
                        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                            when(direction){
                                ItemTouchHelper.LEFT ->{
                                    notificationAdapter.deleteNotification(viewHolder.adapterPosition, view.context)
                                }
                            }
                        }
                    }
                    val touchHelper = ItemTouchHelper(swipeGesture)
                    touchHelper.attachToRecyclerView(recyclerView)
                    notificationAdapter.notifyItemInserted(notificationAdapter.itemCount)
                    notificationAdapter.notifyDataSetChanged()
                    recyclerView.adapter = notificationAdapter
                }else{
                    progressBar.visibility = View.GONE
                    textNoNotification.visibility = View.VISIBLE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

//    private fun sendNotification(notification: PushNotification) = CoroutineScope(Dispatchers.IO).launch {
//        try {
//            val response = RetrofitInstance.api.postNotification(notification)
//            if (response.isSuccessful){
//                Log.d(TAG, "Response: ${Gson().toJson(response)}")
//            }else{
//                Log.d(TAG, response.errorBody().toString())
//            }
//        }catch (e: Exception){
//            Log.e(TAG, e.toString())
//        }
//    }
}