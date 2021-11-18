package com.cyx.onlinecomplaintregistration.resident.adapters

import android.app.AlertDialog
import android.app.Dialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.icu.text.CompactDecimalFormat
import android.os.Build
import android.util.Log
import android.view.*
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cyx.onlinecomplaintregistration.R
import com.cyx.onlinecomplaintregistration.classes.Complaint
import com.cyx.onlinecomplaintregistration.classes.Constants
import com.cyx.onlinecomplaintregistration.resident.activities.home.EditComplaintActivity
import com.cyx.onlinecomplaintregistration.resident.activities.map.ViewLocationActivity
import com.cyx.onlinecomplaintregistration.resident.activities.user.MyAccountActivity
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

class ComplaintPostRecyclerAdapter(
    private var complaint: List<Complaint>
) : RecyclerView.Adapter<ComplaintPostRecyclerAdapter.ViewHolder>(), View.OnCreateContextMenuListener {

    private lateinit var database: FirebaseDatabase
    private lateinit var reference: DatabaseReference
    private lateinit var progressBar: ProgressDialog
    private val entries = mutableListOf<BarEntry>()

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userImg: ImageView = itemView.findViewById(R.id.user_avatar)
        val userName: TextView = itemView.findViewById(R.id.text_name)
        val complaintDescription: TextView = itemView.findViewById(R.id.text_complaint_desc)
        val postDateTime: TextView = itemView.findViewById(R.id.text_post_date)
        val complaintImg: ImageView = itemView.findViewById(R.id.image_complaint)
        val textUpVote: TextView = itemView.findViewById(R.id.text_upvote)
        val textDownVote: TextView = itemView.findViewById(R.id.text_down_vote)
        val textUrgent: TextView = itemView.findViewById(R.id.text_urgent)
        val textImportant: TextView = itemView.findViewById(R.id.text_important)
        val textComplaintCategory: TextView = itemView.findViewById(R.id.text_complaint_category)
        val buttonEdit: MaterialButton = itemView.findViewById(R.id.button_edit)
        val buttonDelete: MaterialButton = itemView.findViewById(R.id.button_delete)
        val cardAvatar: CardView = itemView.findViewById(R.id.cardView)
        val verifiedUser: ImageView = itemView.findViewById(R.id.image_verified_user)
        val buttonViewLocation: Button = itemView.findViewById(R.id.button_view_location)

        init {
            Constants.sharedPref =
                itemView.context.getSharedPreferences("sharedPref", Context.MODE_PRIVATE)
            database = Constants.database
        }


    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.activity_complaint_post, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Glide.with(holder.userImg.context)
            .load(complaint[position].user_avatar)
            .apply(Constants.requestOptions)
            .placeholder(R.drawable.ic_avatar)
            .error(R.drawable.errorimg)
            .into(holder.userImg)
        holder.userName.text = complaint[position].user_name
        if (complaint[position].complaint_priority == 1) {
            holder.textImportant.visibility = View.VISIBLE
            holder.textUrgent.visibility = View.GONE
        } else if (complaint[position].complaint_priority == 2) {
            holder.textImportant.visibility = View.GONE
            holder.textUrgent.visibility = View.VISIBLE
        } else {
            holder.textImportant.visibility = View.GONE
            holder.textUrgent.visibility = View.GONE
        }
        val dateTime = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocalDateTime.parse(complaint[position].complaint_date_time)
        } else {
            TODO("VERSION.SDK_INT < O")
        }
        val formatter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
        } else {
            TODO("VERSION.SDK_INT < O")
        }
        val formatted = dateTime.format(formatter)
        holder.postDateTime.text = formatted
        holder.textComplaintCategory.text = "#${complaint[position].complaint_category}"
        if (complaint[position].complaint_description == "") {
            holder.complaintDescription.visibility = View.GONE
        } else {
            holder.complaintDescription.visibility = View.VISIBLE
            holder.complaintDescription.text = complaint[position].complaint_description
        }

        Glide.with(holder.complaintImg.context)
            .load(complaint[position].complaint_photo)
            .apply(Constants.requestOptions)
            .placeholder(R.drawable.loadingimg)
            .error(R.drawable.errorimg)
            .into(holder.complaintImg)

        loadVerifiedUser(complaint[position].complaint_post_by, holder.verifiedUser)
        numOfUpVote(complaint[position].complaint_uid, holder.textUpVote)
        numOfDownVote(complaint[position].complaint_uid, holder.textDownVote)

        holder.cardAvatar.setOnClickListener {
            val intent = Intent(holder.itemView.context, MyAccountActivity::class.java)
            holder.itemView.context.startActivity(intent)
        }
        holder.userName.setOnClickListener {
            val intent = Intent(holder.itemView.context, MyAccountActivity::class.java)
            holder.itemView.context.startActivity(intent)
        }

        holder.buttonViewLocation.setOnClickListener {
            Intent(holder.itemView.context, ViewLocationActivity::class.java).apply {
                putExtra("latitude", complaint[position].complaint_latitude)
                putExtra("longitude", complaint[position].complaint_longitude)
                holder.itemView.context.startActivity(this)
            }
        }

        holder.textUpVote.setOnClickListener {
            loadVoteCount(holder.itemView.context, complaint[position].complaint_uid)
        }
        holder.textDownVote.setOnClickListener {
            loadVoteCount(holder.itemView.context, complaint[position].complaint_uid)
        }

        holder.buttonEdit.setOnClickListener {
            val intent = Intent(holder.itemView.context, EditComplaintActivity::class.java)
            intent.putExtra("complaint_uid", complaint[position].complaint_uid)
            holder.itemView.context.startActivity(intent)
        }
        holder.buttonDelete.setOnClickListener {
            val builder = AlertDialog.Builder(holder.itemView.context)
            builder.setTitle("Delete Complaint")
            builder.setMessage("Are you sure you want to DELETE this Complaint?")
            builder.setCancelable(true)
            builder.setPositiveButton("Yes") { dialog, _ ->
                dialog.dismiss()
                progressBar = ProgressDialog(holder.itemView.context)
                progressBar.setMessage("Deleting Complaint...")
                progressBar.setCancelable(false)
                progressBar.show()
                deleteComplaint(complaint[position].complaint_uid, holder.itemView.context)
            }
            builder.setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            val alert = builder.create()
            alert.show()
        }
    }

    private fun loadVoteCount(context: Context?, complaintUid: String) {
        reference = database.getReference("UpVote")
        reference.child(complaintUid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val upVoteCount = snapshot.childrenCount.toFloat()
                reference = database.getReference("DownVote")
                reference.child(complaintUid).addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val downVoteCount = snapshot.childrenCount.toFloat()
                        loadBarChartDialog(context, upVoteCount, downVoteCount)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        TODO("Not yet implemented")
                    }

                })
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

    private fun loadBarChartDialog(context: Context?, upVoteCount: Float, downVoteCount: Float) {
        Dialog(context!!).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setCancelable(true)
            setContentView(R.layout.layout_vote_chart)
            val layoutParams = WindowManager.LayoutParams()
            layoutParams.copyFrom(window?.attributes)
            layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
            window?.attributes = layoutParams
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window?.setWindowAnimations(R.style.DialogAnimation)
            showBarChart(this, context, upVoteCount, downVoteCount)
            val buttonBack = findViewById<ImageView>(R.id.image_back)
            buttonBack.setOnClickListener {
                dismiss()
            }
            show()
        }
    }

    private fun showBarChart(
        dialog: Dialog,
        context: Context,
        upVoteCount: Float,
        downVoteCount: Float
    ) {
        entries.clear()
        entries.add(BarEntry(0f, upVoteCount))
        entries.add(BarEntry(1f, downVoteCount))

        val barDataSet = BarDataSet(entries, "")
        barDataSet.setColors(*ColorTemplate.COLORFUL_COLORS)

        val data = BarData(barDataSet)
        val barChart: BarChart = dialog.findViewById(R.id.barChart)
        barChart.data = data
        barChart.data.notifyDataChanged()
        barChart.notifyDataSetChanged()

        barChart.invalidate()

        barChart.axisLeft.setDrawGridLines(false)
        val xAxis: XAxis = barChart.xAxis
        xAxis.setDrawGridLines(false)
        xAxis.setDrawAxisLine(false)

        //remove right y-axis
        barChart.axisRight.isEnabled = true

        //remove legend
        barChart.legend.isEnabled = false


        //remove description label
        barChart.description.isEnabled = false


        //add animation
        barChart.animateY(1500)

        // to draw label on xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.valueFormatter = MyAxisFormatter()
        xAxis.setDrawLabels(true)
        xAxis.granularity = 1f
    }

    inner class MyAxisFormatter : IndexAxisValueFormatter() {

        override fun getAxisLabel(value: Float, axis: AxisBase?): String {
            val index = value.toInt()
            return if (index == 0) "Upvote" else "Downvote"
        }
    }

    private fun loadVerifiedUser(complaintPostBy: String, verifiedUser: ImageView) {
        reference = database.getReference("User/$complaintPostBy")
        reference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    if (snapshot.child("user_nric").value.toString() != "" && snapshot.child("user_phone_num").value.toString() != "") {
                        verifiedUser.visibility = View.VISIBLE
                    } else {
                        verifiedUser.visibility = View.GONE
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

    private fun deleteComplaint(complaintUID: String, context: Context) {
        val uid = Constants.userUID
        reference = database.getReference("Complaint/$uid/$complaintUID")
        reference.removeValue().addOnCompleteListener {
            if (it.isSuccessful){
                val reference = database.getReference("UpVote/$complaintUID")
                reference.removeValue().addOnCompleteListener {
                    val reference = database.getReference("DownVote/$complaintUID")
                    reference.removeValue().addOnCompleteListener {
                        if (progressBar.isShowing) progressBar.dismiss()
                        val builder = AlertDialog.Builder(context)
                        builder.setTitle("Delete Successfully")
                        builder.setMessage("This Complaint has been DELETED.")
                        builder.setCancelable(false)
                        builder.setPositiveButton("OK") { dialog, _ ->
                            dialog.dismiss()
                            notifyDataSetChanged()
                        }
                        val alert = builder.create()
                        alert.show()
                    }
                }
            }
        }
    }

    private fun numOfDownVote(complaintUid: String, textDownVote: TextView) {
        reference = database.getReference("DownVote")
        reference.child("$complaintUid").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.childrenCount > 1) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        textDownVote.text =
                            "${
                                CompactDecimalFormat.getInstance(
                                    Locale.US,
                                    CompactDecimalFormat.CompactStyle.SHORT
                                ).format(snapshot.childrenCount)
                            } Downvotes"
                    }
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        textDownVote.text =
                            "${
                                CompactDecimalFormat.getInstance(
                                    Locale.US,
                                    CompactDecimalFormat.CompactStyle.SHORT
                                ).format(snapshot.childrenCount)
                            } Downvote"
                    }
                }

            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

    private fun numOfUpVote(complaintUID: String, upVoteCount: TextView) {
        reference = database.getReference("UpVote")
        reference.child("$complaintUID").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.childrenCount > 1) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        upVoteCount.text =
                            "${CompactDecimalFormat.getInstance(
                                Locale.US,
                                CompactDecimalFormat.CompactStyle.SHORT
                            ).format(snapshot.childrenCount)} Upvotes"
                    }
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        upVoteCount.text =
                            "${CompactDecimalFormat.getInstance(
                                Locale.US,
                                CompactDecimalFormat.CompactStyle.SHORT
                            ).format(snapshot.childrenCount)} Upvote"
                    }
                }

            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

    override fun getItemCount(): Int {
        return complaint.size
    }

    override fun onCreateContextMenu(
        menu: ContextMenu?,
        v: View?,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        TODO("Not yet implemented")
    }

}