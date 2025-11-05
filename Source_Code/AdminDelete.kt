package com.example.pro_impact_hub

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore

class AdminDelete : AppCompatActivity() {

    private var delNameObj: EditText? = null
    private var delDateObj: TextView? = null
    private var delTimeObj: TextView? = null
    private var delLocationObj: TextView? = null
    private var delDescriptionObj: TextView? = null
    private var btnSearchObj: Button? = null
    private var btnDeleteObj: Button? = null
    private var imagePreviewDelete: ImageView? = null

    private var db: FirebaseFirestore? = null

    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_delete)

        FirebaseFirestore.setLoggingEnabled(true)

        delNameObj = findViewById(R.id.delName)
        delDateObj = findViewById(R.id.delDate)
        delTimeObj = findViewById(R.id.delTime)
        delLocationObj = findViewById(R.id.delLocation)
        delDescriptionObj = findViewById(R.id.delDescription)
        btnSearchObj = findViewById(R.id.btnSearch)
        btnDeleteObj = findViewById(R.id.btnDelete)
        imagePreviewDelete = findViewById(R.id.imagePreviewdelete)

        db = FirebaseFirestore.getInstance()

        // Initialize ProgressDialog
        progressDialog = ProgressDialog(this).apply {
            setMessage("Loading...")
            setCancelable(false)
        }

        btnSearchObj?.setOnClickListener {
            showLoadingIndicator()
            searchData()
        }

        btnDeleteObj?.setOnClickListener {
            confirmDeletion()
        }

        val imgBackObj = findViewById<ImageButton>(R.id.imgBack)
        imgBackObj.setOnClickListener {
            val intent = Intent(this, AdminHome::class.java)
            startActivity(intent)
        }
    }

    private fun showLoadingIndicator() {
        progressDialog.show()
    }

    private fun hideLoadingIndicator() {
        progressDialog.dismiss()
    }

    private fun searchData() {
        try {
            val name = delNameObj?.text.toString()
            if (name.isNotBlank()) {
                db?.collection("Event")?.document(name)?.get()
                    ?.addOnSuccessListener { document ->
                        hideLoadingIndicator()
                        if (document.exists()) {
                            delDateObj?.text = document.getString("Date")
                            delTimeObj?.text = document.getString("Time")
                            delLocationObj?.text = document.getString("Location")
                            delDescriptionObj?.text = document.getString("Desc")
                            val imageUrl = document.getString("url")
                            if (!imageUrl.isNullOrEmpty()) {
                                Glide.with(this).load(imageUrl).into(imagePreviewDelete!!)
                            } else {
                                imagePreviewDelete?.setImageResource(R.drawable.icon_default_image)
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

    private fun confirmDeletion() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Confirm Deletion")
        builder.setMessage("Are you sure you want to delete this event?")
        builder.setPositiveButton("Yes") { dialog, which ->
            showLoadingIndicator()
            deleteData()
        }
        builder.setNegativeButton("No") { dialog, which ->
            dialog.dismiss()
        }
        builder.show()
    }

    private fun deleteData() {
        val name = delNameObj?.text.toString()
        if (name.isNotBlank()) {
            db?.collection("Event")?.document(name)?.delete()
                ?.addOnSuccessListener {
                    hideLoadingIndicator()
                    clearFields()
                    Toast.makeText(this, "Event deleted successfully!", Toast.LENGTH_SHORT).show()
                }
                ?.addOnFailureListener { e ->
                    hideLoadingIndicator()
                    Toast.makeText(this, "Failed to delete event: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            hideLoadingIndicator()
            Toast.makeText(this, "Please enter the event name", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearFields() {
        delNameObj?.text?.clear()
        delDateObj?.text = ""
        delTimeObj?.text = ""
        delLocationObj?.text = ""
        delDescriptionObj?.text = ""
        imagePreviewDelete?.setImageResource(R.drawable.icon_default_image)
    }
}
