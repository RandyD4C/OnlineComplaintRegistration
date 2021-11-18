package com.cyx.onlinecomplaintregistration.classes


class Complaint(
    val complaint_uid: String,
    val complaint_priority: Int,
    val complaint_category: String,
    val complaint_description: String,
    val complaint_photo: String,
    val complaint_video: String,
    val complaint_latitude: Double,
    val complaint_longitude: Double,
    val complaint_postal_code: String,
    val complaint_date_time: String,
    val complaint_post_by: String,
    val complaint_managed_by: String,
    val complaint_solved_date: String,
    val complaint_status: String,
    val user_avatar: String,
    val user_name: String,
) {
    constructor():this("",0,"","","","",0.0,0.0,"","","","","","", "", "")
}