package com.harry1453.autoaux

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.media.MediaBrowserServiceCompat

class MediaBrowserService : MediaBrowserServiceCompat() {
    override fun onCreate() {
        super.onCreate()

        val audioMirror = AudioMirror()

        val session = MediaSessionCompat(this, "AAAux").apply {
            setCallback(MediaSessionCallback(this, audioMirror))
        }

        sessionToken = session.sessionToken
    }

    override fun onLoadChildren(parentId: String, result: Result<List<MediaBrowserCompat.MediaItem>>) {
        when (parentId) {
            ROOT_ID -> result.sendResult(listOf(MEDIA_ITEM))
            else -> result.sendError(null)
        }
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? {
        // No sensitive content, don't validate calling package
        return BrowserRoot(ROOT_ID, null)
    }

    companion object {
        const val ROOT_ID = "root"
        private val MEDIA_ITEM = MediaBrowserCompat.MediaItem(
            MediaDescriptionCompat.Builder()
                .setMediaId("aux_input")
                .setTitle("Auxiliary Input")
                .build(), MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)
    }
}
