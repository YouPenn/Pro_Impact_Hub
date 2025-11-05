package com.example.pro_impact_hub

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.app.ProgressDialog
import android.net.Uri
import com.bumptech.glide.Glide

class FeedbackFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private var userEmail: String? = null
    private lateinit var progressDialog: ProgressDialog

    private fun showNotification(eventName: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, 0)

        val builder = NotificationCompat.Builder(requireContext(), "feedbackChannelId")
            .setSmallIcon(R.drawable.notification_icon) // Replace with your notification icon
            .setContentTitle("Feedback Submitted")
            .setContentText("Your feedback for the event $eventName has been submitted successfully.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(requireContext())) {
            // notificationId is a unique int for each notification that you must define
            notify(1, builder.build())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = FirebaseFirestore.getInstance()

        // Initialize ProgressDialog
        progressDialog = ProgressDialog(context).apply {
            setMessage("Loading...")
            setCancelable(false)
        }

        // Retrieve user email from SharedPreferences
        userEmail = requireActivity().getSharedPreferences("user_session", AppCompatActivity.MODE_PRIVATE)
            .getString("userEmail", null)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_feedback, container, false)

        val tvEventTitle: TextView = view.findViewById(R.id.tvEventTitle)
        val etFeedbackInput: EditText = view.findViewById(R.id.etFeedbackInput)
        val btnSubmitFeedback: Button = view.findViewById(R.id.btnSubmitFeedback)
        val ivEventImage: ImageView = view.findViewById(R.id.ivEventImage)

        val eventName = arguments?.getString("eventName")
        tvEventTitle.text = eventName
        eventName?.let {
            fetchEventDetails(it) { event ->
                Glide.with(this).load(event.imageUrl).into(ivEventImage)

            }
        }

        btnSubmitFeedback.setOnClickListener {
            val feedback = etFeedbackInput.text.toString().trim()
            if (feedback.isNotEmpty()) {
                showLoadingIndicator()
                submitFeedback(eventName, feedback)
            } else {
                Toast.makeText(context, "Please enter your feedback", Toast.LENGTH_SHORT).show()
            }
        }

        val btnBack: ImageButton = view.findViewById(R.id.btnBack)
        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        return view
    }

    private fun fetchEventDetails(eventName: String, callback: (EventDetailsFragment.Event) -> Unit) {
        db.collection("Event").whereEqualTo("Name", eventName).limit(1).get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    documents.firstOrNull()?.let { document ->
                        val event = EventDetailsFragment.Event(
                            document.getString("url") ?: "",
                            document.getString("Name") ?: "",
                            document.getString("Desc") ?: "",
                            document.getString("Location") ?: "",
                            document.getString("Date") ?: "",
                            document.getString("Time") ?: ""
                        )
                        callback(event)
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error loading event details: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun showLoadingIndicator() {
        progressDialog.show()
    }

    private fun hideLoadingIndicator() {
        progressDialog.dismiss()
    }

    private fun submitFeedback(eventName: String?, feedback: String) {
        userEmail?.let { email ->
            val feedbackData = hashMapOf(
                "EventName" to eventName,
                "UserEmail" to email,
                "Feedback" to feedback
            )
            db.collection("Event_Feedback").add(feedbackData)
                .addOnSuccessListener {
                    hideLoadingIndicator()
                    Toast.makeText(context, "Feedback submitted successfully!", Toast.LENGTH_SHORT).show()
                    // Show notification
                    showNotification(eventName ?: "Event")
                    parentFragmentManager.popBackStack() // Go back to the previous fragment
                }
                .addOnFailureListener { e ->
                    hideLoadingIndicator()
                    Toast.makeText(context, "Error submitting feedback: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    companion object {
        fun newInstance(eventName: String): FeedbackFragment {
            val fragment = FeedbackFragment()
            val args = Bundle()
            args.putString("eventName", eventName)
            fragment.arguments = args
            return fragment
        }
    }
}