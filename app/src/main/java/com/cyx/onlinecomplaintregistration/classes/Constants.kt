package com.cyx.onlinecomplaintregistration.classes

import android.Manifest
import android.content.SharedPreferences
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class Constants {
    companion object{
        const val BASE_URL = "https://fcm.googleapis.com"
        const val SERVER_KEY = "AAAAEiPub9U:APA91bF2TQGC55MoGMDso5WI0AjSRR_OIpiaWdvUJdFgmGHtDgVjpjf3w__RTy8WogZ63h8J4jPpO_NXNlP3Qqz68e0t12biTaKXyTif1zwI1muzq4px4vkPMe4grrDc-ShMRtA3CXdu"
        const val CONTENT_TYPE = "application/json"
        val requestOptions = RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL)
        val database =
            Firebase.database("https://august-cirrus-328814-default-rtdb.asia-southeast1.firebasedatabase.app/")

        var sharedPref: SharedPreferences? = null
        var userRole: String?
            get() {
                return sharedPref?.getString("user_role", "")
            }
            set(value) {
                sharedPref?.edit()?.putString("user_role",value)?.apply()
            }

        var userUID: String?
            get() {
                return sharedPref?.getString("user_uid", "")
            }
            set(value) {
                sharedPref?.edit()?.putString("user_uid",value)?.apply()
            }

        var userName: String?
            get() {
                return sharedPref?.getString("user_name", "")
            }
            set(value) {
                sharedPref?.edit()?.putString("user_name",value)?.apply()
            }

        const val TAG = "cameraX"
        const val FILE_NAME_FORMAT = "yy-MM-dd-HH-mm-ss-SSS"
        const val REQUEST_CODE_PERMISSIONS = 123
        val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}