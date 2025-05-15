package com.example.criminalintent.limingxuan249400218

import android.content.Intent
import android.content.pm.PackageManager
import androidx.fragment.app.Fragment
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.net.Uri
import androidx.lifecycle.ViewModelProvider
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit
import android.os.Environment
import android.util.Log

private const val REQUEST_RECORD_AUDIO_PERMISSION = 200
private const val REQUEST_READ_EXTERNAL_STORAGE = 201
private const val REQUEST_CODE_PICK_AUDIO = 202
private const val ARG_CRIME_ID = "crime_id"
private const val REQUEST_AUDIO = 3
private const val PROGRESS_UPDATE_INTERVAL = 100L // milliseconds

class AudioFragment : Fragment() {
    private lateinit var crime: Crime
    private lateinit var recordButton: Button
    private lateinit var playButton: Button
    private lateinit var pauseButton: Button
    private lateinit var slowPlayButton: Button
    private lateinit var statusText: TextView
    private lateinit var uploadButton: Button
    private lateinit var progressBar: SeekBar
    private lateinit var currentTimeText: TextView
    private lateinit var durationText: TextView

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var audioFile: File? = null
    private var isRecording = false
    private var isPlaying = false
    private var isSlowPlaying = false
    private var currentAudioUri: Uri? = null
    private var progressHandler = Handler(Looper.getMainLooper())
    private var progressRunnable: Runnable? = null


    private val crimeDetailViewModel: CrimeDetailViewModel by lazy {
        ViewModelProvider(this).get(CrimeDetailViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_audio, container, false)

        recordButton = view.findViewById(R.id.recordButton)
        playButton = view.findViewById(R.id.playButton)
        pauseButton = view.findViewById(R.id.pauseButton)
        slowPlayButton = view.findViewById(R.id.slowPlayButton)
        statusText = view.findViewById(R.id.statusText)
        uploadButton = view.findViewById(R.id.uploadButton)
        progressBar = view.findViewById(R.id.progressBar)
        currentTimeText = view.findViewById(R.id.currentTimeText)
        durationText = view.findViewById(R.id.durationText)

        setupButtons()
        setupProgressBar()
        updateButtonStates()
        return view
    }

    private fun setupProgressBar() {
        progressBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser && mediaPlayer != null) {
                    mediaPlayer?.seekTo(progress)
                    updateTimeText(progress, mediaPlayer?.duration ?: 0)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                // Pause updates while user is dragging
                progressRunnable?.let { progressHandler.removeCallbacks(it) }
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                // Resume updates after user stops dragging
                startProgressUpdates()
            }
        })
    }

    private fun setupButtons() {
        recordButton.setOnClickListener {
            if (isRecording) {
                stopRecording()
                updateUIForRecordingStopped()
            } else {
                if (checkRecordPermission()) {
                    startRecording()
                    updateUIForRecordingStarted()
                } else {
                    requestRecordPermission()
                }
            }
            isRecording = !isRecording
        }

        playButton.setOnClickListener {
            if (isPlaying) {
                stopPlaying()
                updateUIForPlaybackStopped()
            } else {
                if (currentAudioUri != null || audioFile != null) {
                    if (startPlaying(false)) {
                        updateUIForPlaybackStarted(false)
                    } else {
                        showToast(getString(R.string.audio_playback_failed))
                    }
                } else {
                    showToast(getString(R.string.audio_no_file_to_play))
                }
            }
            isPlaying = !isPlaying
        }

        slowPlayButton.setOnClickListener {
            if (isSlowPlaying) {
                stopPlaying()
                updateUIForPlaybackStopped()
            } else {
                if (currentAudioUri != null || audioFile != null) {
                    if (startPlaying(true)) {
                        updateUIForPlaybackStarted(true)
                    } else {
                        showToast(getString(R.string.audio_playback_failed))
                    }
                } else {
                    showToast(getString(R.string.audio_no_file_to_play))
                }
            }
            isSlowPlaying = !isSlowPlaying
        }

        pauseButton.setOnClickListener {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                    pauseButton.text = getString(R.string.audio_resume)
                    statusText.text = getString(R.string.audio_status_paused)
                    stopProgressUpdates()
                } else {
                    it.start()
                    pauseButton.text = getString(R.string.audio_pause)
                    statusText.text = if (isSlowPlaying) {
                        getString(R.string.audio_status_slow_playing)
                    } else {
                        getString(R.string.audio_status_playing)
                    }
                    startProgressUpdates()
                }
            }
        }

        uploadButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // 使用统一的权限请求码
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    REQUEST_READ_EXTERNAL_STORAGE // 改为使用预定义的常量
                )
            } else {
                openFilePicker()
            }
            }
    }


    private fun startProgressUpdates() {
        progressRunnable = object : Runnable {
            override fun run() {
                mediaPlayer?.let { player ->
                    val currentPosition = player.currentPosition
                    val duration = player.duration
                    progressBar.progress = currentPosition
                    progressBar.max = duration
                    updateTimeText(currentPosition, duration)
                }
                progressHandler.postDelayed(this, PROGRESS_UPDATE_INTERVAL)
            }
        }
        progressRunnable?.let { progressHandler.post(it) }
    }

    private fun stopProgressUpdates() {
        progressRunnable?.let { progressHandler.removeCallbacks(it) }
    }

    private fun updateTimeText(currentPosition: Int, duration: Int) {
        currentTimeText.text = formatTime(currentPosition)
        durationText.text = formatTime(duration)
    }

    @SuppressLint("DefaultLocale")
    private fun formatTime(milliseconds: Int): String {
        return String.format(
            "%02d:%02d",
            TimeUnit.MILLISECONDS.toMinutes(milliseconds.toLong()),
            TimeUnit.MILLISECONDS.toSeconds(milliseconds.toLong()) -
                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milliseconds.toLong()))
        )
    }

    private fun updateButtonStates() {
        val hasAudio = currentAudioUri != null || audioFile != null
        playButton.isEnabled = hasAudio
        slowPlayButton.isEnabled = hasAudio
        pauseButton.isEnabled = false
        progressBar.isEnabled = hasAudio
    }

    private fun checkRecordPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestRecordPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.RECORD_AUDIO),
            REQUEST_RECORD_AUDIO_PERMISSION
        )
    }
    private fun startRecording(): Boolean {
        return try {
            val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_MUSIC)
            audioFile = File.createTempFile(
                "AUD_${System.currentTimeMillis()}",
                ".3gp",
                storageDir
            )

            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(audioFile?.absolutePath)
                prepare()
                start()
            }
            currentAudioUri = null
            resetProgressBar()
            true
        } catch (e: IOException) {
            e.printStackTrace()
            statusText.text = getString(R.string.audio_recording_failed)
            false
        } catch (e: IllegalStateException) {
            e.printStackTrace()
            statusText.text = getString(R.string.audio_recording_failed)
            false
        }
    }

    private fun resetProgressBar() {
        progressBar.progress = 0
        progressBar.max = 100
        currentTimeText.text = "00:00"
        durationText.text = "00:00"
    }

    private fun stopRecording() {
        mediaRecorder?.apply {
            try {
                stop()
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            }
            release()
        }
        mediaRecorder = null
    }

    private fun startPlaying(slow: Boolean): Boolean {
        return try {
            mediaPlayer = MediaPlayer().apply {
                if (currentAudioUri != null) {
                    setDataSource(requireContext(), currentAudioUri!!)
                } else {
                    setDataSource(audioFile?.absolutePath ?: return false)
                }

                prepare()
                if (slow) {
                    setPlaybackParams(playbackParams.apply { speed = 0.5f })
                }

                // Initialize progress bar
                progressBar.max = duration
                durationText.text = formatTime(duration)

                start()
                pauseButton.isEnabled = true
                startProgressUpdates()

                setOnCompletionListener {
                    stopPlaying()
                    updateUIForPlaybackStopped()
                }
            }
            true
        } catch (e: IOException) {
            e.printStackTrace()
            statusText.text = getString(R.string.audio_playback_failed)
            false
        } catch (e: IllegalStateException) {
            e.printStackTrace()
            statusText.text = getString(R.string.audio_playback_failed)
            false
        }
    }

    private fun stopPlaying() {
        mediaPlayer?.apply {
            stop()
            release()
        }
        mediaPlayer = null
        pauseButton.isEnabled = false
        pauseButton.text = getString(R.string.audio_pause)
        isPlaying = false
        isSlowPlaying = false
        stopProgressUpdates()
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "audio/*"
        }
        startActivityForResult(
            Intent.createChooser(intent, getString(R.string.select_audio_file)),
            REQUEST_CODE_PICK_AUDIO
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_RECORD_AUDIO_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startRecording()
                    updateUIForRecordingStarted()
                    isRecording = true
                } else {
                    showToast(getString(R.string.audio_permission_required))
                }
            }
            REQUEST_READ_EXTERNAL_STORAGE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openFilePicker()
                } else {
                    showToast(getString(R.string.storage_permission_required))
                }
            }
        }
    }

    private fun updateUIForRecordingStarted() {
        recordButton.text = getString(R.string.audio_record_stop)
        playButton.isEnabled = false
        slowPlayButton.isEnabled = false
        pauseButton.isEnabled = false
        uploadButton.isEnabled = false
        statusText.text = getString(R.string.audio_status_recording)
    }

    private fun updateUIForRecordingStopped() {
        recordButton.text = getString(R.string.audio_record_start)
        playButton.isEnabled = true
        slowPlayButton.isEnabled = true
        uploadButton.isEnabled = true
        statusText.text = getString(R.string.audio_status_complete)
    }

    private fun updateUIForPlaybackStarted(isSlow: Boolean) {
        playButton.text = getString(R.string.audio_stop)
        slowPlayButton.text = getString(R.string.audio_stop)
        statusText.text = if (isSlow) {
            getString(R.string.audio_status_slow_playing)
        } else {
            getString(R.string.audio_status_playing)
        }
    }

    private fun updateUIForPlaybackStopped() {
        playButton.text = getString(R.string.audio_play)
        slowPlayButton.text = getString(R.string.audio_slow_play)
        statusText.text = getString(R.string.audio_status_finished)
        isPlaying = false
        isSlowPlaying = false
        progressBar.progress = 0
        currentTimeText.text = "00:00"
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun saveAudioToCrime(uri: Uri) {
        try {
            val inputStream = requireActivity().contentResolver.openInputStream(uri)
            val audioData = inputStream?.readBytes()
            inputStream?.close()

            if (audioData != null) {
                val crimeId = arguments?.getSerializable(ARG_CRIME_ID) as? UUID ?: run {
                    showToast(getString(R.string.invalid_crime_id))
                    return
                }

                crimeDetailViewModel.getCrime(crimeId).observe(viewLifecycleOwner) { crime ->
                    crime?.let {
                        it.audioData = audioData
                        crimeDetailViewModel.saveCrime(it)
                        currentAudioUri = uri
                        audioFile = null
                        updateButtonStates()
                        showToast(getString(R.string.audio_file_saved))
                    } ?: showToast(getString(R.string.crime_not_found))
                }
            } else {
                showToast(getString(R.string.audio_file_failed))
            }
        } catch (e: Exception) {
            Log.e("AudioFragment", "Error reading audio file", e)
            showToast(getString(R.string.audio_file_save_failed))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_PICK_AUDIO -> {
                if (resultCode == Activity.RESULT_OK) {
                    data?.data?.let { uri ->
                        val mimeType = requireContext().contentResolver.getType(uri)
                        if (mimeType?.startsWith("audio/") == true) {
                            saveAudioToCrime(uri)
                        } else {
                            showToast(getString(R.string.select_audio_only))
                        }
                    } ?: showToast(getString(R.string.no_file_selected))
                }
            }
        }
    }




    override fun onStop() {
        super.onStop()
        mediaRecorder?.release()
        mediaRecorder = null
        mediaPlayer?.release()
        mediaPlayer = null
        stopProgressUpdates()
    }

    companion object {
        fun newInstance(crimeId: UUID): AudioFragment {
            val args = Bundle().apply {
                putSerializable(ARG_CRIME_ID, crimeId)
            }
            return AudioFragment().apply {
                arguments = args
            }
        }
    }
}