package com.harry1453.autoaux

import android.media.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        feedbackAudio()
                .subscribeOn(Schedulers.io())
                .subscribe({ Log.e("AAA", "Complete") }, { Log.e("AAA", "Error", it) })
    }

    private fun feedbackAudio(): Completable {
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
