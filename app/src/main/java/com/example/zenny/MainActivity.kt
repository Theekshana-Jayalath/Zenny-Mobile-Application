package com.example.zenny

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Apply window insets for edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Animate all splash elements
        val icon = findViewById<View>(R.id.splash_icon)
        val text = findViewById<View>(R.id.splash_text)
        val tagline = findViewById<View>(R.id.splash_tagline)


        icon.animate()
            .alpha(1f)
            .setDuration(800)
            .start()

        text.animate()
            .alpha(1f)
            .setDuration(800)
            .setStartDelay(200)
            .start()

        tagline.animate()
            .alpha(1f)
            .setDuration(800)
            .setStartDelay(400)
            .start()


    }
}