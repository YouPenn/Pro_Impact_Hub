package com.example.pro_impact_hub

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.text.SimpleDateFormat
import java.util.*

class AdminEdit : AppCompatActivity() {

    private var editNameObj: EditText? = null
    private var editDateObj: EditText? = null
    private var editTimeObj: EditText? = null
    private var editLocationObj: EditText? = null
    private var editDescriptionObj: EditText? = null
    private var btnUpdateObj: Button? = null
    private var btnEditSearch: Button? = null
    private var imagePreviewEdit: ImageView? = null

    private val NAME_KEY = "Name"
    private val DATE_KEY = "Date"
    private val TIME_KEY = "Time"
    private val LOCATION_KEY = "Location"
    private val DESCRIPTION_KEY = "Desc"
    private val URL_KEY = "url"

    private var db: FirebaseFirestore? = null
    private lateinit var storageReference: StorageReference
    private lateinit var selectedImageUri: Uri
    private val REQUEST_IMAGE_PICK = 1

    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_edit)

        editNameObj = findViewById(R.id.editName)
        editDateObj = findViewById(R.id.editDate)
        editTimeObj = findViewById(R.id.editTime)
        editLocationObj = findViewById(R.id.editLocation)
        editDescriptionObj = findViewById(R.id.editDescription)
        btnUpdateObj = findViewById(R.id.btnUpdate)
        btnEditSearch = findViewById(R.id.btnEditsearch)
        imagePreviewEdit = findViewById(R.id.imagePreviewEdit)

        db = FirebaseFirestore.getInstance()
        storageReference = FirebaseStorage.getInstance().reference

        // Initialize ProgressDialog
        progressDialog = ProgressDialog(this).apply {
            setMessage("Loading...")
            setCancelable(false)
        }

        val imgBackObj = findViewById<ImageButton>(R.id.imgBack)
        imgBackObj.setOnClickListener {
            val intent = Intent(this, AdminHome::class.java)
            startActivity(intent)
        }

        val btnEditImage = findViewById<Button>(R.id.btnEditImage)
        btnEditImage.setOnClickListener {
            openGallery()
        }

        btnEditSearch?.setOnClickListener {
            showLoadingIndicator()
            searchEvent()
        }

        btnUpdateObj?.setOnClickListener {
            showLoadingIndicator()
            checkEventExistsAndProceed()
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_IMAGE_PICK)
    }

    private fun searchEvent() {
        try {
            val name = editNameObj?.text.toString()
            if (name.isNotBlank()) {
                db?.collection("Event")?.document(name)?.get()
                    ?.addOnSuccessListener { document ->
                        hideLoadingIndicator()
                        if (document != null && document.exists()) {
                            val date = document.getString("Date")
                            val time = document.getString("Time")
                            val location = document.getString("Location")
                            val description = document.getString("Desc")
                            val imageUrl = document.getString("url")

                            // Log retrieved values
                            Log.d("AdminEdit", "Date: $date")
                            Log.d("AdminEdit", "Time: $time")
                            Log.d("AdminEdit", "Location: $location")
                            Log.d("AdminEdit", "Description: $description")
                            Log.d("AdminEdit", "Image URL: $imageUrl")

                            // Update UI elements with retrieved values
                            editDateObj?.setText(date ?: "")
                            editTimeObj?.setText(time ?: "")
                            editLocationObj?.setText(location ?: "")
                            editDescriptionObj?.setText(description ?: "")

                            if (!imageUrl.isNullOrEmpty()) {
                                Glide.with(this).load(imageUrl).into(imagePreviewEdit!!)
                            } else {
                                imagePreviewEdit?.setImageResource(R.drawable.icon_default_image)
                            }
                            Toast.makeText(this, "Event found!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "No such event exists!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    ?.addOnFailureListener { e ->
                        hideLoadingIndicator()
                        Toast.makeText(this, "Failed to retrieve event: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                hideLoadingIndicator()
                Toast.makeText(this, "Please enter the event name", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            hideLoadingIndicator()
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.data!!
            findViewById<ImageView>(R.id.imagePreviewEdit).setImageURI(selectedImageUri)
        }
    }

    private fun checkEventExistsAndProceed() {
        val name: String = editNameObj?.text.toString()

        if (name.isNotBlank()) {
            db?.collection("Event")?.document(name)?.get()
                ?.addOnSuccessListener { document ->
                    if (document.exists()) {
                        if (::selectedImageUri.isInitialized) {
                            uploadImageToFirebaseStorage()
                        } else {
                            updateData(null)
                        }
                    } else {
                        hideLoadingIndicator()
                        Toast.makeText(this, "Event does not exist", Toast.LENGTH_SHORT).show()
                    }
                }
                ?.addOnFailureListener { e ->
                    hideLoadingIndicator()
                    Toast.makeText(this, "Failed to check event: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            hideLoadingIndicator()
            Toast.makeText(this, "Name field cannot be empty", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uploadImageToFirebaseStorage() {
        val imageName = "event_image_${UUID.randomUUID()}"
        val imageRef = storageReference.child("images/$imageName")

        imageRef.putFile(selectedImageUri)
            .addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    updateData(downloadUri.toString())
                }
            }
            .addOnFailureListener { e ->
                hideLoadingIndicator()
                Toast.makeText(this, "Image upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // New validation methods for date and time
    private fun isValidDate(date: String): Boolean {
        return try {
            val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.US)
            sdf.isLenient = false
            sdf.parse(date)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun isValidTime(time: String): Boolean {
        return try {
            val sdf = SimpleDateFormat("HH:mm", Locale.US)
            sdf.isLenient = false
            sdf.parse(time)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun updateData(imageUrl: String?) {
        val name: String = editNameObj?.text.toString()
        val date: String = editDateObj?.text.toString()
        val time: String = editTimeObj?.text.toString()
        val location: String = editLocationObj?.text.toString()
        val description: String = editDescriptionObj?.text.toString()

        if (name.isNotBlank() && date.isNotBlank() && time.isNotBlank() && location.isNotBlank() && description.isNotBlank()) {
            if (isValidDate(date) && isValidTime(time)) {
                val updatedEventData: MutableMap<String, Any> = HashMap()
                updatedEventData[NAME_KEY] = name
                updatedEventData[DATE_KEY] = date
                updatedEventData[TIME_KEY] = time
                updatedEventData[LOCATION_KEY] = location
                updatedEventData[DESCRIPTION_KEY] = description
                imageUrl?.let { updatedEventData[URL_KEY] = it }

                val eventRef = db?.collection("Event")?.document(name)
                eventRef?.update(updatedEventData)
                    ?.addOnSuccessListener {
                        hideLoadingIndicator()
                        Toast.makeText(this, "Event updated successfully!", Toast.LENGTH_SHORT).show()
                        finish() // Finish the activity after successful update
                    }
                    ?.addOnFailureListener { e ->
                        hideLoadingIndicator()
                        Toast.makeText(this, "Failed to update event: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                hideLoadingIndicator()
                Toast.makeText(this, "Please enter a valid date and time", Toast.LENGTH_SHORT).show()
            }
        } else {
            hideLoadingIndicator()
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLoadingIndicator() {
        progressDialog.show()
    }

    private fun hideLoadingIndicator() {
        progressDialog.dismiss()
    }
}
