package com.harry1453.autoaux

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import androidx.media.MediaBrowserServiceCompat
import android.graphics.BitmapFactory

class MediaBrowserService : MediaBrowserServiceCompat() {
    override fun onCreate() {
        super.onCreate()

        val audioMirror = AudioMirror(this)

        val session = MediaSessionCompat(this, "AAAux").apply {
            setCallback(MediaSessionCallback(this, BitmapFactory.decodeResource(resources, R.mipmap.aux_cable_light_darkbg), audioMirror))
        }

        sessionToken = session.sessionToken

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mChannel = NotificationChannel("channel", "name", NotificationManager.IMPORTANCE_DEFAULT)
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(mChannel)
        }

        startForeground(SERVICE_ID, NotificationCompat.Builder(this, "channel")
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(1)
                .setMediaSession(session.sessionToken))
            .setContentTitle("Aux Input")
            .build())
    }

    override fun onLoadChildren(parentId: String, result: Result<List<MediaBrowserCompat.MediaItem>>) {
        when (parentId) {
            ROOT_ID -> result.sendResult(listOf(mediaItem))
            else -> result.sendError(null)
        }
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? {
        // No sensitive content, don't validate calling package
        return BrowserRoot(ROOT_ID, null)
    }


    private val mediaItem by lazy {
        MediaBrowserCompat.MediaItem(MediaDescriptionCompat.Builder()
                .setIconBitmap(BitmapFactory.decodeResource(resources, R.mipmap.aux_cable_light_darkbg))
                .setMediaId("aux_input")
                .setTitle("Auxiliary Input")
                .build(), MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)
    }

    companion object {
        private const val SERVICE_ID = 1001
        private const val ROOT_ID = "root"
    }
}
