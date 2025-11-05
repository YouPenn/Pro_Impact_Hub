package com.example.pro_impact_hub

import android.os.Bundle
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.firebase.firestore.FirebaseFirestore

class AdminViewFeedback : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var searchEditText: EditText
    private lateinit var searchButton: Button
    private lateinit var scrollViewContainer: LinearLayout
    private lateinit var imgBack: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_view_feedback)

        db = FirebaseFirestore.getInstance()

        searchEditText = findViewById(R.id.searchEditText)
        searchButton = findViewById(R.id.searchButton)
        scrollViewContainer = findViewById(R.id.scrollViewContainer)
        imgBack = findViewById(R.id.imgBack)

        imgBack.setOnClickListener {
            finish() // Go back to the previous activity
        }

        searchButton.setOnClickListener {
            val eventNameToSearch = searchEditText.text.toString().trim()
            if (eventNameToSearch.isNotEmpty()) {
                searchEventFeedback(eventNameToSearch)
            } else {
                Toast.makeText(this, "Please enter an event name", Toast.LENGTH_SHORT).show()
            }
        }

        // Set OnEditorActionListener on searchEditText
        searchEditText.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE || event?.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER) {
                searchButton.performClick()
                true
            } else {
                false
            }
        }
    }

    private fun searchEventFeedback(eventName: String) {
        db.collection("Event_Feedback").whereEqualTo("EventName", eventName).get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(this, "No feedback found for the event", Toast.LENGTH_SHORT).show()
                } else {
                    scrollViewContainer.removeAllViews() // Clear existing feedback views
                    for (document in documents) {
                        val eventTitle = document.getString("EventName") ?: "N/A"
                        val userName = document.getString("UserEmail") ?: "N/A"
                        val feedbackDesc = document.getString("Feedback") ?: "N/A"

                        val feedbackView = layoutInflater.inflate(R.layout.feedback, null) as CardView

                        val tvEventTitle: TextView = feedbackView.findViewById(R.id.tvEventTitle)
                        val tvUserName: TextView = feedbackView.findViewById(R.id.tvUserName)
                        val tvFeedbackDesc: TextView = feedbackView.findViewById(R.id.tvFeedbackDesc)

                        tvEventTitle.text = eventTitle
                        tvUserName.text = userName
                        tvFeedbackDesc.text = feedbackDesc

                        scrollViewContainer.addView(feedbackView)
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error fetching feedback: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}
