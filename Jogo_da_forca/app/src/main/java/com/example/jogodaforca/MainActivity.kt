package com.example.jogodaforca

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp

class MainActivity : AppCompatActivity() {

    private lateinit var playerNameEditText: EditText
    private lateinit var createRoomButton: Button
    private lateinit var joinRoomButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        playerNameEditText = findViewById(R.id.playerNameEditText)
        createRoomButton = findViewById(R.id.createRoomButton)
        joinRoomButton = findViewById(R.id.joinRoomButton)

        createRoomButton.setOnClickListener {
            val playerName = playerNameEditText.text.toString()
            if (playerName.isNotEmpty()) {
                val intent = Intent(this, CreateRoomActivity::class.java)
                intent.putExtra("playerName", playerName)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show()
            }
        }

        joinRoomButton.setOnClickListener {
            val playerName = playerNameEditText.text.toString()
            if (playerName.isNotEmpty()) {
                val intent = Intent(this, JoinRoomActivity::class.java)
                intent.putExtra("playerName", playerName)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show()
            }
        }
    }
}