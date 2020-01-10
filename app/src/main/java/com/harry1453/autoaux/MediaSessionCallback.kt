package com.harry1453.autoaux

import android.graphics.Bitmap
import android.media.MediaMetadata
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.session.PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN

class MediaSessionCallback(private val mediaSession: MediaSessionCompat, albumArt: Bitmap, private val audioMirror: AudioMirror) : MediaSessionCompat.Callback() {
    init {
        setStopped()
    }

    private val onError: (Throwable) -> Unit = {
        it.printStackTrace()
        val errorMessage = if (it.message.isNullOrBlank()) {
            "Unknown Error: ${it.javaClass.name}"
        } else {
            "Error: ${it.message}"
        }
        mediaSession.setPlaybackState(PlaybackStateCompat.Builder()
            .setState(PlaybackStateCompat.STATE_ERROR, PLAYBACK_POSITION_UNKNOWN, 1F)
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
        mediaSession.isActive = true
        mediaSession.setMetadata(mirrorMetadata)
        mediaSession.setPlaybackState(PlaybackStateCompat.Builder()
            .setState(PlaybackStateCompat.STATE_PLAYING, PLAYBACK_POSITION_UNKNOWN, 1F)
            .setActions(PlaybackStateCompat.ACTION_STOP)
            .build())
    }

    private fun setStopped() {
        mediaSession.isActive = false
        mediaSession.setMetadata(mirrorMetadata)
        mediaSession.setPlaybackState(PlaybackStateCompat.Builder()
            .setState(PlaybackStateCompat.STATE_STOPPED, PLAYBACK_POSITION_UNKNOWN, 1F)
            .setActions(PlaybackStateCompat.ACTION_PLAY)
            .build())
    }

    private val mirrorMetadata = MediaMetadataCompat.Builder()
        .putString(MediaMetadata.METADATA_KEY_TITLE, "Aux In")
        .putString(MediaMetadata.METADATA_KEY_DISPLAY_DESCRIPTION, "Auxiliary Input")
        .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, albumArt)
        .build()
}