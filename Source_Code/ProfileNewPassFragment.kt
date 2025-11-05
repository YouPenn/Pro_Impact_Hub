package com.example.pro_impact_hub

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.*
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.firebase.firestore.FirebaseFirestore

class ProfileNewPassFragment : Fragment() {

    private lateinit var passwordEditText: EditText
    private lateinit var toggleButton: ToggleButton
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var toggleButton2: ToggleButton
    private lateinit var btnUpdateObj: Button
    private lateinit var userEmail: String
    private lateinit var db: FirebaseFirestore
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Retrieve the user email from SharedPreferences
        val sharedPreferences = activity?.getSharedPreferences("user_session", Context.MODE_PRIVATE)
        userEmail = sharedPreferences?.getString("userEmail", "No Email") ?: "No Email"
        db = FirebaseFirestore.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile_new_pass, container, false)

        passwordEditText = view.findViewById(R.id.etNewPassword)
        toggleButton = view.findViewById(R.id.toggleButton)
        confirmPasswordEditText = view.findViewById(R.id.confirm_password)
        toggleButton2 = view.findViewById(R.id.toggleButton2)
        btnUpdateObj = view.findViewById(R.id.btnUpdate)

        // Initialize ProgressDialog
        progressDialog = ProgressDialog(context).apply {
            setMessage("Loading...")
            setCancelable(false)
        }

        toggleButton.setOnCheckedChangeListener { _, isChecked ->
            togglePasswordVisibility(isChecked, passwordEditText, toggleButton)
        }

        toggleButton2.setOnCheckedChangeListener { _, isChecked ->
            togglePasswordVisibility(isChecked, confirmPasswordEditText, toggleButton2)
        }

        btnUpdateObj.setOnClickListener {
            val newPassword = passwordEditText.text.toString().trim()
            val confirmPassword = confirmPasswordEditText.text.toString().trim()

            if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in both fields", Toast.LENGTH_SHORT).show()
            } else if (newPassword != confirmPassword) {
                Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show()
            } else if (!isPasswordValid(newPassword)) {
                Toast.makeText(requireContext(), "Password must be at least 12 characters long and include uppercase letters, lowercase letters, numbers, and symbols", Toast.LENGTH_LONG).show()
            } else {
                showConfirmationDialog(newPassword)
            }
        }

        return view
    }

    private fun togglePasswordVisibility(isChecked: Boolean, editText: EditText, toggleButton: ToggleButton) {
        if (isChecked) {
            editText.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            toggleButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.icon_eye_on, 0)
        } else {
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            toggleButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.icon_eye_off, 0)
        }
        editText.setSelection(editText.text.length)
    }

    private fun isPasswordValid(password: String): Boolean {
        val passwordPattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@\$!%*?&])[A-Za-z\\d@\$!%*?&]{12,}$"
        return password.matches(passwordPattern.toRegex())
    }

    private fun showConfirmationDialog(newPassword: String) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Confirm Password Renewal")
        builder.setMessage("Are you sure you want to renew your password?")
        builder.setPositiveButton("Yes") { _, _ ->
            showLoadingIndicator()
            updatePassword(newPassword)
        }
        builder.setNegativeButton("No") { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    private fun updatePassword(newPassword: String) {
        val encryptedPassword = Encrypt.encryptPassword(newPassword) // Encrypt the new password
        db.collection("Student").document(userEmail)
            .update("Password", encryptedPassword)
            .addOnSuccessListener {
                hideLoadingIndicator()
                Toast.makeText(requireContext(), "Password updated successfully", Toast.LENGTH_SHORT).show()
                clearSharedPreferencesAndLogout()
            }
            .addOnFailureListener { e ->
                hideLoadingIndicator()
                Toast.makeText(requireContext(), "Failed to update password: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun clearSharedPreferencesAndLogout() {
        val sharedPreferences = activity?.getSharedPreferences("user_session", Context.MODE_PRIVATE)
        sharedPreferences?.edit()?.clear()?.apply()

        val intent = Intent(requireActivity(), UserLogin::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    private fun showLoadingIndicator() {
        progressDialog.show()
    }

    private fun hideLoadingIndicator() {
        progressDialog.dismiss()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Find the button by its ID
        val ibBackObj: ImageButton = view.findViewById(R.id.btnBack)

        ibBackObj.setOnClickListener {
            val fragment = SettingFragment()
            val transaction = requireActivity().supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment_container, fragment)
            transaction.addToBackStack(null)
            transaction.commit()
        }
    }
}
