package com.cyx.onlinecomplaintregistration.classes

class ReportedComplaint(val complaint_uid: String, val reported_complaint_uid: String, val report_description: String) {
    constructor():this("","","")
}