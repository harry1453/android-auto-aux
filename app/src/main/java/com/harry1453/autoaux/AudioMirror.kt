package com.harry1453.autoaux

import android.Manifest
import android.content.Context
import android.content.Context.AUDIO_SERVICE
import android.content.pm.PackageManager
import android.media.*
import android.util.Log
import androidx.core.content.ContextCompat
import io.reactivex.Completable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import android.media.AudioDeviceInfo
import android.media.AudioManager


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

    private fun mirror(): Completable {
        return Completable.create { emitter ->
            check(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) { "Permission to Record Audio Not Granted" }
            Log.e("AAA", "Starting")
            val buffer = ByteArray(bufferSize)
            val audioRecord = AudioRecord(audioSource, sampleRate, AudioFormat.CHANNEL_IN_STEREO, encoding, bufferSize)
            try {
                val audioTrack = AudioTrack(audioAttributes, audioFormat, bufferSize, AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE)
                try {
                    /*
                    val manager = context.getSystemService(AUDIO_SERVICE) as AudioManager
                    manager.mode = AudioManager.MODE_NORMAL

                    val inputDevice = manager.getDevices(AudioManager.GET_DEVICES_INPUTS).find { deviceInfo ->
                        when(deviceInfo.type) {
                            AudioDeviceInfo.TYPE_AUX_LINE -> true
                            else -> false
                        }
                    }
                    val outputDevice = manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).find { deviceInfo ->
                        when(deviceInfo.type) {
                            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_USB_ACCESSORY -> true
                            else -> false
                        }
                    }

                    if (inputDevice != null) audioRecord.preferredDevice = inputDevice
                    if (outputDevice != null) audioTrack.preferredDevice = outputDevice
                    */

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