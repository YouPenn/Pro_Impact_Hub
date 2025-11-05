package com.example.pro_impact_hub

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore

class HomeFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var scrollView: ScrollView
    private lateinit var btnBackToTop: ImageButton
    private lateinit var shakeDetector: ShakeDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = FirebaseFirestore.getInstance() // Initialize Firestore
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        scrollView = view.findViewById(R.id.eventview)
        btnBackToTop = view.findViewById(R.id.btnBackToTop)
        val linearLayoutContainer = view.findViewById<LinearLayout>(R.id.eventsLinearLayout)

        // Fetch events from Firestore and populate the UI
        fetchEvents { events ->
            for (event in events) {
                val subLayout = inflater.inflate(R.layout.home_event, null) as CardView
                populateEventCard(subLayout, event)
                linearLayoutContainer.addView(subLayout)
            }
        }

        val searchEditText = view.findViewById<EditText>(R.id.searchEditText)
        val searchButton = view.findViewById<Button>(R.id.searchButton)

        // Clear focus from searchEditText when the view is created
        searchEditText.clearFocus()

        searchButton.setOnClickListener {
            val eventNameToSearch = searchEditText.text.toString().trim()
            searchEvent(eventNameToSearch, linearLayoutContainer)
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

        // Set OnClickListener for btnBackToTop
        btnBackToTop.setOnClickListener {
            scrollView.smoothScrollTo(0, 0)
        }

        // Initialize and start the ShakeDetector
        shakeDetector = ShakeDetector(requireContext()) {
            scrollView.smoothScrollTo(0, 0)
        }
        shakeDetector.start()

        return view
    }

    override fun onResume() {
        super.onResume()
        shakeDetector.start()
    }

    override fun onPause() {
        super.onPause()
        shakeDetector.stop()
    }

    private fun fetchEvents(callback: (List<Event>) -> Unit) {
        db.collection("Event").get().addOnSuccessListener { result ->
            val events = result.mapNotNull { doc ->
                val imageUrl = doc.getString("url")
                val name = doc.getString("Name")
                val description = doc.getString("Desc")
                val location = doc.getString("Location")
                val date = doc.getString("Date")
                val time = doc.getString("Time")
                if (imageUrl != null && name != null && date != null && time != null && description != null && location != null) {
                    Event(imageUrl, name, description, location, date, time)
                } else null
            }
            callback(events)
        }.addOnFailureListener {
            Toast.makeText(context, "Error getting documents: ${it.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun populateEventCard(subLayout: CardView, event: Event) {
        val eventImageView = subLayout.findViewById<ImageView>(R.id.eventImageView)
        val eventNameTextView = subLayout.findViewById<TextView>(R.id.eventNameTextView)
        val eventDateTextView = subLayout.findViewById<TextView>(R.id.eventDateTextView)
        val viewEventButton = subLayout.findViewById<Button>(R.id.button)

        Glide.with(this).load(event.imageUrl).into(eventImageView)
        eventNameTextView.text = event.name
        eventDateTextView.text = "${event.date} at ${event.time}"

        viewEventButton.setOnClickListener {
            navigateToEventDetails(event.name)
        }
    }

    private fun searchEvent(eventName: String, container: LinearLayout) {
        for (i in 0 until container.childCount) {
            val cardView = container.getChildAt(i) as CardView
            val eventNameTextView = cardView.findViewById<TextView>(R.id.eventNameTextView)
            if (eventNameTextView.text.toString() == eventName) {
                navigateToEventDetails(eventName)
                return
            }
        }
        Toast.makeText(requireContext(), "No Event Found", Toast.LENGTH_SHORT).show()
    }

    private fun navigateToEventDetails(eventName: String) {
        val bundle = Bundle()
        bundle.putString("eventName", eventName)
        val eventDetailsFragment = EventDetailsFragment()
        eventDetailsFragment.arguments = bundle
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, eventDetailsFragment)
            .addToBackStack(null)
            .commit()
    }

    data class Event(
        val imageUrl: String,
        val name: String,
        val description: String,
        val location: String,
        val date: String,
        val time: String
    )

    companion object {
        @JvmStatic
        fun newInstance(userEmail: String) = HomeFragment().apply {
            arguments = Bundle().apply {
                putString("userEmail", userEmail)
            }
        }
    }
}
