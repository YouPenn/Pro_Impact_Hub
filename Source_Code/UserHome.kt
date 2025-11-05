package com.example.pro_impact_hub

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView

class UserHome : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private var userEmail: String? = null
    private lateinit var navigationView: NavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_home)

        // Retrieve the user email from intent and store it as a member variable
        userEmail = intent.getStringExtra("userEmail")

        // Setup toolbar and navigation drawer
        setupToolbarAndDrawer()

        // Load HomeFragment with the user's email
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, HomeFragment.newInstance(userEmail ?: ""))
                .commit()
            navigationView.setCheckedItem(R.id.nav_home)
        }

    }

    private fun setupToolbarAndDrawer() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)
        val toggle = ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open_nav, R.string.close_nav)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, HomeFragment.newInstance(userEmail ?: ""))
                    .commit()
                setCheckedItem(R.id.nav_home)
            }
            R.id.nav_joined_event -> {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, JoinedEventFragment())
                    .commit()
                setCheckedItem(R.id.nav_joined_event)
            }
            R.id.nav_settings -> {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, SettingFragment())
                    .commit()
                setCheckedItem(R.id.nav_settings)
            }
            R.id.nav_about -> {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, AboutFragment())
                    .commit()
                setCheckedItem(R.id.nav_about)
            }
            R.id.nav_logout -> {
                // Clear shared preferences
                val sharedPreferences = getSharedPreferences("user_session", MODE_PRIVATE)
                with(sharedPreferences.edit()) {
                    clear()
                    apply()
                }
                Toast.makeText(this, "Logout!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, UserLogin::class.java)
                startActivity(intent)
                finish()
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun setCheckedItem(itemId: Int) {
        navigationView.setCheckedItem(itemId)
        navigationView.invalidate()  // Force the navigation view to redraw
        Log.d("UserHome", "setCheckedItem: $itemId")
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
