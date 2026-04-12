package com.mixtapeo.lyrisync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class UniversalTrackData(
    val title: String,
    val artist: String,
    val positionMs: Long,
    val isPlaying: Boolean,
    val triggerNewFetch: Boolean // True if the song literally just changed
)

object UniversalMediaBridge {
    private val _mediaState = MutableStateFlow(UniversalTrackData("", "", 0L, false, false))
    val mediaState: StateFlow<UniversalTrackData> = _mediaState

    fun updateState(title: String, artist: String, positionMs: Long, isPlaying: Boolean, isNewSong: Boolean) {
        _mediaState.value = UniversalTrackData(title, artist, positionMs, isPlaying, isNewSong)
    }
}