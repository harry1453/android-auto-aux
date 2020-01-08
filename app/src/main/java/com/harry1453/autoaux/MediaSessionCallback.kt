package com.harry1453.autoaux

import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat

class MediaSessionCallback(private val mediaSession: MediaSessionCompat, private val audioMirror: AudioMirror) : MediaSessionCompat.Callback() {
    init {
        mediaSession.setPlaybackState(stoppedState)
    }

    private val onError: (Throwable) -> Unit = {
        it.printStackTrace()
        val errorMessage = if (it.message.isNullOrBlank()) {
            "Error: ${it.javaClass.name}"
        } else {
            "Error: ${it.javaClass.name}: ${it.message}"
        }
        mediaSession.setPlaybackState(PlaybackStateCompat.Builder()
            .setState(PlaybackStateCompat.STATE_ERROR, 0, 1F, 0) // TODO can we leave updateTime as 0?
            .setErrorMessage(PlaybackStateCompat.ERROR_CODE_APP_ERROR, errorMessage)
            .build())
    }

    override fun onPlay() {
        audioMirror.start(onError)
        mediaSession.setPlaybackState(mirroringState)
    }

    override fun onStop() {
        audioMirror.stop()
        mediaSession.setPlaybackState(stoppedState)
    }

    override fun onPause() { // TODO is this necessary?
        onStop()
    }

    companion object {
        private val mirroringState = PlaybackStateCompat.Builder()
            .setState(PlaybackStateCompat.STATE_PLAYING, 0, 1F, 0) // TODO can we leave updateTime as 0?
            .setActions(PlaybackStateCompat.ACTION_STOP)
            .build()

        private val stoppedState = PlaybackStateCompat.Builder()
            .setState(PlaybackStateCompat.STATE_STOPPED, 0, 1F, 0) // TODO can we leave updateTime as 0?
            .setActions(PlaybackStateCompat.ACTION_PLAY)
            .build()
    }
}