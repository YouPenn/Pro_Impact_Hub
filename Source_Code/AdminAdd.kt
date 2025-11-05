package com.example.pro_impact_hub

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class AdminAdd : AppCompatActivity() {

    private var etNameObj: EditText? = null
    private var etDateObj: EditText? = null
    private var etTimeObj: EditText? = null
    private var etLocationObj: EditText? = null
    private var etDescriptionObj: EditText? = null
    private var btnAddObj: Button? = null

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
    private val REQUEST_IMAGE_CAPTURE = 2
    private val REQUEST_PERMISSION = 1001

    private lateinit var currentPhotoPath: String

    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_add)

        etNameObj = findViewById(R.id.etName)
        etDateObj = findViewById(R.id.etDate)
        etTimeObj = findViewById(R.id.etTime)
        etLocationObj = findViewById(R.id.etLocation)
        etDescriptionObj = findViewById(R.id.etDescription)
        btnAddObj = findViewById(R.id.btnAdd)

        FirebaseApp.initializeApp(this)
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

        val btnUploadImage = findViewById<Button>(R.id.btnUploadImage)
        btnUploadImage.setOnClickListener {
            openGallery()
        }

        val btnCamera = findViewById<Button>(R.id.btnCamera)
        btnCamera.setOnClickListener {
            checkPermissionsAndOpenCamera()
        }

        btnAddObj?.setOnClickListener {
            showLoadingIndicator()
            if (::selectedImageUri.isInitialized) {
                uploadImageToFirebaseStorage()
            } else {
                addNewEvent(null)
            }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_IMAGE_PICK)
    }

    private fun checkPermissionsAndOpenCamera() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            requestPermissions(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ),
                REQUEST_PERMISSION
            )
        } else {
            dispatchTakePictureIntent()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            dispatchTakePictureIntent()
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    null
                }
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        "${applicationContext.packageName}.provider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.data!!
            findViewById<ImageView>(R.id.imagePreview).setImageURI(selectedImageUri)
        } else if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            val file = File(currentPhotoPath)
            selectedImageUri = Uri.fromFile(file)
            findViewById<ImageView>(R.id.imagePreview).setImageURI(selectedImageUri)
        }
    }

    private fun uploadImageToFirebaseStorage() {
        val imageName = "event_image_${UUID.randomUUID()}"
        val imageRef = storageReference.child("images/$imageName")

        imageRef.putFile(selectedImageUri)
            .addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    addNewEvent(downloadUri.toString())
                }
            }
            .addOnFailureListener { e ->
                hideLoadingIndicator()
                Toast.makeText(this, "Image upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

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

    private fun addNewEvent(imageUrl: String?) {
        val name: String = etNameObj?.text.toString()
        val date: String = etDateObj?.text.toString()
        val time: String = etTimeObj?.text.toString()
        val location: String = etLocationObj?.text.toString()
        val description: String = etDescriptionObj?.text.toString()

        if (name.isBlank() || date.isBlank() || time.isBlank() || location.isBlank() || description.isBlank()) {
            hideLoadingIndicator()
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isValidDate(date)) {
            hideLoadingIndicator()
            Toast.makeText(this, "Invalid date format. Use DD-MM-YYYY", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isValidTime(time)) {
            hideLoadingIndicator()
            Toast.makeText(this, "Invalid time format. Use HH:MM", Toast.LENGTH_SHORT).show()
            return
        }

        checkEventExistsAndAdd(name, date, time, location, description, imageUrl)
    }

    private fun checkEventExistsAndAdd(name: String, date: String, time: String, location: String, description: String, imageUrl: String?) {
        db?.collection("Event")?.document(name)?.get()
            ?.addOnSuccessListener { document ->
                if (document.exists()) {
                    hideLoadingIndicator()
                    Toast.makeText(this, "Event with this name already exists", Toast.LENGTH_SHORT).show()
                } else {
                    val newEvent: MutableMap<String, Any> = HashMap()
                    newEvent[NAME_KEY] = name
                    newEvent[DATE_KEY] = date
                    newEvent[TIME_KEY] = time
                    newEvent[LOCATION_KEY] = location
                    newEvent[DESCRIPTION_KEY] = description
                    imageUrl?.let { newEvent[URL_KEY] = it }

                    db?.collection("Event")?.document(name)?.set(newEvent)
                        ?.addOnSuccessListener {
                            hideLoadingIndicator()
                            Toast.makeText(this, "Event added successfully!", Toast.LENGTH_SHORT).show()
                            clearFields()
                        }
                        ?.addOnFailureListener { e ->
                            hideLoadingIndicator()
                            Toast.makeText(this, "Failed to add event: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            ?.addOnFailureListener { e ->
                hideLoadingIndicator()
                Toast.makeText(this, "Failed to check event: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun clearFields() {
        etNameObj?.text?.clear()
        etDateObj?.text?.clear()
        etTimeObj?.text?.clear()
        etLocationObj?.text?.clear()
        etDescriptionObj?.text?.clear()
        findViewById<ImageView>(R.id.imagePreview).setImageURI(null)
    }

    private fun showLoadingIndicator() {
        progressDialog.show()
    }

    private fun hideLoadingIndicator() {
        progressDialog.dismiss()
    }
}
