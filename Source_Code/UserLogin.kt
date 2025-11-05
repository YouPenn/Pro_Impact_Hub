package com.example.pro_impact_hub

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.text.TextUtils
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class UserLogin : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9001

    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_login)

        db = FirebaseFirestore.getInstance() // Initialize Firebase Firestore
        auth = FirebaseAuth.getInstance() // Initialize Firebase Auth

        // Initialize ProgressDialog
        progressDialog = ProgressDialog(this).apply {
            setMessage("Loading...")
            setCancelable(false)
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val btnGoogleIn = findViewById<Button>(R.id.btnGoogle)
        btnGoogleIn.setOnClickListener {
            showLoadingIndicator()
            signInWithGoogle()
        }

        val emailEditText = findViewById<EditText>(R.id.etEmail)
        val passwordEditText = findViewById<EditText>(R.id.etPassword)

        emailEditText.setSingleLine(true)
        emailEditText.setEllipsize(TextUtils.TruncateAt.END)

        val btnSignupObj = findViewById<Button>(R.id.btnSignup)
        btnSignupObj.setOnClickListener {
            val intent = Intent(this, UserRegister::class.java)
            startActivity(intent)
        }

        val imgBackObj = findViewById<Button>(R.id.btnBack)
        imgBackObj.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        val btnLoginObj = findViewById<Button>(R.id.btnLogin)
        btnLoginObj.setOnClickListener {
            val userEmail = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            showLoadingIndicator()
            loginUser(userEmail, password)
        }

        // Set OnEditorActionListener on passwordEditText
        passwordEditText.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO || event?.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER) {
                btnLoginObj.performClick()
                true
            } else {
                false
            }
        }

        val toggleButton = findViewById<ToggleButton>(R.id.toggleButton)
        toggleButton.setOnCheckedChangeListener { _, isChecked ->
            passwordEditText.inputType = if (isChecked) {
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            passwordEditText.setSelection(passwordEditText.text.length)

            // Update the icon based on the isChecked state
            val iconResId = if (isChecked) {
                R.drawable.icon_eye_on
            } else {
                R.drawable.icon_eye_off
            }
            toggleButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, iconResId, 0)
        }

        val btnForgotPassObj = findViewById<Button>(R.id.btnForgotPass)
        btnForgotPassObj.setOnClickListener {
            val intent = Intent(this, UserForgotPass::class.java)
            startActivity(intent)
        }
    }

    private fun showLoadingIndicator() {
        progressDialog.show()
    }

    private fun hideLoadingIndicator() {
        progressDialog.dismiss()
    }

    private fun loginUser(userEmail: String, password: String) {
        if (userEmail.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            hideLoadingIndicator()
            return
        }

        db.collection("Student").document(userEmail)
            .get()
            .addOnSuccessListener { document ->
                hideLoadingIndicator()
                if (document.exists()) {
                    val storedPassword = document.getString("Password")
                    val encryptedPassword = Encrypt.encryptPassword(password)

                    if (storedPassword == encryptedPassword) {
                        val userName = document.getString("Name") ?: "No Name"

                        val sharedPreferences = getSharedPreferences("user_session", MODE_PRIVATE)
                        with(sharedPreferences.edit()) {
                            putString("userEmail", userEmail)
                            putString("userName", userName)
                            apply()
                        }

                        val intent = Intent(this, UserHome::class.java).apply {
                            putExtra("userEmail", userEmail)
                            putExtra("userName", userName)
                        }
                        startActivity(intent)
                        finish() // Close the login activity
                    } else {
                        Toast.makeText(this, "Invalid email or password", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Invalid email or password", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                hideLoadingIndicator()
                Toast.makeText(this, "Login failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun signInWithGoogle() {
        googleSignInClient.signOut().addOnCompleteListener(this) {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                showLoadingIndicator()
                firebaseAuthWithGoogle(account?.idToken!!)
            } catch (e: ApiException) {
                hideLoadingIndicator()
                Toast.makeText(this, "Google sign-in failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                hideLoadingIndicator()
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val userEmail = user?.email
                    val userName = user?.displayName

                    // Save user info to user session
                    val sharedPreferences = getSharedPreferences("user_session", MODE_PRIVATE)
                    with(sharedPreferences.edit()) {
                        putString("userEmail", userEmail)
                        putString("userName", userName)
                        apply()
                    }

                    // Navigate to UserHome activity
                    val intent = Intent(this, UserHome::class.java).apply {
                        putExtra("userEmail", userEmail)
                        putExtra("userName", userName)
                    }
                    startActivity(intent)
                    finish() // Close the login activity
                } else {
                    Toast.makeText(this, "Authentication Failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onBackPressed() {
        // Do nothing to disable the back button
    }
}
