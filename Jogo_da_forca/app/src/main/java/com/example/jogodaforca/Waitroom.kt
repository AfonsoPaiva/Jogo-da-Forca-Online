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
    private lateinit var currentPlayersTextView: TextView
    private lateinit var startGameButton: Button
    private lateinit var playersListView: ListView
    private lateinit var roomIdTextView: TextView
    private var currentPlayers = 1 // O host entra primeiro

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wait_room)

        // Obter o ID da sala passado pela Intent
        roomId = intent.getStringExtra("roomId") ?: return
        currentPlayersTextView = findViewById(R.id.currentPlayersTextView)
        startGameButton = findViewById(R.id.startGameButton)
        playersListView = findViewById(R.id.playersListView)
        roomIdTextView = findViewById(R.id.roomIdTextView)

        // Set the roomId in the TextView
        roomIdTextView.text = "Room ID: $roomId"

        val database = FirebaseDatabase.getInstance().reference.child("rooms").child(roomId)

        // Listener para monitorar mudanças na sala
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val room = snapshot.getValue(Room::class.java)
                if (room != null) {
                    currentPlayers = room.currentPlayers
                    currentPlayersTextView.text = "Jogadores atuais: $currentPlayers/${room.maxPlayers}"

                    // Habilitar o botão "Começar" se o número máximo de jogadores for alcançado e o jogador for o host
                    startGameButton.isEnabled = currentPlayers == room.maxPlayers && room.host == "HostPlayer"

                    // Atualizar a lista de jogadores
                    updatePlayersList(room.players)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@WaitRoomActivity, "Erro ao carregar dados da sala.", Toast.LENGTH_SHORT).show()
            }
        })

        startGameButton.setOnClickListener {
            database.child("rooms").child(roomId).get().addOnSuccessListener { snapshot ->
                val room = snapshot.getValue(Room::class.java)
                if (room != null && room.host == "HostPlayer" && room.currentPlayers == room.maxPlayers) {
                    database.child("rooms").child(roomId).child("status").setValue("In Progress")
                    val intent = Intent(this, GameActivity::class.java)
                    intent.putExtra("roomId", roomId)
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "Você não é o host ou a sala não está cheia!", Toast.LENGTH_SHORT).show()
                }
            }
        }

    }

    // Função para atualizar a lista de jogadores na interface
    private fun updatePlayersList(players: List<String>) {
        // Usando um adaptador simples para exibir a lista de jogadores no ListView
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, players)
        playersListView.adapter = adapter
    }
}