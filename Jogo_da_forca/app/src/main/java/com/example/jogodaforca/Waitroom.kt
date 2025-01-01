package com.example.jogodaforca

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*

class WaitRoomActivity : AppCompatActivity() {

    private lateinit var roomId: String
    private lateinit var roomName: String
    private lateinit var currentPlayersTextView: TextView
    private lateinit var startGameButton: Button
    private lateinit var playersListView: ListView
    private lateinit var roomIdTextView: TextView
    private lateinit var roomNameTextView: TextView
    private var currentPlayers = 1 // The host enters first
    private lateinit var database: DatabaseReference
    private lateinit var hostName: String
    private lateinit var playerName: String // Define playerName

    // Function to update the list of players in the interface
    private fun updatePlayersList(players: List<String>, host: String) {
        // Add the prefix "host:" to the host's name
        val displayPlayers = players.map { if (it == host) "host: $it" else it }
        // Use a simple adapter to display the list of players in the ListView
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, displayPlayers)
        playersListView.adapter = adapter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wait_room)

        // Get the room ID and room name passed by the Intent
        roomId = intent.getStringExtra("roomId") ?: return
        roomName = intent.getStringExtra("roomName") ?: "Waiting Room"
        playerName = intent.getStringExtra("playerName") ?: "Player" // Get playerName from Intent

        currentPlayersTextView = findViewById(R.id.currentPlayersTextView)
        startGameButton = findViewById(R.id.startGameButton)
        playersListView = findViewById(R.id.playersListView)
        roomIdTextView = findViewById(R.id.roomIdTextView)
        roomNameTextView = findViewById(R.id.roomNameTextView)

        // Set the room name and ID in the TextViews
        roomNameTextView.text = "Nome da Sala: $roomName"
        roomIdTextView.text = "Room ID: $roomId"

        database = FirebaseDatabase.getInstance().reference.child("rooms").child(roomId)

        // Listener to monitor changes in the room
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val room = snapshot.getValue(Room::class.java)
                if (room != null) {
                    currentPlayers = room.currentPlayers
                    currentPlayersTextView.text = "Jogadores atuais: $currentPlayers/${room.maxPlayers}"
                    hostName = room.host

                    // Enable the "Start Game" button only for the host
                    if (hostName == playerName) {
                        startGameButton.isEnabled = true
                        startGameButton.visibility = Button.VISIBLE
                    } else {
                        startGameButton.isEnabled = false
                        startGameButton.visibility = Button.INVISIBLE
                    }

                    // Update the list of players
                    updatePlayersList(room.players, room.host)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@WaitRoomActivity, "Erro ao carregar dados da sala.", Toast.LENGTH_SHORT).show()
            }
        })

        startGameButton.setOnClickListener {
            if (hostName == playerName) {
                database.child("gameStarted").setValue(true).addOnSuccessListener {
                    val intent = Intent(this, GameActivity::class.java)
                    intent.putExtra("roomId", roomId)
                    startActivity(intent)
                }.addOnFailureListener {
                    Toast.makeText(this, "Erro ao iniciar o jogo.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Apenas o host pode iniciar o jogo.", Toast.LENGTH_SHORT).show()
            }
        }

        // Listen for game start
        database.child("gameStarted").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val gameStarted = snapshot.getValue(Boolean::class.java) ?: false
                if (gameStarted) {
                    val intent = Intent(this@WaitRoomActivity, GameActivity::class.java)
                    intent.putExtra("roomId", roomId)
                    startActivity(intent)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@WaitRoomActivity, "Erro ao acessar os dados da sala.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        // Remove the room from Firebase when the user leaves the page
        database.removeValue()
    }
}