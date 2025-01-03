package com.example.jogodaforca

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
    private lateinit var playerName: String
    private lateinit var database: DatabaseReference
    private lateinit var playersTextView: TextView
    private lateinit var startGameButton: Button
    private lateinit var currentPlayersTextView: TextView
    private lateinit var roomIdTextView: TextView
    private lateinit var roomNameTextView: TextView
    private lateinit var roundsTextView: TextView
    private lateinit var playersListView: ListView
    private var currentPlayers = 0
    private var hostName = ""
    private var endRound = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wait_room)

        roomId = intent.getStringExtra("roomId") ?: return
        roomName = intent.getStringExtra("roomName") ?: "Waiting Room"
        playerName = intent.getStringExtra("playerName") ?: "Player"

        currentPlayersTextView = findViewById(R.id.currentPlayersTextView)
        startGameButton = findViewById(R.id.startGameButton)
        playersListView = findViewById(R.id.playersListView)
        roomIdTextView = findViewById(R.id.roomIdTextView)
        roomNameTextView = findViewById(R.id.roomNameTextView)
        roundsTextView = findViewById(R.id.roundsTextView)

        roomNameTextView.text = "Room Name: $roomName"
        roomIdTextView.text = "Room ID: $roomId"

        database = FirebaseDatabase.getInstance().reference.child("rooms").child(roomId)

        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val room = snapshot.getValue(Room::class.java)
                if (room != null) {
                    currentPlayers = room.currentPlayers
                    currentPlayersTextView.text = "Current Players: $currentPlayers/${room.maxPlayers}"
                    hostName = room.host
                    endRound = room.endRound
                    roundsTextView.text = "Rounds: $endRound"

                    if (hostName == playerName) {
                        startGameButton.isEnabled = true
                        startGameButton.visibility = Button.VISIBLE
                    } else {
                        startGameButton.isEnabled = false
                        startGameButton.visibility = Button.INVISIBLE
                    }

                    updatePlayersList(room.players, room.host)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@WaitRoomActivity, "Erro ao carregar dados da sala.", Toast.LENGTH_SHORT).show()
            }
        })

        val playerName = intent.getStringExtra("playerName") ?: "Player"
        val roomRef = FirebaseDatabase.getInstance().reference.child("rooms").child(roomId)

        startGameButton.setOnClickListener {
            if (hostName == playerName) {
                roomRef.child("gameStarted").setValue(true).addOnSuccessListener {
                    val intent = Intent(this, GameActivity::class.java)
                    intent.putExtra("roomId", roomId)
                    intent.putExtra("playerName", playerName)
                    intent.putExtra("endRound", endRound)
                    startActivity(intent)
                }
            }
        }
        database.child("gameStarted").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val gameStarted = snapshot.getValue(Boolean::class.java) ?: false
                if (gameStarted) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        val intent = Intent(this@WaitRoomActivity, GameActivity::class.java)
                        intent.putExtra("roomId", roomId)
                        intent.putExtra("playerName", playerName)
                        intent.putExtra("endRound", endRound)
                        startActivity(intent)
                    }, 2000) // Delay of 2 seconds
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@WaitRoomActivity, "Erro ao acessar os dados da sala.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updatePlayersList(players: Map<String, Player>, host: String) {
        val displayPlayers = players.map { if (it.key == host) "host: ${it.key}" else it.key }
        val adapter = ArrayAdapter(this, R.layout.list_item, R.id.listItemTextView, displayPlayers)
        playersListView.adapter = adapter
    }

    override fun onDestroy() {
        super.onDestroy()
        database.removeValue()
    }
}