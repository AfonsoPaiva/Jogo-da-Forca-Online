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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_room)

        playerName = intent.getStringExtra("playerName") ?: "Player"

        roomNameEditText = findViewById(R.id.roomNameEditText)
        maxPlayersEditText = findViewById(R.id.maxPlayersEditText)
        createRoomButton = findViewById(R.id.createRoomButton)

        createRoomButton.setOnClickListener {
            val roomName = roomNameEditText.text.toString()
            val maxPlayers = maxPlayersEditText.text.toString().toIntOrNull()

            if (roomName.isNotEmpty() && maxPlayers != null && maxPlayers > 1) {
                val roomId = generateRoomId()
                val database = FirebaseDatabase.getInstance().reference
                val roomRef = database.child("rooms").child(roomId)

                val roomData = mapOf(
                    "host" to playerName,
                    "maxPlayers" to maxPlayers,
                    "currentPlayers" to 1,
                    "gameStarted" to false,
                    "players" to listOf(playerName)
                )

                roomRef.setValue(roomData).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val intent = Intent(this, WaitRoomActivity::class.java)
                        intent.putExtra("roomId", roomId)
                        intent.putExtra("roomName", roomName)
                        startActivity(intent)
                    } else {
                        Toast.makeText(this, "Erro ao criar a sala. Tente novamente.", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Por favor, preencha os campos corretamente.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun generateRoomId(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val random = Random()
        val roomId = StringBuilder()
        for (i in 0 until 5) {
            roomId.append(chars[random.nextInt(chars.length)])
        }
        return roomId.toString()
    }
}