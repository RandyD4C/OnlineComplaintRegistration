package com.cyx.onlinecomplaintregistration.resident.activities.user

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import com.cyx.onlinecomplaintregistration.R

class AboutUsActivity : AppCompatActivity() {

    private lateinit var buttonCall: Button
    private lateinit var buttonEmail: Button
    private lateinit var imageInstagram: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about_us)
        this.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        this.supportActionBar?.title = "About Us"

        buttonCall = findViewById(R.id.button_call)
        buttonEmail = findViewById(R.id.button_email)
        imageInstagram = findViewById(R.id.image_instagram)

        buttonCall.setOnClickListener {
            val dialIntent = Intent(Intent.ACTION_DIAL)
            dialIntent.data = Uri.parse("tel:0174152563")
            startActivity(dialIntent)
        }
        buttonEmail.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO)
            intent.data = Uri.parse("mailto: chongyongxuan2000@gmail.com")
            intent.putExtra(Intent.EXTRA_SUBJECT, "")
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            }
        }
        imageInstagram.setOnClickListener {
            val url = "http://www.instagram.com/randy_0903?utm_source=qr"
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            startActivity(i)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}