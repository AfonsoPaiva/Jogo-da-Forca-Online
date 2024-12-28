package com.example.jogodaforca

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.GenericTypeIndicator

class GameActivity : AppCompatActivity() {

    private lateinit var wordTextView: TextView
    private lateinit var letterEditText: EditText
    private lateinit var guessButton: Button
    private var hiddenWord: String = ""
    private var guessedLetters: MutableList<Char> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        // Initialize UI components
        wordTextView = findViewById(R.id.wordTextView)
        letterEditText = findViewById(R.id.letterEditText)
        guessButton = findViewById(R.id.guessButton)

        // Get roomId from Intent
        val roomId = intent.getStringExtra("roomId") ?: run {
            Toast.makeText(this, "Erro: ID da sala não encontrado.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Reference to Firebase
        val roomRef = FirebaseDatabase.getInstance().reference.child("rooms").child(roomId)

        // Fetch the hidden word from Firebase
        roomRef.child("word").get().addOnSuccessListener { snapshot ->
            hiddenWord = snapshot.getValue(String::class.java) ?: "example"
            updateWordDisplay() // Update word display with underscores
        }.addOnFailureListener {
            Toast.makeText(this, "Erro ao carregar a palavra do jogo.", Toast.LENGTH_SHORT).show()
        }

        // Configure guess button
        guessButton.setOnClickListener {
            val guessedLetter = letterEditText.text.toString().trim().lowercase()
            if (guessedLetter.isNotEmpty() && guessedLetter.length == 1) {
                handleGuess(roomRef, guessedLetter[0])
            } else {
                Toast.makeText(this, "Por favor, insira apenas uma letra.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateWordDisplay() {
        val displayedWord = StringBuilder()
        for (letter in hiddenWord) {
            if (guessedLetters.contains(letter)) {
                displayedWord.append(letter)
            } else {
                displayedWord.append("_")
            }
            displayedWord.append(" ")
        }
        wordTextView.text = displayedWord.toString().trim()
    }

    private fun handleGuess(roomRef: DatabaseReference, guessedLetter: Char) {
        // Fetch guessed letters from Firebase
        roomRef.child("guessedLetters").get().addOnSuccessListener { snapshot ->
            val guessedLettersFromFirebase = snapshot.getValue(object : GenericTypeIndicator<MutableList<String>>() {}) ?: mutableListOf()

            if (!guessedLettersFromFirebase.contains(guessedLetter.toString())) {
                guessedLetters.add(guessedLetter)
                guessedLettersFromFirebase.add(guessedLetter.toString())
                roomRef.child("guessedLetters").setValue(guessedLettersFromFirebase)
                    .addOnSuccessListener {
                        updateWordDisplay()
                        letterEditText.text.clear()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Erro ao atualizar a letra no servidor.", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Essa letra já foi adivinhada!", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Erro ao acessar os dados da sala.", Toast.LENGTH_SHORT).show()
        }
    }
}
