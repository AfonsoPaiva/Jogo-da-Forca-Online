package com.example.jogodaforca

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*

class EndGameActivity : AppCompatActivity() {

    private lateinit var playersTextView: TextView
    private lateinit var roomRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_endgame)

        playersTextView = findViewById(R.id.playersTextView)

        // Get roomId from Intent
        val roomId = intent.getStringExtra("roomId") ?: run {
            finish()
            return
        }

        // Reference to Firebase
        roomRef = FirebaseDatabase.getInstance().reference.child("rooms").child(roomId)

        // Fetch and display players and points
        fetchPlayersAndPoints()
    }

    private fun fetchPlayersAndPoints() {
        roomRef.child("players").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val playersList = mutableListOf<Pair<String, Int>>()
                for (playerSnapshot in snapshot.children) {
                    val playerName = playerSnapshot.getValue(String::class.java) ?: ""
                    val playerPoints = playerSnapshot.child("points").getValue(Int::class.java) ?: 0
                    playersList.add(Pair(playerName, playerPoints))
                }
                playersList.sortByDescending { it.second }
                displayPlayers(playersList)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun displayPlayers(playersList: List<Pair<String, Int>>) {
        val displayText = playersList.joinToString("\n") { "${it.first}: ${it.second} points" }
        playersTextView.text = displayText
    }
}