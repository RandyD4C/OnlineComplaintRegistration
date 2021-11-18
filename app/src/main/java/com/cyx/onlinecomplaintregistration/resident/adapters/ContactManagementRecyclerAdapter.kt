package com.cyx.onlinecomplaintregistration.resident.adapters

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.view.*
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cyx.onlinecomplaintregistration.R
import com.cyx.onlinecomplaintregistration.classes.Constants
import com.cyx.onlinecomplaintregistration.classes.User
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase


class ContactManagementRecyclerAdapter(
    private var user: List<User>
) : RecyclerView.Adapter<ContactManagementRecyclerAdapter.ViewHolder>() {

    private lateinit var database: FirebaseDatabase
    private lateinit var reference: DatabaseReference

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageAvatar: ImageView = itemView.findViewById(R.id.image_avatar)
        val textUserFullName: TextView = itemView.findViewById(R.id.text_user_full_name)
        val textDepartment: TextView = itemView.findViewById(R.id.text_department)
        val textPhoneNum: TextView = itemView.findViewById(R.id.text_phone_num)
        val imagePhone: ImageView = itemView.findViewById(R.id.image_phone)
        val cardContactManagement: CardView = itemView.findViewById(R.id.card_contact_management)

        init {
            Constants.sharedPref =
                itemView.context.getSharedPreferences("sharedPref", Context.MODE_PRIVATE)
            database = Constants.database
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.contact_management_recycler_view, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Glide.with(holder.imageAvatar.context)
            .load(user[position].user_avatar)
            .apply(Constants.requestOptions)
            .placeholder(R.drawable.ic_avatar)
            .error(R.drawable.errorimg)
            .into(holder.imageAvatar)

        holder.textUserFullName.text = user[position].user_full_name
        holder.textDepartment.text = user[position].user_department
        if (user[position].user_department == ""){
            holder.textDepartment.text = "-"
        }else{
            holder.textDepartment.text = user[position].user_department
        }
        if (user[position].user_phone_num == ""){
            holder.textPhoneNum.text = "-"
        }else{
            holder.textPhoneNum.text = user[position].user_phone_num
        }

        holder.imagePhone.setOnClickListener {
            val dialIntent = Intent(Intent.ACTION_DIAL)
            dialIntent.data = Uri.parse("tel:" + user[position].user_phone_num)
            holder.itemView.context.startActivity(dialIntent)
        }

        holder.cardContactManagement.setOnClickListener {
            Dialog(it.context).apply {
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                setCancelable(true)
                setContentView(R.layout.layout_view_management)
                val layoutParams = WindowManager.LayoutParams()
                layoutParams.copyFrom(window?.attributes)
                layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
                layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
                window?.attributes = layoutParams
                window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                window?.setWindowAnimations(R.style.DialogAnimation)
                loadManagementDetail(this, holder.itemView, user, position)
                show()
            }
        }
    }

    private fun loadManagementDetail(
        dialog: Dialog,
        itemView: View,
        user: List<User>,
        position: Int
    ) {
        val buttonBack = dialog.findViewById<ImageView>(R.id.image_back)
        val buttonEmail = dialog.findViewById<Button>(R.id.button_email)
        val buttonCall = dialog.findViewById<Button>(R.id.button_phone)
        val imageAvatar = dialog.findViewById<ImageView>(R.id.image_avatar)
        val textUserName = dialog.findViewById<TextView>(R.id.text_user_name)
        val textFullName = dialog.findViewById<TextView>(R.id.text_full_name)
        val textEmail = dialog.findViewById<TextView>(R.id.text_email)
        val textDepartment = dialog.findViewById<TextView>(R.id.text_department)
        val textPhoneNum = dialog.findViewById<TextView>(R.id.text_phone_num)

        Glide.with(imageAvatar.context)
            .load(user[position].user_avatar)
            .apply(Constants.requestOptions)
            .placeholder(R.drawable.ic_avatar)
            .error(R.drawable.errorimg)
            .into(imageAvatar)

        textUserName.text = user[position].user_name
        textFullName.text = user[position].user_full_name
        textEmail.text = user[position].user_email
        textDepartment.text = user[position].user_department
        textPhoneNum.text = user[position].user_phone_num

        buttonBack.setOnClickListener {
            dialog.dismiss()
        }
        buttonEmail.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO)
            intent.data = Uri.parse("mailto: ${user[position].user_email}")
            intent.putExtra(Intent.EXTRA_SUBJECT, "")
            if (intent.resolveActivity(itemView.context.packageManager) != null) {
                itemView.context.startActivity(intent)
            }
        }
        buttonCall.setOnClickListener {
            val dialIntent = Intent(Intent.ACTION_DIAL)
            dialIntent.data = Uri.parse("tel:" + user[position].user_phone_num)
            itemView.context.startActivity(dialIntent)
        }
    }

    override fun getItemCount(): Int {
        return user.size
    }

}