package com.example.jogodaforca

import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class GameActivity : AppCompatActivity() {

    private lateinit var wordTextView: TextView
    private lateinit var guessEditText: EditText
    private lateinit var guessButton: Button
    private lateinit var guessedLettersTextView: TextView
    private lateinit var wrongGuessCountTextView: TextView
    private lateinit var currentRoundTextView: TextView
    private lateinit var hintTextView: TextView
    private var hiddenWord: String = ""
    private var guessedLetters: MutableList<Char> = mutableListOf()
    private var wrongGuessCount: Int = 0
    private lateinit var roomRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        // Initialize UI components
        wordTextView = findViewById(R.id.wordTextView)
        guessEditText = findViewById(R.id.guessEditText)
        guessButton = findViewById(R.id.guessButton)
        guessedLettersTextView = findViewById(R.id.guessedLettersTextView)
        wrongGuessCountTextView = findViewById(R.id.wrongGuessCountTextView)
        currentRoundTextView = findViewById(R.id.currentRoundTextView)
        hintTextView = findViewById(R.id.hintTextView)

        // Get roomId from Intent
        val roomId = intent.getStringExtra("roomId") ?: run {
            Toast.makeText(this, "Erro: ID da sala não encontrado.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Reference to Firebase
        roomRef = FirebaseDatabase.getInstance().reference.child("rooms").child(roomId)

        // Listen for game start
        roomRef.child("gameStarted").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val gameStarted = snapshot.getValue(Boolean::class.java) ?: false
                if (gameStarted) {
                    startGame(roomId)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@GameActivity, "Erro ao acessar os dados da sala.", Toast.LENGTH_SHORT).show()
            }
        })

        // Configure guess button
        guessButton.setOnClickListener {
            val guess = guessEditText.text.toString().trim().lowercase()
            if (guess.isNotEmpty()) {
                if (guess.length == 1) {
                    handleGuess(roomRef, guess[0])
                } else {
                    handleWordGuess(roomRef, guess)
                }
            } else {
                Toast.makeText(this, "Por favor, insira uma letra ou palavra.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startGame(roomId: String) {
        roomRef.child("currentRound").get().addOnSuccessListener { snapshot ->
            val currentRound = snapshot.getValue(Int::class.java) ?: 1
            currentRoundTextView.text = "Round: $currentRound"
        }
        fetchRandomWordAndHint()
        startRoundTimer()
    }

    private fun fetchRandomWordAndHint() {
        thread {
            try {
                val url = URL("https://wordsapiv1.p.rapidapi.com/words/?random=true")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("x-rapidapi-key", "YOUR_RAPIDAPI_KEY")
                connection.setRequestProperty("x-rapidapi-host", "wordsapiv1.p.rapidapi.com")

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonResponse = JSONObject(response)
                    val word = jsonResponse.getString("word")
                    val definition = jsonResponse.getJSONArray("results").getJSONObject(0).getString("definition")

                    runOnUiThread {
                        hiddenWord = word
                        wordTextView.text = "Word: ${"_ ".repeat(hiddenWord.length)}"
                        hintTextView.text = "Hint: $definition"
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@GameActivity, "Failed to fetch word", Toast.LENGTH_SHORT).show()
                    }
                }
                connection.disconnect()
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@GameActivity, "Failed to fetch word", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun handleGuess(roomRef: DatabaseReference, guess: Char) {
        if (guessedLetters.contains(guess)) {
            Toast.makeText(this, "You already guessed that letter.", Toast.LENGTH_SHORT).show()
            return
        }

        guessedLetters.add(guess)
        guessedLettersTextView.text = "Guessed Letters: ${guessedLetters.joinToString(", ")}"

        if (hiddenWord.contains(guess)) {
            val updatedWord = hiddenWord.map { if (guessedLetters.contains(it)) it else '_' }.joinToString(" ")
            wordTextView.text = "Word: $updatedWord"
            if (!updatedWord.contains('_')) {
                Toast.makeText(this, "Congratulations! You've guessed the word!", Toast.LENGTH_SHORT).show()
                // Handle end of round or game
            }
        } else {
            wrongGuessCount++
            wrongGuessCountTextView.text = "Wrong Guesses: $wrongGuessCount"
            if (wrongGuessCount >= 6) { // Assuming 6 wrong guesses end the game
                Toast.makeText(this, "Game Over! The word was $hiddenWord.", Toast.LENGTH_SHORT).show()
                // Handle end of game
            }
        }
    }

    private fun handleWordGuess(roomRef: DatabaseReference, guess: String) {
        if (guess == hiddenWord) {
            Toast.makeText(this, "Congratulations! You've guessed the word!", Toast.LENGTH_SHORT).show()
            // Handle end of round or game
        } else {
            wrongGuessCount++
            wrongGuessCountTextView.text = "Wrong Guesses: $wrongGuessCount"
            if (wrongGuessCount >= 6) { // Assuming 6 wrong guesses end the game
                Toast.makeText(this, "Game Over! The word was $hiddenWord.", Toast.LENGTH_SHORT).show()
                // Handle end of game
            }
        }
    }

    private fun startRoundTimer() {
        // Start a countdown timer for the round
        object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // Update UI with the remaining time
            }

            override fun onFinish() {
                // Handle end of round
                Toast.makeText(this@GameActivity, "Time's up!", Toast.LENGTH_SHORT).show()
            }
        }.start()
    }
}