package com.example.citymove

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.splash_screen)

        supportActionBar?.hide()

        val logo = findViewById<View>(R.id.frameLogoIcon)
        val brand = findViewById<View>(R.id.layoutBrand)
        val tagline = findViewById<View>(R.id.tvTagline)

        logo.scaleX = 0.6f
        logo.scaleY = 0.6f

        logo.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(800)
            .start()

        brand.animate()
            .alpha(1f)
            .setStartDelay(400)
            .setDuration(600)
            .start()

        tagline.animate()
            .alpha(1f)
            .setStartDelay(700)
            .setDuration(600)
            .start()

        Handler(Looper.getMainLooper()).postDelayed({
            checkLoginStatus()
        }, 2500)
    }

    private fun checkLoginStatus() {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) {
            // User is signed in, go to HomeActivity
            startActivity(Intent(this, HomeActivity::class.java))
        } else {
            // No user is signed in, go to LoginActivity
            startActivity(Intent(this, LoginActivity::class.java))
        }
        finish()
    }
}