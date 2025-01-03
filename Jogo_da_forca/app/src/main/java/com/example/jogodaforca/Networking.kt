package com.example.jogodaforca

data class Room(
    val host: String = "",
    val maxPlayers: Int = 0,
    val currentPlayers: Int = 0,
    val gameStarted: Boolean = false,
    val players: Map<String, Player> = emptyMap(),
    val roomName: String = "",
    val endRound: Int = 0,
)

data class Player(
    val name: String = "",
    val points: Int = 0,
    val currentRound: Int = 1
)