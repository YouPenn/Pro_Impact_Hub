package com.example.pro_impact_hub

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.MediaController
import android.widget.TextView
import android.widget.VideoView
import androidx.fragment.app.Fragment

class AboutFragment : Fragment() {

    private lateinit var videoView: VideoView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_about, container, false)

        val tvEmail = view.findViewById<TextView>(R.id.tvEmail)
        val tvPhone = view.findViewById<TextView>(R.id.tvPhone)
        val tvAddress = view.findViewById<TextView>(R.id.tvAddress)
        videoView = view.findViewById(R.id.videoView)

        tvEmail.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("mailto:proimpacthub@gmail.com"))
            startActivity(intent)
        }

        tvPhone.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:0190721617"))
            startActivity(intent)
        }

        tvAddress.setOnClickListener {
            val intent = Intent(context, Map::class.java)
            startActivity(intent)
        }

        // Set up VideoView with local video
        setupVideoView(view)

        return view
    }

    private fun setupVideoView(root: View) {
        videoView = root.findViewById(R.id.videoView)
        val videoUri: Uri = Uri.parse("android.resource://${requireContext().packageName}/raw/ph_see_you_again")
        videoView.setVideoURI(videoUri)

        val mediaController = MediaController(context)
        videoView.setMediaController(mediaController)
        mediaController.setAnchorView(videoView)

        videoView.setOnPreparedListener {
            videoView.start()
        }
    }
}
