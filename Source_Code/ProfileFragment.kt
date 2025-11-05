package com.example.pro_impact_hub

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import de.hdodenhof.circleimageview.CircleImageView
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class ProfileFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var storageReference: FirebaseStorage
    private lateinit var imageviewAccountProfile: CircleImageView
    private lateinit var userEmail: String
    private var selectedImageUri: Uri? = null
    private var cameraImageUri: Uri? = null

    private lateinit var editUserName: EditText
    private lateinit var editAbout: EditText

    private var originalUserName: String? = null
    private var originalAbout: String? = null
    private var originalImageUrl: String? = null

    private lateinit var progressDialog: ProgressDialog

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val granted = permissions.entries.all { it.value == true }
            if (granted) {
                // All permissions have been granted
                chooseImageSource()
            } else {
                // Notify user that permissions are necessary
                Toast.makeText(context, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }

    private val getContent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = if (result.data?.data != null) result.data?.data else cameraImageUri
            uri?.let {
                imageviewAccountProfile.setImageURI(uri)
                selectedImageUri = uri
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        db = FirebaseFirestore.getInstance()
        storageReference = FirebaseStorage.getInstance()

        progressDialog = ProgressDialog(context).apply {
            setMessage("Loading...")
            setCancelable(false)
        }

        val sharedPreferences = activity?.getSharedPreferences("user_session", Context.MODE_PRIVATE)
        userEmail = sharedPreferences?.getString("userEmail", "No Email") ?: "No Email"

        editUserName = view.findViewById(R.id.editUserName)
        editAbout = view.findViewById(R.id.editAbout)
        imageviewAccountProfile = view.findViewById(R.id.imageview_account_profile)

        val btnSave = view.findViewById<Button>(R.id.btnSave)
        btnSave.setOnClickListener {
            showLoadingIndicator()
            updateProfile(editUserName.text.toString(), editAbout.text.toString(), selectedImageUri)
        }

        val btnUploadImage = view.findViewById<FloatingActionButton>(R.id.floatingActionButton)
        btnUploadImage.setOnClickListener {
            requestPermissions()
        }

        loadUserData()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ibBackObj: ImageButton = view.findViewById(R.id.btnBack)

        ibBackObj.setOnClickListener {
            val fragment = SettingFragment()
            val transaction = requireActivity().supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment_container, fragment)
            transaction.addToBackStack(null)
            transaction.commit()
        }
    }

    private fun showLoadingIndicator() {
        progressDialog.show()
    }

    private fun hideLoadingIndicator() {
        progressDialog.dismiss()
    }

    private fun requestPermissions() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA
                )
            )
        } else {
            chooseImageSource()
        }
    }

    private fun chooseImageSource() {
        val items = arrayOf("Take Photo", "Choose from Gallery")
        android.app.AlertDialog.Builder(context).setTitle("Select Image")
            .setItems(items) { dialog, which ->
                when (which) {
                    0 -> takePhoto()
                    1 -> pickImageFromGallery()
                }
            }.show()
    }

    private fun takePhoto() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.resolveActivity(requireActivity().packageManager)?.also {
            val photoFile: File? = try {
                createImageFile()
            } catch (ex: IOException) {
                Toast.makeText(context, "Photo file creation failed", Toast.LENGTH_SHORT).show()
                null
            }
            photoFile?.also {
                cameraImageUri = FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.provider",
                    it
                )
                intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri)
                getContent.launch(intent)
            }
        }
    }

    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        )
    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        getContent.launch(intent)
    }

    private fun updateProfile(name: String, about: String, imageUri: Uri?) {
        var message = ""
        var completedTasks = 0
        var totalTasks = 0
        var allTasksSuccessful = true

        if (imageUri != null && imageUri.toString() != originalImageUrl) {
            totalTasks++
            uploadImageToFirebaseStorage(imageUri) { success ->
                if (success) {
                    message += if (message.isEmpty()) "Profile image" else " and Profile image"
                } else {
                    allTasksSuccessful = false
                }
                checkCompletion(++completedTasks, totalTasks, message, allTasksSuccessful)
            }
        }

        if (name != originalUserName) {
            totalTasks++
            updateUserName(name) { success ->
                if (success) {
                    message += if (message.isEmpty()) "Username" else " and Username"
                } else {
                    allTasksSuccessful = false
                }
                checkCompletion(++completedTasks, totalTasks, message, allTasksSuccessful)
            }
        }

        if (about != originalAbout) {
            totalTasks++
            updateAbout(about) { success ->
                if (success) {
                    message += if (message.isEmpty()) "About me" else " and About me"
                } else {
                    allTasksSuccessful = false
                }
                checkCompletion(++completedTasks, totalTasks, message, allTasksSuccessful)
            }
        }

        if (totalTasks == 0) {
            hideLoadingIndicator()
            Toast.makeText(requireContext(), "No changes detected", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUserName(name: String, callback: (Boolean) -> Unit) {
        val userRef = db.collection("Student").document(userEmail)
        userRef.update("Name", name).addOnSuccessListener {
            callback(true)
        }.addOnFailureListener {
            callback(false)
        }
    }

    private fun updateAbout(about: String, callback: (Boolean) -> Unit) {
        val userRef = db.collection("Student").document(userEmail)
        userRef.update("About", about).addOnSuccessListener {
            callback(true)
        }.addOnFailureListener {
            callback(false)
        }
    }

    private fun checkCompletion(completedTasks: Int, totalTasks: Int, message: String, allTasksSuccessful: Boolean) {
        if (completedTasks == totalTasks) {
            hideLoadingIndicator()
            if (allTasksSuccessful) {
                Toast.makeText(requireContext(), "$message updated successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Some updates failed. Please try again.", Toast.LENGTH_SHORT).show()
            }
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, SettingFragment())
                .commit()
        }
    }

    private fun uploadImageToFirebaseStorage(imageUri: Uri, callback: (Boolean) -> Unit) {
        val imageRef = storageReference.reference.child("images/${userEmail}_${System.currentTimeMillis()}")
        imageRef.putFile(imageUri).addOnSuccessListener {
            imageRef.downloadUrl.addOnSuccessListener { uri ->
                val userRef = db.collection("Student").document(userEmail)
                userRef.update("img", uri.toString())
                    .addOnSuccessListener { callback(true) }
                    .addOnFailureListener { callback(false) }
            }.addOnFailureListener {
                callback(false)
            }
        }.addOnFailureListener {
            callback(false)
        }
    }

    private fun loadUserData() {
        showLoadingIndicator()
        db.collection("Student").document(userEmail)
            .get()
            .addOnSuccessListener { document ->
                hideLoadingIndicator()
                if (document.exists()) {
                    originalUserName = document.getString("Name") ?: ""
                    originalAbout = document.getString("About") ?: ""
                    originalImageUrl = document.getString("img")

                    editUserName.setText(originalUserName)
                    editAbout.setText(originalAbout)

                    if (originalImageUrl != null && originalImageUrl!!.isNotEmpty()) {
                        Glide.with(this@ProfileFragment).load(originalImageUrl).into(imageviewAccountProfile)
                    }
                } else {
                    Toast.makeText(context, "No user data found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                hideLoadingIndicator()
                Toast.makeText(context, "Failed to load user data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
