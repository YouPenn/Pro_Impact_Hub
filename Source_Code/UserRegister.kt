package com.example.pro_impact_hub

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class UserRegister : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_rigister)

        db = FirebaseFirestore.getInstance()

        // Initialize ProgressDialog
        progressDialog = ProgressDialog(this).apply {
            setMessage("Loading...")
            setCancelable(false)
        }

        val spinner = findViewById<Spinner>(R.id.spSecurityQues)
        val questions = resources.getStringArray(R.array.question).toList()
        val adapter = CustomSpinnerAdapter(this, android.R.layout.simple_spinner_item, questions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        val passwordEditText = findViewById<EditText>(R.id.etPassword)
        val toggleButton = findViewById<ToggleButton>(R.id.toggleButton)
        val confirmPasswordEditText = findViewById<EditText>(R.id.confirm_password)
        val toggleButton2 = findViewById<ToggleButton>(R.id.toggleButton2)
        val btnRegisterObj = findViewById<Button>(R.id.btnRegister)
        val btnLoginObj = findViewById<Button>(R.id.btnLogin)

        val emailEditText = findViewById<EditText>(R.id.etEmail)
        val securityQuesSpin = findViewById<Spinner>(R.id.spSecurityQues)
        val secAnsEditText = findViewById<EditText>(R.id.secAns)

        toggleButton.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Show password
                passwordEditText.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                toggleButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.icon_eye_on, 0)
            } else {
                // Hide password
                passwordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                toggleButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.icon_eye_off, 0)
            }
            // Move cursor to the end of the text
            passwordEditText.setSelection(passwordEditText.text.length)
        }

        toggleButton2.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Show password
                confirmPasswordEditText.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                toggleButton2.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.icon_eye_on, 0)
            } else {
                // Hide password
                confirmPasswordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                toggleButton2.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.icon_eye_off, 0)
            }
            // Move cursor to the end of the text
            confirmPasswordEditText.setSelection(confirmPasswordEditText.text.length)
        }

        btnRegisterObj.setOnClickListener {
            showLoadingIndicator()
            registerUser(
                emailEditText.text.toString(),
                passwordEditText.text.toString(),
                confirmPasswordEditText.text.toString(),
                securityQuesSpin.selectedItem.toString(),
                secAnsEditText.text.toString()
            )
        }

        btnLoginObj.setOnClickListener {
            val intent = Intent(this, UserLogin::class.java)
            startActivity(intent)
        }
    }

    private fun showLoadingIndicator() {
        progressDialog.show()
    }

    private fun hideLoadingIndicator() {
        progressDialog.dismiss()
    }

    private fun registerUser(email: String, password: String, confirmPassword: String, securityQuestion: String, securityAnswer: String) {
        var errorMessage = ""

        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || securityAnswer.isEmpty()) {
            errorMessage += "Please fill in all fields. "
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            errorMessage += "Please enter a valid email address. "
        }

        if (password.length < 12 || !password.matches(".*[A-Z].*".toRegex()) || !password.matches(".*[a-z].*".toRegex()) || !password.matches(".*\\d.*".toRegex()) || !password.matches(".*[!@#\$%^&*()].*".toRegex())) {
            errorMessage += "Password must be at least 12 characters and include uppercase, lowercase, number, and symbol. "
        }

        if (password != confirmPassword) {
            errorMessage += "Passwords do not match. "
        }

        if (securityQuestion == "Select Security Question") {
            errorMessage += "Please select a security question. "
        }

        if (errorMessage.isNotEmpty()) {
            hideLoadingIndicator()
            Toast.makeText(this, errorMessage.trim(), Toast.LENGTH_LONG).show()
            return
        }

        // Encrypt password before storing
        val encryptedPassword = Encrypt.encryptPassword(password)

        val user = hashMapOf(
            "Email" to email,
            "Password" to encryptedPassword,
            "SecurityQuestion" to securityQuestion,
            "SecurityAnswer" to securityAnswer,
            "Name" to "",
            "About" to "",
            "img" to ""
        )

        db.collection("Student")
            .document(email)
            .set(user)
            .addOnSuccessListener {
                hideLoadingIndicator()
                Toast.makeText(this, "User registered successfully", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, UserLogin::class.java)
                startActivity(intent)
            }
            .addOnFailureListener { e ->
                hideLoadingIndicator()
                Toast.makeText(this, "Error registering user: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
