package com.harry1453.autoaux

import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.method.LinkMovementMethod
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
    }

    fun buildTransportControls() {
        val mediaController = MediaControllerCompat.getMediaController(this)
        // Grab the view for the play/pause button
        play_pause.apply {
            setOnClickListener {
                val pbState = mediaController.playbackState.state
                if (pbState == PlaybackStateCompat.STATE_PLAYING) {
                    mediaController.transportControls.stop()
                    Toast.makeText(this@MainActivity, "Stopped", Toast.LENGTH_LONG).show()
                } else {
                    if(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                        showPermissionExplanationUi()
                        return@setOnClickListener
                    }
                    mediaController.transportControls.play()
                    Toast.makeText(this@MainActivity, "Mirroring", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showPermissionExplanationUi() {
        Toast.makeText(this, "Permission to record audio is required to mirror audio", Toast.LENGTH_LONG).show()
    }

    private fun onPermissionsGranted() {
        Toast.makeText(this, "Permission Granted!", Toast.LENGTH_LONG).show()
    }

    private fun requestPermissions(showMessage: Boolean) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), PERMISSION_REQUEST_CODE)
        } else if (showMessage) {
            Toast.makeText(this, "Permission Granted!", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        request_permissions.setOnClickListener { requestPermissions(true) }

        icons8_text.movementMethod = LinkMovementMethod.getInstance()

        mediaBrowser = MediaBrowserCompat(this, ComponentName(this, MediaBrowserService::class.java), connectionCallback, null)

        requestPermissions(false)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    showPermissionExplanationUi()
                } else {
                    onPermissionsGranted()
                }
            }
            else -> {}
        }
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
        mediaBrowser.disconnect()
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1000
    }
}
