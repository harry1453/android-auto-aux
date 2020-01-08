package com.harry1453.autoaux

import android.media.*
import android.util.Log
import io.reactivex.Completable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

/**
 * A class that takes microphone input and mirrors it to the device's audio output
 */
class AudioMirror {
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
            if (disposable != null) {
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

    private fun mirror(): Completable {
        return Completable.create { emitter ->
            Log.e("AAA", "Starting")
            val buffer = ByteArray(bufferSize)
            val audioRecord = AudioRecord(audioSource, sampleRate, channelConfig, encoding, bufferSize)
            try {
                val audioTrack = AudioTrack(audioAttributes, audioFormat, bufferSize, AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE)
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
                    audioTrack.release()
                }
            } finally {
                audioRecord.release()
                Log.e("AAA", "Done")
            }
        }
    }

    private val sampleRate = 96000

    private val audioSource = MediaRecorder.AudioSource.UNPROCESSED
    private val channelConfig = AudioFormat.CHANNEL_IN_STEREO
    private val encoding = AudioFormat.ENCODING_PCM_16BIT

    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, encoding)
        .apply {
            if (this <= 0) error("Error getting min buffer size: $this")
        }

    private val audioAttributes = AudioAttributes.Builder()
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .build()

    private val audioFormat = AudioFormat.Builder()
        .setEncoding(encoding)
        .setSampleRate(sampleRate)
        .build()
}