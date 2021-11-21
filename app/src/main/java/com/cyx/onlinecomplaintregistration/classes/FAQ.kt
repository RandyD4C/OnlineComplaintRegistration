package com.cyx.onlinecomplaintregistration.classes

class FAQ(
    val faq_question: String,
    val faq_answer: String,
    val faq_uid: String
) {
    constructor():this("","","")
}