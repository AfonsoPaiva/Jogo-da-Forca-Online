package com.example.jogodaforca

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase
import java.util.Random

class CreateRoomActivity : AppCompatActivity() {

    private lateinit var roomNameEditText: EditText
    private lateinit var maxPlayersEditText: EditText
    private lateinit var createRoomButton: Button
    private lateinit var playerName: String
    private lateinit var roundsEditText: EditText
    private var roomId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_room)

        playerName = intent.getStringExtra("playerName") ?: "Player"

        roomNameEditText = findViewById(R.id.roomNameEditText)
        maxPlayersEditText = findViewById(R.id.maxPlayersEditText)
        createRoomButton = findViewById(R.id.createRoomButton)
        roundsEditText = findViewById(R.id.roundsEditText)

        createRoomButton.setOnClickListener {
            val roomName = roomNameEditText.text.toString()
            val maxPlayers = maxPlayersEditText.text.toString().toIntOrNull()
            val rounds = roundsEditText.text.toString().toIntOrNull() ?: 1

            if (roomName.isNotEmpty() && maxPlayers != null && maxPlayers > 1) {
                roomId = generateRoomId()
                val database = FirebaseDatabase.getInstance().reference
                val roomRef = database.child("rooms").child(roomId!!)

                val roomData = mapOf(
                    "host" to playerName,
                    "maxPlayers" to maxPlayers,
                    "currentPlayers" to 1,
                    "gameStarted" to false,
                    "players" to mapOf(
                        playerName to Player(
                            playerName,
                            0
                        )
                    ), // Initialize player points to 0
                    "roomName" to roomName,
                    "endRound" to rounds,)

                roomRef.setValue(roomData).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val intent = Intent(this, WaitRoomActivity::class.java)
                        intent.putExtra("roomId", roomId)
                        intent.putExtra("roomName", roomName)
                        intent.putExtra("playerName", playerName)
                        intent.putExtra("endRound", rounds)
                        startActivity(intent)
                    }
                }
            }
        }
    }

    private fun generateRoomId(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val random = Random()
        val roomId = StringBuilder()
        for (i in 0 until 5) {
            roomId.append(chars[random.nextInt(chars.length)])
        }
        return roomId.toString()
    }
}