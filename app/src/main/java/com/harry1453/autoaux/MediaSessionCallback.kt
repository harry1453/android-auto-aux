package com.harry1453.autoaux

import android.media.MediaMetadata
import android.os.SystemClock
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat

class MediaSessionCallback(private val mediaSession: MediaSessionCompat, private val audioMirror: AudioMirror) : MediaSessionCompat.Callback() {
    private var playStart = SystemClock.elapsedRealtime()

    init {
        setStopped()
    }

    private val onError: (Throwable) -> Unit = {
        it.printStackTrace()
        val errorMessage = if (it.message.isNullOrBlank()) {
            "Error: ${it.javaClass.name}"
        } else {
            "Error: ${it.javaClass.name}: ${it.message}"
        }
        mediaSession.setPlaybackState(PlaybackStateCompat.Builder()
            .setState(PlaybackStateCompat.STATE_ERROR, 0, 1F, playStart)
            .setErrorMessage(PlaybackStateCompat.ERROR_CODE_APP_ERROR, errorMessage)
            .build())
    }

    override fun onPlay() {
        audioMirror.start(onError)
        setMirroring()
    }

    override fun onStop() {
        audioMirror.stop()
        setStopped()
    }

    override fun onPause() {
        onStop()
    }

    private fun setMirroring() {
        playStart = SystemClock.elapsedRealtime()
        mediaSession.setMetadata(MIRROR_METADATA)
        mediaSession.setPlaybackState(PlaybackStateCompat.Builder()
            .setState(PlaybackStateCompat.STATE_PLAYING, 0, 1F, playStart)
            .setActions(PlaybackStateCompat.ACTION_STOP)
            .build())
    }

    private fun setStopped() {
        mediaSession.setMetadata(MIRROR_METADATA)
        mediaSession.setPlaybackState(PlaybackStateCompat.Builder()
            .setState(PlaybackStateCompat.STATE_STOPPED, 0, 1F, playStart)
            .setActions(PlaybackStateCompat.ACTION_PLAY)
            .build())
    }

    companion object {
        private val MIRROR_METADATA = MediaMetadataCompat.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, "Aux In")
            .putString(MediaMetadata.METADATA_KEY_DISPLAY_DESCRIPTION, "Auxiliary Input")
            .build()
    }
}