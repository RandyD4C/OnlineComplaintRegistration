package com.cyx.onlinecomplaintregistration.resident.fragments

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import com.cyx.onlinecomplaintregistration.R
import com.cyx.onlinecomplaintregistration.activities.LoginActivity
import com.cyx.onlinecomplaintregistration.classes.Constants
import com.cyx.onlinecomplaintregistration.resident.activities.user.ContactManagementActivity
import com.cyx.onlinecomplaintregistration.resident.activities.user.HelpActivity
import com.cyx.onlinecomplaintregistration.resident.activities.user.MyAccountActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class UserFragment : Fragment() {

    private lateinit var database: FirebaseDatabase
    private lateinit var reference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_user, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val buttonLogout: CardView = view.findViewById(R.id.card_logout)
        val buttonMyAccount: CardView = view.findViewById(R.id.card_my_account)
        val buttonContactManagement: CardView = view.findViewById(R.id.card_contact_management)
        val buttonHelp: CardView = view.findViewById(R.id.card_help)
        val buttonAboutUs: CardView = view.findViewById(R.id.card_about_us)

        database = Constants.database
        Constants.sharedPref = view.context.getSharedPreferences("sharedPref", Context.MODE_PRIVATE)

        buttonMyAccount.setOnClickListener {
            val intent = Intent(view.context, MyAccountActivity::class.java)
            view.context.startActivity(intent)
        }

        buttonContactManagement.setOnClickListener {
            val intent = Intent(view.context, ContactManagementActivity::class.java)
            view.context.startActivity(intent)
        }

        buttonHelp.setOnClickListener {
            val intent = Intent(view.context, HelpActivity::class.java)
            view.context.startActivity(intent)
        }
        buttonAboutUs.setOnClickListener {
            Toast.makeText(view.context, "No function yet", Toast.LENGTH_SHORT).show()
        }

        buttonLogout.setOnClickListener {
            Dialog(view.context).apply {
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                setCancelable(true)
                setContentView(R.layout.layout_logout)
                val buttonYes = findViewById<Button>(R.id.button_yes)
                val buttonNo = findViewById<Button>(R.id.button_no)
                buttonYes.setOnClickListener {
                    dismiss()
                    RemoveUserToken()
                }
                buttonNo.setOnClickListener {
                    dismiss()
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

        }
    }

    private fun RemoveUserToken() {
        val uid = Constants.userUID
        val removeToken = mapOf(
            "user_token" to ""
        )
        reference = database.getReference("User/$uid")
        reference.updateChildren(removeToken).addOnCompleteListener {
            if (it.isSuccessful){
                Constants.userUID = ""
                Constants.userName = ""
                Constants.userRole = ""
                FirebaseAuth.getInstance().signOut()
                val intent = Intent(requireContext(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }else{
                Toast.makeText(requireContext(),it.exception.toString(),Toast.LENGTH_SHORT).show()
            }
        }
    }
}