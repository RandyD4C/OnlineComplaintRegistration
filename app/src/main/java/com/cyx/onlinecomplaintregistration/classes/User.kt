package com.cyx.onlinecomplaintregistration.classes

class User(
    var user_uid: String,
    var user_full_name: String,
    var user_name: String,
    var user_nric: String,
    var user_email: String,
    var user_phone_num: String,
    var user_avatar: String,
    var user_department: String,
    var user_role: String,
    var user_status: String
) {
    constructor():this("","","","","","","","","","")
}