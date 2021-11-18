package com.cyx.onlinecomplaintregistration.classes

class Notification(
    val notification_uid: String,
    val notification_title: String,
    val notification_message: String,
    val notification_date_time: String,
    val notification_category: Int,
    val notification_status: String,
    val user_uid: String
    ) {
    constructor():this("","","","",0,"","")
}