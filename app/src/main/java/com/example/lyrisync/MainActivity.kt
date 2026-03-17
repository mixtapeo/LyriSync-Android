package com.example.lyrisync

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.http.GET
import retrofit2.http.Query
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

data class LrcResponse(
    val id: Int,
    val name: String,
    val artistName: String,
    val syncedLyrics: String?,
    val plainLyrics: String?
)

data class LyricLine(
    val timeMs: Long,
    val text: String
)

interface LrcLibService {
    @GET("api/search")
    suspend fun searchLyrics(
        @Query("track_name") track: String,
        @Query("artist_name") artist: String
    ): List<LrcResponse>
}
private var syncJob: Job? = null
class MainActivity : AppCompatActivity() {

    private var parsedLyrics = listOf<LyricLine>()



    // 1. Replace with your Client ID from the Spotify Developer Dashboard
    private val clientId = "06f5df4fd4234a06bbc234600ed42851"
    private val redirectUri = "lyrisync://callback"
    private var spotifyAppRemote: SpotifyAppRemote? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Add this to prove the code is reaching this point
        findViewById<TextView>(R.id.songTitleText)?.text = "App Started! Connecting..."
        Log.d("Lyrisync", "onCreate finished")
    }

    override fun onStart() {
        super.onStart()
        Log.d("Lyrisync", "Attempting to connect with Client ID: $clientId")
        // 2. Setup connection parameters
        val connectionParams = ConnectionParams.Builder(clientId)
            .setRedirectUri(redirectUri)
            .showAuthView(true) // Set this to TRUE
            .build()

        // 3. Connect to Spotify
        SpotifyAppRemote.connect(this, connectionParams, object : Connector.ConnectionListener {
            override fun onConnected(appRemote: SpotifyAppRemote) {
                spotifyAppRemote = appRemote
                Log.d("Lyrisync", "Connected to Spotify!")
                connected()
            }

            override fun onFailure(throwable: Throwable) {
                Log.e("Lyrisync", "Connection failed: ${throwable.message}", throwable)
                // Common causes: Spotify isn't installed or Client ID is wrong
            }
        })
        SpotifyAppRemote.connect(this, connectionParams, object : Connector.ConnectionListener {
            override fun onConnected(appRemote: SpotifyAppRemote) {
                spotifyAppRemote = appRemote
                Log.d("Lyrisync", "Connected to Spotify!")
                connected()
            }

            // This is the one giving you the "overrides nothing" error
            override fun onFailure(throwable: Throwable) {
                Log.e("Lyrisync", "Connection failed: ${throwable.message}", throwable)

                // Use runOnUiThread to update the screen since this might happen on a background thread
                runOnUiThread {
                    val titleView = findViewById<TextView>(R.id.songTitleText)
                    titleView.text = "Connection Failed: ${throwable.message}"
                }
            }
        })
    }

    private var currentTrackUri: String? = null

    private fun connected() {
        spotifyAppRemote?.playerApi?.subscribeToPlayerState()?.setEventCallback { playerState ->
            val track = playerState.track
            if (track != null) {
                if (track.uri != currentTrackUri) {
                    currentTrackUri = track.uri
                    findViewById<TextView>(R.id.songTitleText).text = track.name
                    findViewById<TextView>(R.id.artistNameText).text = track.artist.name
                    fetchLyrics(track.name, track.artist.name)
                }

                // Start the sync loop if it's not running
                if (syncJob == null || !syncJob!!.isActive) {
                    startSyncLoop()
                }
            }
        }
    }

    private fun startSyncLoop() {
        syncJob?.cancel() // Stop any old loops
        syncJob = lifecycleScope.launch {
            while (isActive) {
                spotifyAppRemote?.playerApi?.playerState?.setResultCallback { playerState ->
                    val currentMs = playerState.playbackPosition
                    val currentLine = parsedLyrics.lastOrNull { it.timeMs <= currentMs }

                    val japaneseView = findViewById<TextView>(R.id.japaneseLyricText)
                    if (currentLine != null && japaneseView.text != currentLine.text) {
                        runOnUiThread {
                            japaneseView.text = currentLine.text
                            updateJishoDetails(currentLine.text)
                        }
                    }
                }
                delay(500) // Check every half-second
            }
        }
    }

    private fun fetchLyrics(title: String, artist: String) {
        val retrofit = retrofit2.Retrofit.Builder()
            .baseUrl("https://lrclib.net/")
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()

        val service = retrofit.create(LrcLibService::class.java)

        // Run on a background thread so the UI doesn't freeze
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = service.searchLyrics(title, artist)
                val bestMatch = response.firstOrNull { it.syncedLyrics != null || it.plainLyrics != null }

                withContext(Dispatchers.Main) {
                    if (bestMatch != null) {
                        // For now, let's just show the first line of plain lyrics
                        val lyrics = bestMatch.plainLyrics ?: "No text lyrics available."
                        findViewById<TextView>(R.id.japaneseLyricText).text = lyrics.lineSequence().firstOrNull()
                        Log.d("Lyrisync", "Lyrics Loaded!")
                        // parse lyrics to sync time
                        withContext(Dispatchers.Main) {
                            bestMatch.syncedLyrics?.let {
                                parsedLyrics = parseLrc(it)
                            }
                        }
                    } else {
                        findViewById<TextView>(R.id.japaneseLyricText).text = "Lyrics not found."
                    }
                }
            } catch (e: Exception) {
                Log.e("Lyrisync", "Network Error: ${e.message}")
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // 5. Always disconnect to save battery and memory
        spotifyAppRemote?.let {
            SpotifyAppRemote.disconnect(it)
        }
    }
}

// Checks if the line contains any Kanji characters
fun containsKanji(text: String): Boolean {
    return text.any { Character.UnicodeBlock.of(it) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS }
}

// LRC files look like [00:12.34] xxxx. We need to convert 00:12.34 into total milliseconds.
fun parseLrc(lrcContent: String): List<LyricLine> {
    val lyricList = mutableListOf<LyricLine>()
    val lines = lrcContent.split("\n")

    val regex = Regex("\\[(\\d{2}):(\\d{2})\\.(\\d{2})](.*)")

    for (line in lines) {
        val match = regex.find(line)
        if (match != null) {
            val min = match.groupValues[1].toLong()
            val sec = match.groupValues[2].toLong()
            val ms = match.groupValues[3].toLong() * 10 // xx is usually centiseconds
            val text = match.groupValues[4].trim()

            val totalMs = (min * 60 * 1000) + (sec * 1000) + ms
            lyricList.add(LyricLine(totalMs, text))
        }
    }
    return lyricList.sortedBy { it.timeMs }
}



private fun updateJishoDetails(text: String) {
    if (containsKanji(text)) {
        Log.d("Lyrisync", "Kanji detected in: $text. Fetching from Jisho...")
        // We'll put the Jisho API call here next!
    }
}