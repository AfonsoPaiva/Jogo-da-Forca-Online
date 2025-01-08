# Jogo da Forca Online

 Um jogo da forca simples em que um utilizador consegue criar uma sala, na qual  podem entrar outros jogadores através do código inserido inicialmente.

 ![Foto-sample](https://i.postimg.cc/Z57FGjfQ/Intro.png)

### Features
✔️ **Jogo Multijogador Co-op** 

✔️ **Interface Intuitiva**

✔️ **Sistema de Pontuação**
 
✔️ **Seleção de Palavras**

✔️ **Chat Integrado** 

✔️ **Responsividade**

### APIs Utilizadas 

|Nome    | Propósito  |
|------------------------|------------|
|[Random-Word-API](https://random-word-api.herokuapp.com/word?number=1) | Procura uma palavra aleatória |
| [Free-Dictionary-API](https://api.dictionaryapi.dev/api/v2/entries/en/dog)| Procura uma definição da palvra aleatória escolhida, neste caso a palvra **dog**|
| [Wikipedia-API](https://api.dictionaryapi.dev/api/v2/entries/en/dog)| Procura uma imagem da palvra aleatória escolhida, neste caso a palvra **dog**|


```kotlin
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
                        Log.d("GameActivity", "Image URL is empty")s
                    }
                    // Hide loading and start the timer
                    loadingLayout.visibility = View.GONE
                    startRoundTimer()
                    resetLetterButtons()
                }
            }
        }
    }
```

Para a realização deste projeto foram utilizadas 3 APIs publicas diferentes dado do seguinte excerto que serve para a seleção de uma palavra aleatória num banco de dados, de seguida é encontrada uma definição para essa mesma palavra e por fim é procurada uma imagem que corresponda a essa palavra e apenas com estas 3 condições diferentes é que são encontradas as `hidden words` .

### Base de dados 

 ![Foto-Base-Dados](https://i.postimg.cc/nzvdfB9Q/room-eg.png)

Nesta Base de Dados são armazenados todos as informações necessárias para o jogo, cada jogador possui uma ronda diferente e ambos jogam com palavras diferentes, a pontuação é armazenada no final e ao sair da sala a base de dados será eliminada
