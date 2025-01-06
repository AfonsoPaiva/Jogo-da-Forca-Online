package com.example.jogodaforca

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.database.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class GameActivity : AppCompatActivity() {

    private lateinit var timerTextView: TextView
    private lateinit var wordTextView: TextView
    private lateinit var wrongGuessCountTextView: TextView
    private lateinit var currentRoundTextView: TextView
    private lateinit var hintTextView: TextView
    private lateinit var wordImageView: ImageView
    private lateinit var lettersGridLayout: GridLayout
    private lateinit var loadingLayout: LinearLayout
    private var hiddenWord: String = ""
    private var guessedLetters: MutableList<Char> = mutableListOf()
    private var wrongGuessCount: Int = 0
    private var points: Int = 1000
    private lateinit var roomRef: DatabaseReference
    private var roundTimer: CountDownTimer? = null
    private var endRound: Int = 0
    private lateinit var playerName: String
    private lateinit var roomId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        // Initialize UI components
        timerTextView = findViewById(R.id.timerTextView)
        wordTextView = findViewById(R.id.wordTextView)
        wrongGuessCountTextView = findViewById(R.id.wrongGuessCountTextView)
        currentRoundTextView = findViewById(R.id.currentRoundTextView)
        hintTextView = findViewById(R.id.hintTextView)
        wordImageView = findViewById(R.id.wordImageView)
        lettersGridLayout = findViewById(R.id.lettersGridLayout)
        loadingLayout = findViewById(R.id.loadingLayout)

        // Get roomId, playerName, and endRound from Intent
        roomId = intent.getStringExtra("roomId") ?: run {
            finish()
            return
        }
        playerName = intent.getStringExtra("playerName") ?: run {
            finish()
            return
        }
        endRound = intent.getIntExtra("endRound", 0)
        if (endRound == 0) {
            Log.e("GameActivity", "endRound value not passed correctly")
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
                    startGame()
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        // Dynamically create letter buttons
        createLetterButtons()
    }

    private fun createLetterButtons() {
        val letters = ('A'..'Z').toList()
        letters.forEach { letter ->
            val button = Button(this).apply {
                text = letter.toString()
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    height = ViewGroup.LayoutParams.WRAP_CONTENT
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                }
                setOnClickListener {
                    handleGuess(letter.lowercaseChar(), this)
                }
            }
            lettersGridLayout.addView(button)
        }
    }

    private fun resetLetterButtons() {
        for (i in 0 until lettersGridLayout.childCount) {
            val button = lettersGridLayout.getChildAt(i) as Button
            button.visibility = View.VISIBLE
        }
    }

    override fun onBackPressed() {
        // Do nothing to prevent going back
    }

    private fun startGame() {
        roomRef.child("players").child(playerName).get().addOnSuccessListener { snapshot ->
            val player = snapshot.getValue(Player::class.java)
            val currentRound = player?.currentRound ?: 1
            currentRoundTextView.text = "Round: $currentRound"
        }
        fetchRandomWordAndHint()
    }

    private fun fetchRandomWordAndHint() {
        // Show loading layout and stop the timer
        runOnUiThread {
            val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
            loadingLayout.startAnimation(fadeIn)
            loadingLayout.visibility = View.VISIBLE
            roundTimer?.cancel()
        }

        thread {
            var word = ""
            var definition = ""
            var imageUrl = ""
            var validWordFound = false

            while (!validWordFound) {
                try {
                    // Fetch random word
                    val wordUrl = URL("https://random-word-api.herokuapp.com/word?number=1")
                    val wordConnection = wordUrl.openConnection() as HttpURLConnection
                    wordConnection.requestMethod = "GET"

                    val wordResponseCode = wordConnection.responseCode
                    if (wordResponseCode == HttpURLConnection.HTTP_OK) {
                        val wordResponse = wordConnection.inputStream.bufferedReader().use { it.readText() }
                        val wordJsonArray = JSONArray(wordResponse)
                        if (wordJsonArray.length() > 0) {
                            word = wordJsonArray.getString(0)

                            // Fetch word definition
                            val definitionUrl = URL("https://api.dictionaryapi.dev/api/v2/entries/en/$word")
                            val definitionConnection = definitionUrl.openConnection() as HttpURLConnection
                            definitionConnection.requestMethod = "GET"

                            val definitionResponseCode = definitionConnection.responseCode
                            if (definitionResponseCode == HttpURLConnection.HTTP_OK) {
                                val definitionResponse = definitionConnection.inputStream.bufferedReader().use { it.readText() }
                                val definitionJsonArray = JSONArray(definitionResponse)
                                if (definitionJsonArray.length() > 0) {
                                    val definitionJson = definitionJsonArray.getJSONObject(0)
                                    val meaningsArray = definitionJson.getJSONArray("meanings")
                                    if (meaningsArray.length() > 0) {
                                        val definitionsArray = meaningsArray.getJSONObject(0).getJSONArray("definitions")
                                        if (definitionsArray.length() > 0) {
                                            definition = definitionsArray.getJSONObject(0).getString("definition")

                                            // Fetch word image from Wikipedia API
                                            val wikiUrl = URL("https://en.wikipedia.org/w/api.php?action=query&titles=$word&prop=pageimages&format=json&pithumbsize=200")
                                            val wikiConnection = wikiUrl.openConnection() as HttpURLConnection
                                            wikiConnection.requestMethod = "GET"

                                            val wikiResponseCode = wikiConnection.responseCode
                                            if (wikiResponseCode == HttpURLConnection.HTTP_OK) {
                                                val wikiResponse = wikiConnection.inputStream.bufferedReader().use { it.readText() }
                                                val wikiJson = JSONObject(wikiResponse)
                                                val pages = wikiJson.getJSONObject("query").getJSONObject("pages")
                                                val page = pages.keys().asSequence().firstOrNull()?.let { pages.getJSONObject(it) }
                                                if (page != null && page.has("thumbnail")) {
                                                    imageUrl = page.getJSONObject("thumbnail").getString("source")
                                                    validWordFound = true
                                                }
                                            }
                                            wikiConnection.disconnect()
                                        }
                                    }
                                }
                                definitionConnection.disconnect()
                            }
                            wordConnection.disconnect()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    // this will happens if catch an error
                    word = "error"
                    definition = "An error occurred"
                }
            }

            runOnUiThread {
                if (!isDestroyed) {
                    hiddenWord = word
                    wordTextView.text = "Word: ${"_ ".repeat(hiddenWord.length)}"
                    hintTextView.text = "Hint: $definition"
                    if (imageUrl.isNotEmpty()) {
                        Glide.with(this).load(imageUrl).into(wordImageView)
                    } else {
                        Log.d("GameActivity", "Image URL is empty")
                    }
                    // Hide loading and start the timer
                    loadingLayout.visibility = View.GONE
                    startRoundTimer()
                    resetLetterButtons()
                }
            }
        }
    }

    private fun handleGuess(guess: Char, button: Button) {
        if (guessedLetters.contains(guess)) {
            return
        }

        guessedLetters.add(guess)
        button.visibility = View.GONE

        if (hiddenWord.contains(guess)) {
            points += 10
            val updatedWord = hiddenWord.map { if (guessedLetters.contains(it)) it else '_' }.joinToString(" ")
            wordTextView.text = "Word: $updatedWord"
            if (!updatedWord.contains('_')) {
                nextRound()
            }
        } else {
            points -= 20
        }
        wrongGuessCountTextView.text = "Points: $points"
    }

    private fun startRoundTimer() {
        roundTimer?.cancel() // Cancel any existing timer
        roundTimer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timerTextView.text = "Time: ${millisUntilFinished / 1000}"
            }

            override fun onFinish() {
                points -= 500
                wrongGuessCountTextView.text = "Points: $points"
                nextRound()
            }
        }.start()
    }

    private fun nextRound() {
        roomRef.child("players").child(playerName).get().addOnSuccessListener { snapshot ->
            val player = snapshot.getValue(Player::class.java)
            val currentRound = player?.currentRound ?: 1
            Log.d("GameActivity", "Current Round: $currentRound")
            Log.d("GameActivity", "End Round: $endRound")
            if (currentRound >= endRound) {
                Log.d("GameActivity", "Game ended")
                // Update points in the database
                roomRef.child("players").child(playerName).setValue(player?.copy(points = player.points + points)).addOnCompleteListener {
                    val intent = Intent(this, EndGameActivity::class.java)
                    intent.putExtra("roomId", roomRef.key)
                    intent.putExtra("playerName", playerName)
                    startActivity(intent)
                    finish()
                }
            } else {
                roomRef.child("players").child(playerName).setValue(player?.copy(currentRound = currentRound + 1))
                currentRoundTextView.text = "Round: ${currentRound + 1}"
                guessedLetters.clear()
                wrongGuessCount = 0
                fetchRandomWordAndHint()
            }
        }
    }
}