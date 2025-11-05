package com.example.pro_impact_hub

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import android.app.NotificationChannel
import android.app.NotificationManager

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Create notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val eventChannel = NotificationChannel(
                "eventChannelId", "EventChannel", NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Channel for Event Notifications"
            }
            val feedbackChannel = NotificationChannel(
                "feedbackChannelId", "FeedbackChannel", NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Channel for Feedback Notifications"
            }
            val notificationManager: NotificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(eventChannel)
            notificationManager.createNotificationChannel(feedbackChannel)
        }

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        val btnUserObj = findViewById<Button>(R.id.btnUser)
        btnUserObj.setOnClickListener {
            val intent = Intent(this, UserLogin::class.java)
            startActivity(intent)
        }

        val btnAdminObj = findViewById<Button>(R.id.btnAdmin)
        btnAdminObj.setOnClickListener {
            val intent = Intent(this, AdminLogin::class.java)
            startActivity(intent)
        }
    }

    override fun onBackPressed() {
        // Do nothing, disable the back button functionality
    }
}
