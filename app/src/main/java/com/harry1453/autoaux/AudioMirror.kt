package com.harry1453.autoaux

import android.Manifest
import android.content.Context
import android.content.Context.AUDIO_SERVICE
import android.content.pm.PackageManager
import android.media.*
import android.media.AudioManager.AUDIOFOCUS_REQUEST_GRANTED
import android.util.Log
import androidx.core.content.ContextCompat
import io.reactivex.Completable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers


/**
 * A class that takes microphone input and mirrors it to the device's audio output
 */
class AudioMirror(private val context: Context) {
    private var disposable: Disposable? = null
    private val lock = Any()

    private fun startMirroring(onError: (Throwable) -> Unit): Disposable = mirror()
            .subscribeOn(Schedulers.io())
            .subscribe(onComplete, onError)

    private val onComplete: () -> Unit = {
        synchronized(lock) {
            disposable = null
        }
    }

    fun start(onError: (Throwable) -> Unit) {
        synchronized(lock) {
            if (disposable == null) {
                disposable = startMirroring(onError)
            }
        }
    }

    fun stop() {
        synchronized(lock) {
            disposable?.dispose()
            disposable = null
        }
    }

    private fun getAudioFocus(): AudioFocusRequest {
        val audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).build()
        val manager = context.getSystemService(AUDIO_SERVICE) as AudioManager
        check(manager.requestAudioFocus(audioFocusRequest) == AUDIOFOCUS_REQUEST_GRANTED)
        return audioFocusRequest
    }

    private fun AudioFocusRequest.release() {
        val manager = context.getSystemService(AUDIO_SERVICE) as AudioManager
        manager.abandonAudioFocusRequest(this)
    }

    private fun mirror(): Completable {
        return Completable.create { emitter ->
            check(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) { "Permission to Record Audio Not Granted" }
            Log.e("AAA", "Starting")
            val buffer = ByteArray(bufferSize)
            val audioRecord = AudioRecord(audioSource, sampleRate, AudioFormat.CHANNEL_IN_STEREO, encoding, bufferSize)
            try {
                val audioTrack = AudioTrack(audioAttributes, audioFormat, bufferSize, AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE)
                val audioFocus = getAudioFocus()
                try {
                    audioRecord.startRecording()
                    audioTrack.play()

                    Log.e("AAA", "Begin loop")
                    while (!emitter.isDisposed) {
                        val readSamples = audioRecord.read(buffer, 0, buffer.size)
                        if (readSamples > 0) {
                            val writeSamples = audioTrack.write(buffer, 0, readSamples)
                            if (writeSamples <= 0) {
                                Log.e("AAA", "writeSamples was $writeSamples")
                            }
                        } else {
                            Log.e("AAA", "readSamples was $readSamples")
                        }
                    }
                } finally {
                    audioFocus.release()
                    audioTrack.release()
                }
            } finally {
                audioRecord.release()
                Log.e("AAA", "Done")
            }
        }
    }

    private val sampleRate = 48000

    private val audioSource = MediaRecorder.AudioSource.UNPROCESSED
    private val encoding = AudioFormat.ENCODING_PCM_16BIT

    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_STEREO, encoding)
        .also {
            if (it <= 0) error("Error getting min buffer size: $it")
        }

    private val audioAttributes = AudioAttributes.Builder()
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .build()

    private val audioFormat = AudioFormat.Builder()
        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
        .setEncoding(encoding)
        .setSampleRate(sampleRate)
        .build()
}