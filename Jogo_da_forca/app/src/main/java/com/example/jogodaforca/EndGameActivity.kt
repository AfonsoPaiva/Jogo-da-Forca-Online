package com.example.jogodaforca

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*

class EndGameActivity : AppCompatActivity() {

    private lateinit var roomId: String
    private lateinit var database: DatabaseReference
    private lateinit var playersTextView: TextView
    private lateinit var playAgainButton: Button
    private lateinit var playerName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_endgame)

        playersTextView = findViewById(R.id.playersTextView)
        playAgainButton = findViewById(R.id.playAgainButton)

        roomId = intent.getStringExtra("roomId") ?: return
        val playerName = intent.getStringExtra("playerName") ?: "Player"
        database = FirebaseDatabase.getInstance().reference.child("rooms").child(roomId)

        database.child("players").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val players = snapshot.getValue(object : GenericTypeIndicator<Map<String, Player>>() {})
                if (players != null) {
                    val displayPlayers = players.map {
                        "${it.value.name}: ${it.value.points} points"
                    }
                    playersTextView.text = displayPlayers.joinToString("\n")
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })


        playAgainButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
            database.removeValue()
        }
    }
}