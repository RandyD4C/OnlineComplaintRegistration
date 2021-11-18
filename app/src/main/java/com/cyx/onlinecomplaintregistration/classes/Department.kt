package com.cyx.onlinecomplaintregistration.classes

class Department(
    val department_uid: String,
    val department_name: String,
    val department_status: String
) {
    constructor():this("","","")
}