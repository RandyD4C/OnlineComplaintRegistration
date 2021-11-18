package com.cyx.onlinecomplaintregistration.interfaces

import android.net.Uri
import android.widget.EditText

interface Communicator {
    fun passDataCom(fullname: String?, username: String?, email: String?, password: String?, avatar: Uri?)
    fun goBackFragment1(fullname: String?, username: String?, email: String?, password: String?, avatar: Uri?)
}