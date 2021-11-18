package com.cyx.onlinecomplaintregistration.activities

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatDelegate
import com.cyx.onlinecomplaintregistration.R
import com.cyx.onlinecomplaintregistration.fragments.Registration1Fragment
import com.cyx.onlinecomplaintregistration.fragments.Registration2Fragment
import com.cyx.onlinecomplaintregistration.interfaces.Communicator

class RegistrationActivity : AppCompatActivity(), Communicator {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN)
        val fragmentRegister1 = Registration1Fragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragmentRegister1).commit()
    }

    override fun passDataCom(
        fullname: String?,
        username: String?,
        email: String?,
        password: String?,
        avatar: Uri?
    ) {
        val bundle = Bundle()
        bundle.putString("fullname", fullname)
        bundle.putString("username", username)
        bundle.putString("email", email)
        bundle.putString("password", password)
        bundle.putString("avatar", avatar.toString())

        val transaction = this.supportFragmentManager.beginTransaction()
        val fragmentRegister2 = Registration2Fragment()
        fragmentRegister2.arguments = bundle

        transaction.replace(R.id.fragment_container, fragmentRegister2)
        transaction.commit()
    }

    override fun goBackFragment1(
        fullname: String?,
        username: String?,
        email: String?,
        password: String?,
        avatar: Uri?
    ) {
        val bundle = Bundle()
        bundle.putString("fullname", fullname)
        bundle.putString("username", username)
        bundle.putString("email", email)
        bundle.putString("password", password)
        bundle.putString("avatar", avatar.toString())

        val transaction = this.supportFragmentManager.beginTransaction()
        val fragmentRegister1 = Registration1Fragment()
        fragmentRegister1.arguments = bundle

        transaction.replace(R.id.fragment_container, fragmentRegister1)
        transaction.commit()
    }
}