package com.cyx.onlinecomplaintregistration.resident.adapters

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.icu.text.CompactDecimalFormat
import android.os.Build
import android.view.*
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cyx.onlinecomplaintregistration.R
import com.cyx.onlinecomplaintregistration.classes.Complaint
import com.cyx.onlinecomplaintregistration.classes.Constants
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.firebase.database.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

class ComplaintPostNoButtonRecyclerAdapter(
    private var complaint: List<Complaint>
) : RecyclerView.Adapter<ComplaintPostNoButtonRecyclerAdapter.ViewHolder>(), View.OnCreateContextMenuListener {

    private var upVoteCount = 0F
    private var downVoteCount = 0F
    private val entries = mutableListOf<BarEntry>()
    private lateinit var database: FirebaseDatabase
    private lateinit var reference: DatabaseReference

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
        val frameSolved: FrameLayout = itemView.findViewById(R.id.frame_solved)
        val frameRemoved: FrameLayout = itemView.findViewById(R.id.frame_removed)
        val buttonViewLocation: Button = itemView.findViewById(R.id.button_view_location)

        init {
            Constants.sharedPref =
                itemView.context.getSharedPreferences("sharedPref", Context.MODE_PRIVATE)
            database = Constants.database
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.activity_complaint_post_no_button, parent, false)
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

        numOfUpVote(complaint[position].complaint_uid, holder.textUpVote)
        numOfDownVote(complaint[position].complaint_uid, holder.textDownVote)

        if (complaint[position].complaint_status == "Done"){
            holder.frameSolved.visibility = View.VISIBLE
            holder.frameRemoved.visibility = View.GONE
            holder.buttonViewLocation.visibility = View.GONE
        }else if (complaint[position].complaint_status == "Removed"){
            holder.frameSolved.visibility = View.GONE
            holder.frameRemoved.visibility = View.VISIBLE
            holder.buttonViewLocation.visibility = View.GONE
        }else{
            holder.frameSolved.visibility = View.GONE
            holder.frameRemoved.visibility = View.GONE
            holder.buttonViewLocation.visibility = View.VISIBLE
        }
        holder.frameSolved.setOnClickListener {
            Dialog(it.context).apply {
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                setCancelable(true)
                setContentView(R.layout.layout_complaint_solved)
                val layoutParams = WindowManager.LayoutParams()
                layoutParams.copyFrom(window?.attributes)
                layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
                layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
                window?.attributes = layoutParams
                window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                window?.setWindowAnimations(R.style.DialogAnimation)
                loadComplaintSolved(
                    this,
                    complaint[position].complaint_post_by,
                    complaint[position].complaint_uid
                )
                val buttonOK = findViewById<Button>(R.id.button_ok)
                buttonOK.setOnClickListener {
                    dismiss()
                }
                show()
            }
        }
        holder.textUpVote.setOnClickListener {
            loadVoteCount(holder.itemView.context, complaint[position].complaint_uid)
        }
        holder.textDownVote.setOnClickListener {
            loadVoteCount(holder.itemView.context, complaint[position].complaint_uid)
        }
    }

    private fun loadComplaintSolved(dialog: Dialog, complaintPostBy: String, complaintUid: String) {
        val textSolvedDate = dialog.findViewById<TextView>(R.id.text_solved_date)
        val textSolvedBy = dialog.findViewById<TextView>(R.id.text_solved_by)
        val textDepartment = dialog.findViewById<TextView>(R.id.text_department)
        reference = database.getReference("Complaint/$complaintPostBy/$complaintUid")
        reference.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()){
                    val dateTime = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        LocalDateTime.parse(snapshot.child("complaint_solved_date").value.toString())
                    } else {
                        TODO("VERSION.SDK_INT < O")
                    }
                    val formatter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                    } else {
                        TODO("VERSION.SDK_INT < O")
                    }
                    val formatted = dateTime.format(formatter)
                    textSolvedDate.text = formatted

                    loadSolvedBy(snapshot.child("complaint_managed_by").value.toString(), textSolvedBy, textDepartment)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

    private fun loadSolvedBy(uid: String, textSolvedBy: TextView, textDepartment: TextView) {
        reference = database.getReference("User/$uid")
        reference.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()){
                    textSolvedBy.text = snapshot.child("user_full_name").value.toString()
                    textDepartment.text = snapshot.child("user_department").value.toString()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
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

    private fun numOfDownVote(complaintUid: String, textDownVote: TextView) {
        reference = database.getReference("DownVote")
        reference.child("$complaintUid").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                downVoteCount = snapshot.childrenCount.toFloat()
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

    private fun numOfUpVote(complaintUID: String, textUpVoteCount: TextView) {
        reference = database.getReference("UpVote")
        reference.child("$complaintUID").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                upVoteCount = snapshot.childrenCount.toFloat()
                if (snapshot.childrenCount > 1) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        textUpVoteCount.text =
                            "${CompactDecimalFormat.getInstance(
                                Locale.US,
                                CompactDecimalFormat.CompactStyle.SHORT
                            ).format(snapshot.childrenCount)} Upvotes"
                    }
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        textUpVoteCount.text =
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