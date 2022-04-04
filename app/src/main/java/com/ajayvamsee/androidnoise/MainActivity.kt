package com.ajayvamsee.androidnoise

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.github.squti.androidwaverecorder.RecorderState
import com.github.squti.androidwaverecorder.WaveRecorder
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private val PERMISSIONS_REQUEST_RECORD_AUDIO = 77
    private lateinit var waveRecorder: WaveRecorder
    private lateinit var filePath: String
    private var isRecording = false
    private var isPaused = false
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        filePath = externalCacheDir?.absolutePath + "/audioFile.wav"

        waveRecorder = WaveRecorder(filePath)

        // media player installation
        mediaPlayer = MediaPlayer()

        waveRecorder.onStateChangeListener = {
            when (it) {
                RecorderState.RECORDING -> startRecording()
                RecorderState.STOP -> stopRecording()
                RecorderState.PAUSE -> pauseRecording()
            }
        }
        waveRecorder.onTimeElapsed = {
            Log.e(TAG, "onCreate: time elapsed $it")
            timeTextView.text = formatTimeUnit(it * 1000)
        }

        startStopRecordingButton.setOnClickListener {

            if (!isRecording) {
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.RECORD_AUDIO
                    )
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.RECORD_AUDIO),
                        PERMISSIONS_REQUEST_RECORD_AUDIO
                    )
                } else {
                    waveRecorder.startRecording()
                }
            } else {
                waveRecorder.stopRecording()
            }
        }

        pauseResumeRecordingButton.setOnClickListener {
            if (!isPaused) {
                waveRecorder.pauseRecording()
            } else {
                waveRecorder.resumeRecording()
            }
        }
        showAmplitudeSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                amplitudeTextView.text = "Amplitude : 0"
                amplitudeTextView.visibility = View.VISIBLE
                waveRecorder.onAmplitudeListener = {
                    GlobalScope.launch(Dispatchers.Main) {
                        amplitudeTextView.text = "Amplitude : $it"
                    }
                }

            } else {
                waveRecorder.onAmplitudeListener = null
                amplitudeTextView.visibility = View.GONE
            }
        }

        noiseSuppressorSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            waveRecorder.noiseSuppressorActive = isChecked
            if (isChecked)
                Toast.makeText(this, "Noise Suppressor Activated", Toast.LENGTH_SHORT).show()

        }

        playAudio.setOnClickListener {
            var fis: FileInputStream? = null
            try {
                fis = FileInputStream(filePath)
                mediaPlayer?.setDataSource(fis.fd)
                mediaPlayer?.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
                )
                mediaPlayer?.prepare()
                mediaPlayer?.start()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                if (fis != null) {
                    try {
                        fis.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }

        }

        stopAudio.setOnClickListener {
            if(mediaPlayer?.isPlaying == true){
                mediaPlayer?.stop()
                mediaPlayer?.reset()
                mediaPlayer?.release()
            }else{
                Toast.makeText(this,"No Media is Playing",Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startRecording() {
        Log.d(TAG, waveRecorder.audioSessionId.toString())
        isRecording = true
        isPaused = false
        messageTextView.visibility = View.GONE
        recordingTextView.text = "Recording..."
        recordingTextView.visibility = View.VISIBLE
        startStopRecordingButton.text = "STOP"
        pauseResumeRecordingButton.text = "PAUSE"
        pauseResumeRecordingButton.visibility = View.VISIBLE
        noiseSuppressorSwitch.isEnabled = false
    }

    private fun stopRecording() {
        isRecording = false
        isPaused = false
        recordingTextView.visibility = View.GONE
        messageTextView.visibility = View.VISIBLE
        pauseResumeRecordingButton.visibility = View.GONE
        showAmplitudeSwitch.isChecked = false
        Toast.makeText(this, "File saved at : $filePath", Toast.LENGTH_LONG).show()
        startStopRecordingButton.text = "START"
        noiseSuppressorSwitch.isEnabled = true
    }

    private fun pauseRecording() {
        recordingTextView.text = "PAUSE"
        pauseResumeRecordingButton.text = "RESUME"
        isPaused = true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSIONS_REQUEST_RECORD_AUDIO -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    waveRecorder.startRecording()
                }
                return
            }

            else -> {
            }
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }

    private fun formatTimeUnit(timeInMilliseconds: Long): String {
        return try {
            String.format(
                Locale.getDefault(),
                "%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(timeInMilliseconds),
                TimeUnit.MILLISECONDS.toSeconds(timeInMilliseconds) - TimeUnit.MINUTES.toSeconds(
                    TimeUnit.MILLISECONDS.toMinutes(timeInMilliseconds)
                )
            )
        } catch (e: Exception) {
            "00:00"
        }
    }
}