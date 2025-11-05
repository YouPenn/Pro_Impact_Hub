package com.example.pro_impact_hub

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import android.os.Handler
import com.google.firebase.firestore.FirebaseFirestore


class UserForgotPass : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_forgot_pass)


        val imgBackObj = findViewById<ImageButton>(R.id.imgBack)
        imgBackObj.setOnClickListener {
            val intent = Intent(this, UserLogin::class.java)
            startActivity(intent)
        }

        val imgSecurityObj = findViewById<ImageButton>(R.id.imgSecurity)
        val imgOTPObj = findViewById<ImageButton>(R.id.imgOTP)

        imgSecurityObj.setOnClickListener{
            imgSecurityObj.post {
                imgSecurityObj.visibility = View.GONE
                imgOTPObj.visibility = View.GONE
            }
            imgSecurityObj.isClickable = false
            imgOTPObj.isClickable = false
            imgBackObj.isClickable = false
            replaceFragment(SecurityFragment())
        }

        imgOTPObj.setOnClickListener{
            imgOTPObj.post {
                imgSecurityObj.visibility = View.GONE
                imgOTPObj.visibility = View.GONE
            }
            imgSecurityObj.isClickable = false
            imgOTPObj.isClickable = false
            imgBackObj.isClickable = false
            replaceFragment(OtpFragment())
        }
    }
    private fun replaceFragment(fragment: Fragment){
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.forgot_pass,fragment)
            .commit()
    }
}


