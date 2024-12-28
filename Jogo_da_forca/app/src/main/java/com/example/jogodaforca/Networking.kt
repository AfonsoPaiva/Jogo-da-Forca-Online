package com.example.jogodaforca

import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

data class Room(
    val roomId: String = "",
    val status: String = "Waiting", // Status da sala: "Waiting", "In Progress", "Finished"
    val host: String = "", // Jogador que criou a sala (host)
    val players: MutableList<String> = mutableListOf(), // Lista de jogadores
    val currentPlayers: Int = 0, // Número atual de jogadores
    val maxPlayers: Int = 4, // Número máximo de jogadores
    val word: String = "",
    val guessedLetters: MutableList<String> = mutableListOf() // Lista de letras adivinhadas
)


class Networking {

    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private var roomRef: DatabaseReference? = null

    // Cria uma sala no Firebase com o número máximo de jogadores e o host
    fun createRoom(roomId: String, word: String, hostName: String) {
        val roomData = Room(
            roomId = roomId,
            host = hostName,
            currentPlayers = 1, // O host é o primeiro jogador
            players = mutableListOf(hostName), // O host está na lista de jogadores
            maxPlayers = 4, // Defina o número máximo de jogadores (ex: 4)
            word = word
        )
        // Salva os dados da sala no Firebase
        database.reference.child("rooms").child(roomId).setValue(roomData)
    }

    // Adiciona um jogador à sala (se não exceder o limite de jogadores)
    fun joinRoom(roomId: String, playerName: String) {
        roomRef = database.reference.child("rooms").child(roomId)

        roomRef?.get()?.addOnSuccessListener {
            val room = it.getValue(Room::class.java)

            // Verifique se a sala existe e se o número de jogadores não excede o limite
            if (room != null && room.currentPlayers < room.maxPlayers) {
                // Adiciona o jogador à lista de jogadores
                val updatedPlayers = room.players.toMutableList()
                updatedPlayers.add(playerName)

                // Atualiza os jogadores e o número de jogadores
                roomRef?.child("players")?.setValue(updatedPlayers)
                roomRef?.child("currentPlayers")?.setValue(room.currentPlayers + 1)
            } else {
                println("Não é possível adicionar mais jogadores. Sala cheia ou inexistente.")
            }
        }
    }

    // Inicia o jogo (muda o status da sala)
    fun startGame(roomId: String) {
        roomRef = database.reference.child("rooms").child(roomId)
        roomRef?.child("status")?.setValue("In Progress")
    }

    // Faz uma tentativa de adivinhar uma letra
    fun makeGuess(roomId: String, letter: Char) {
        roomRef = database.reference.child("rooms").child(roomId)

        roomRef?.child("guessedLetters")?.get()?.addOnSuccessListener {
            val guessedLetters = it.getValue(MutableList::class.java) as MutableList<String>? ?: mutableListOf()

            if (!guessedLetters.contains(letter.toString())) {
                guessedLetters.add(letter.toString())
                roomRef?.child("guessedLetters")?.setValue(guessedLetters)
            }
        }
    }

    // Faz o "listener" escutar as mudanças na sala em tempo real
    fun listenToGame(roomId: String, listener: GameListener) {
        roomRef = database.reference.child("rooms").child(roomId)

        roomRef?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val room = snapshot.getValue(Room::class.java)
                room?.let { listener.onGameUpdated(it) }
            }

            override fun onCancelled(error: DatabaseError) {
                println("Erro ao ouvir as mudanças no Firebase: ${error.message}")
            }
        })
    }

    interface GameListener {
        fun onGameUpdated(room: Room)
    }
}
