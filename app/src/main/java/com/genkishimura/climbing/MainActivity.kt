package com.genkishimura.climbing

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.genkishimura.climbing.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.captureButton.setOnClickListener {
            Toast.makeText(this, "Capture flow will be added next", Toast.LENGTH_SHORT).show()
        }

        binding.syncButton.setOnClickListener {
            Toast.makeText(this, "YouTube sync flow will be added next", Toast.LENGTH_SHORT).show()
        }
    }
}

