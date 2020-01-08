package com.harry1453.autoaux

import android.content.ComponentName
import android.media.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.widget.ImageView
import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private lateinit var mediaBrowser: MediaBrowserCompat

    private val connectionCallback = object : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            mediaBrowser.sessionToken.also { token ->
                val mediaController = MediaControllerCompat(this@MainActivity, token)
                MediaControllerCompat.setMediaController(this@MainActivity, mediaController)
            }
            buildTransportControls()
        }

        override fun onConnectionSuspended() {
            // The Service has crashed. Disable transport controls until it automatically reconnects
        }

        override fun onConnectionFailed() {
            // The Service has refused our connection
        }
    }

    private var controllerCallback = object : MediaControllerCompat.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {}
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {}
    }

    fun buildTransportControls() {
        val mediaController = MediaControllerCompat.getMediaController(this)
        // Grab the view for the play/pause button
        play_pause.apply {
            setOnClickListener {
                val pbState = mediaController.playbackState.state
                if (pbState == PlaybackStateCompat.STATE_PLAYING) {
                    mediaController.transportControls.stop()
                } else {
                    mediaController.transportControls.play()
                }
            }
        }

        // Display the initial state
        val metadata = mediaController.metadata
        val pbState = mediaController.playbackState

        // Register a Callback to stay in sync
        mediaController.registerCallback(controllerCallback)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mediaBrowser = MediaBrowserCompat(this, ComponentName(this, MediaBrowserService::class.java), connectionCallback, null)
    }

    override fun onStart() {
        super.onStart()
        mediaBrowser.connect()
    }

    override fun onResume() {
        super.onResume()
        volumeControlStream = AudioManager.STREAM_MUSIC
    }

    override fun onStop() {
        super.onStop()
        MediaControllerCompat.getMediaController(this)?.unregisterCallback(controllerCallback)
        mediaBrowser.disconnect()
    }
}
