package com.example.pro_impact_hub

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    private lateinit var mediaPlayer: MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.splash_screen)

        // Initialize and start MediaPlayer
        mediaPlayer = MediaPlayer.create(this, R.raw.drum)
        mediaPlayer.start()

        val splashImage: ImageView = findViewById(R.id.splashImage)
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        splashImage.startAnimation(fadeIn)

        // Navigate to MainActivity after animation ends
        fadeIn.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}

            override fun onAnimationEnd(animation: Animation?) {
                val intent = Intent(this@SplashActivity, MainActivity::class.java)
                startActivity(intent)
                finish() // Close the SplashActivity so the user can't go back to it
            }

            override fun onAnimationRepeat(animation: Animation?) {}
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release() // Release the MediaPlayer resource
    }
}
