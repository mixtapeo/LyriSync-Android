package com.mixtapeo.lyrisync

import android.content.ComponentName
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.util.Log

class LyriSyncMediaService : NotificationListenerService() {

    private lateinit var mediaSessionManager: MediaSessionManager
    private var currentControllers: List<MediaController> = emptyList()

    override fun onCreate() {
        super.onCreate()
        mediaSessionManager = getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        // Once connected, grab the active media sessions
        val componentName = ComponentName(this, LyriSyncMediaService::class.java)

        try {
            // Get currently playing apps
            updateControllers(mediaSessionManager.getActiveSessions(componentName))

            // Listen for when new apps start playing
            mediaSessionManager.addOnActiveSessionsChangedListener({ controllers ->
                updateControllers(controllers)
            }, componentName)
        } catch (e: SecurityException) {
            Log.e("LyriSync", "Missing Notification Access permission!")
        }
    }

    private fun updateControllers(controllers: List<MediaController>?) {
        this.currentControllers = controllers ?: emptyList()

        // Find the one that is currently playing
        val activeController = currentControllers.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_PLAYING
        } ?: currentControllers.firstOrNull() // Fallback to the most recent one

        activeController?.let { controller ->
            // 1. Extract the Metadata
            val metadata = controller.metadata
            val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)

            Log.d("LyriSync", "Now Playing: $title by $artist")

            // 2. Set up a listener for track skips or pauses
            // THE OVERRIDES GO IN HERE!
            controller.registerCallback(object : MediaController.Callback() {

                override fun onMetadataChanged(metadata: MediaMetadata?) {
                    val newTitle = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: ""
                    val newArtist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: ""

                    Log.d("LyriSync", "Track Changed: $newTitle by $newArtist")
                    // Tell the bridge a new song started!
                    UniversalMediaBridge.updateState(newTitle, newArtist, 0L, true, true)
                }

                override fun onPlaybackStateChanged(state: PlaybackState?) {
                    val isPlaying = state?.state == PlaybackState.STATE_PLAYING
                    val currentPosition = state?.position ?: 0L

                    Log.d("LyriSync", "Is Playing: $isPlaying, Position: $currentPosition")
                    // Tell the bridge the time updated! (Keep the existing title/artist)
                    val current = UniversalMediaBridge.mediaState.value
                    UniversalMediaBridge.updateState(current.title, current.artist, currentPosition, isPlaying, false)
                }
            })
        }
    }
}