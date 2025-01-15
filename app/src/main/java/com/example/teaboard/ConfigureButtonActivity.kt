package com.example.teaboard

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.teaboard.BuildConfig
import com.example.teaboard.models.ButtonConfig
import com.example.teaboard.audio.AudioPlayer
import com.example.teaboard.audio.AudioRecorder
import com.example.teaboard.audio.AndroidAudioPlayer
import com.example.teaboard.audio.AndroidAudioRecorder
import com.example.teaboard.storage.StorageService
import com.example.teaboard.services.StorageServiceFactory
import com.example.teaboard.utils.toPlatformFile
import com.example.teaboard.utils.toFile
import com.example.teaboard.constants.ValidationConstants
import com.example.teaboard.constants.PreferencesKeys
import kotlinx.coroutines.launch
import java.io.File

class ConfigureButtonActivity : AppCompatActivity() {

    private lateinit var imagePreview: ImageView
    private lateinit var previewLabel: TextView
    private lateinit var etButtonLabel: EditText
    private lateinit var btnRecordAudio: Button
    private lateinit var btnStopRecording: Button
    private lateinit var btnPlayAudio: Button
    private lateinit var btnTakePhoto: Button
    private lateinit var btnSelectImage: Button
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button
    private lateinit var uploadingLayout: LinearLayout

    private lateinit var audioRecorder: AudioRecorder
    private lateinit var audioPlayer: AudioPlayer
    private lateinit var storageService: StorageService

    private var buttonId: Int = 0
    private var currentAudioFile: File? = null
    private var currentImageFile: File? = null
    private var currentPhotoUri: Uri? = null

    companion object {
        const val EXTRA_BUTTON_ID = "button_id"
        const val RESULT_CONFIGURED = 100
    }

    /**
     * Validate button label input
     */
    private fun validateLabel(label: String): ValidationResult {
        return when {
            label.isBlank() -> ValidationResult.Error(getString(R.string.enter_label))
            label.length > ValidationConstants.Label.MAX_LENGTH -> ValidationResult.Error("Label is too long (max ${ValidationConstants.Label.MAX_LENGTH} characters)")
            label.length < ValidationConstants.Label.MIN_LENGTH -> ValidationResult.Error("Label is too short")
            else -> ValidationResult.Success
        }
    }

    /**
     * Validate image file
     */
    private fun validateImageFile(file: File?): ValidationResult {
        return when {
            file == null -> ValidationResult.Error(getString(R.string.select_image_prompt))
            !file.exists() -> ValidationResult.Error(getString(R.string.image_file_not_exist))
            file.length() == 0L -> ValidationResult.Error("Image file is empty")
            file.length() > ValidationConstants.Image.MAX_SIZE_BYTES -> ValidationResult.Error("Image is too large (max ${ValidationConstants.Image.MAX_SIZE_BYTES / 1024 / 1024}MB)")
            else -> ValidationResult.Success
        }
    }

    /**
     * Validate audio file
     */
    private fun validateAudioFile(file: File?): ValidationResult {
        return when {
            file == null -> ValidationResult.Error(getString(R.string.record_sound_prompt))
            !file.exists() -> ValidationResult.Error(getString(R.string.audio_file_not_exist))
            file.length() < ValidationConstants.Audio.MIN_DURATION_MS -> ValidationResult.Error(getString(R.string.audio_file_empty_rerecord))
            file.length() > ValidationConstants.Audio.MAX_SIZE_BYTES -> ValidationResult.Error("Audio is too large (max ${ValidationConstants.Audio.MAX_SIZE_BYTES / 1024 / 1024}MB)")
            else -> ValidationResult.Success
        }
    }

    /**
     * Validation result sealed class
     */
    private sealed class ValidationResult {
        object Success : ValidationResult()
        data class Error(val message: String) : ValidationResult()
    }

    // Activity Result Launchers
    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (!allGranted) {
            Toast.makeText(this, getString(R.string.permissions_needed), Toast.LENGTH_LONG).show()
        }
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && currentPhotoUri != null) {
            // Mostrar la imagen tomada en el preview
            imagePreview.setImageURI(currentPhotoUri)
            // currentImageFile ya fue asignado en takePhoto(), no necesitamos reasignarlo
            if (BuildConfig.DEBUG) {
                Log.d("ConfigureButton", "Foto tomada exitosamente")
            }
        } else {
            if (BuildConfig.DEBUG) {
                Log.w("ConfigureButton", "Error al tomar foto o cancelado")
            }
            currentImageFile = null
            currentPhotoUri = null
        }
    }

    private val selectImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            imagePreview.setImageURI(it)
            // Copiar imagen a directorio local
            copyImageToLocal(it)
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLanguage(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_configure_button)

        buttonId = intent.getIntExtra(EXTRA_BUTTON_ID, 0)

        initializeViews()
        initializeServices()
        setupListeners()
        checkPermissions()
    }

    private fun initializeViews() {
        imagePreview = findViewById(R.id.imagePreview)
        previewLabel = findViewById(R.id.previewLabel)
        etButtonLabel = findViewById(R.id.etButtonLabel)
        btnRecordAudio = findViewById(R.id.btnRecordAudio)
        btnStopRecording = findViewById(R.id.btnStopRecording)
        btnPlayAudio = findViewById(R.id.btnPlayAudio)
        btnTakePhoto = findViewById(R.id.btnTakePhoto)
        btnSelectImage = findViewById(R.id.btnSelectImage)
        btnSave = findViewById(R.id.btnSave)
        btnCancel = findViewById(R.id.btnCancel)
        uploadingLayout = findViewById(R.id.uploadingLayout)

        // Initially hide play button (only show after recording)
        btnPlayAudio.visibility = View.GONE
    }

    private fun initializeServices() {
        audioRecorder = AndroidAudioRecorder(this)
        audioPlayer = AndroidAudioPlayer(this)
        storageService = StorageServiceFactory.create(this)

        // Initialize Drive service only if sync is enabled
        val syncEnabled = getSharedPreferences("TeaBoardPrefs", MODE_PRIVATE)
            .getBoolean(PreferencesKeys.Sync.SYNC_ENABLED, PreferencesKeys.Defaults.SYNC_ENABLED)

        if (syncEnabled) {
            lifecycleScope.launch {
                try {
                    storageService.initialize()
                } catch (e: Exception) {
                    Toast.makeText(this@ConfigureButtonActivity, getString(R.string.error_initializing_drive, e.message), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupListeners() {
        btnTakePhoto.setOnClickListener {
            takePhoto()
        }

        btnSelectImage.setOnClickListener {
            selectImageLauncher.launch("image/*")
        }

        btnRecordAudio.setOnClickListener {
            startRecording()
        }

        btnStopRecording.setOnClickListener {
            stopRecording()
        }

        btnPlayAudio.setOnClickListener {
            playAudio()
        }

        btnSave.setOnClickListener {
            saveConfiguration()
        }

        btnCancel.setOnClickListener {
            finish()
        }

        // Update preview label in real-time as user types
        etButtonLabel.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                previewLabel.text = s?.toString()?.takeIf { it.isNotEmpty() } ?: getString(R.string.button_1)
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun checkPermissions() {
        val permissions = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECORD_AUDIO)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.CAMERA)
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissions.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissions.toTypedArray())
        }
    }

    private fun takePhoto() {
        val photoFile = File(filesDir, "images").apply {
            if (!exists()) mkdirs()
        }
        val imageFile = File(photoFile, "button_${buttonId}_${System.currentTimeMillis()}.jpg")

        currentPhotoUri = FileProvider.getUriForFile(
            this,
            "${applicationContext.packageName}.fileprovider",
            imageFile
        )
        currentImageFile = imageFile
        currentPhotoUri?.let { uri ->
            takePictureLauncher.launch(uri)
        }
    }

    private fun copyImageToLocal(uri: Uri) {
        try {
            val imageDir = File(filesDir, "images")
            if (!imageDir.exists()) {
                imageDir.mkdirs()
            }

            val imageFile = File(imageDir, "button_${buttonId}_${System.currentTimeMillis()}.jpg")
            contentResolver.openInputStream(uri)?.use { input ->
                imageFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            currentImageFile = imageFile
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.error_copying_image, e.message), Toast.LENGTH_SHORT).show()
        }
    }

    private fun startRecording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, getString(R.string.audio_permission_needed), Toast.LENGTH_SHORT).show()
            return
        }

        val audioFile = audioRecorder.startRecording(buttonId)?.toFile()
        if (audioFile != null) {
            currentAudioFile = audioFile
            btnRecordAudio.visibility = View.GONE
            btnStopRecording.visibility = View.VISIBLE
            Toast.makeText(this, getString(R.string.recording_started), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, getString(R.string.error_starting_recording), Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopRecording() {
        val audioFile = audioRecorder.stopRecording()?.toFile()
        if (audioFile != null && audioFile.exists()) {
            lifecycleScope.launch {
                try {
                    val trimmedFile = audioRecorder.trimSilence(audioFile.toPlatformFile())?.toFile()

                    runOnUiThread {
                        if (trimmedFile != null && trimmedFile.exists()) {
                            currentAudioFile = trimmedFile
                            btnRecordAudio.visibility = View.VISIBLE
                            btnStopRecording.visibility = View.GONE
                            btnPlayAudio.visibility = View.VISIBLE
                            btnPlayAudio.isEnabled = true
                            Toast.makeText(this@ConfigureButtonActivity, getString(R.string.recording_completed), Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@ConfigureButtonActivity, getString(R.string.error_stopping_recording), Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    if (BuildConfig.DEBUG) {
                        Log.e("ConfigureButton", "Error trimming audio", e)
                    }
                    runOnUiThread {
                        // If trimming fails, keep the original file
                        currentAudioFile = audioFile
                        btnRecordAudio.visibility = View.VISIBLE
                        btnStopRecording.visibility = View.GONE
                        btnPlayAudio.visibility = View.VISIBLE
                        btnPlayAudio.isEnabled = true
                        Toast.makeText(this@ConfigureButtonActivity, getString(R.string.recording_completed), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            Toast.makeText(this, getString(R.string.error_stopping_recording), Toast.LENGTH_SHORT).show()
        }
    }

    private fun playAudio() {
        currentAudioFile?.let { audioFile ->
            if (!audioFile.exists()) {
                Toast.makeText(this, getString(R.string.audio_file_not_exist), Toast.LENGTH_SHORT).show()
                return
            }

            if (audioFile.length() == 0L) {
                Toast.makeText(this, getString(R.string.audio_file_empty), Toast.LENGTH_SHORT).show()
                return
            }

            audioPlayer.playAudio(
                audioPath = audioFile.absolutePath,
                onComplete = {
                    runOnUiThread {
                        Toast.makeText(this, getString(R.string.playback_completed), Toast.LENGTH_SHORT).show()
                    }
                },
                onError = { error ->
                    runOnUiThread {
                        Toast.makeText(this, getString(R.string.error_playing_audio, error), Toast.LENGTH_LONG).show()
                    }
                }
            )
        } ?: run {
            Toast.makeText(this, getString(R.string.no_audio), Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveConfiguration() {
        val label = etButtonLabel.text.toString().trim()

        // Validate label
        when (val result = validateLabel(label)) {
            is ValidationResult.Error -> {
                Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
                return
            }
            ValidationResult.Success -> {}
        }

        // Validate image
        when (val result = validateImageFile(currentImageFile)) {
            is ValidationResult.Error -> {
                Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
                return
            }
            ValidationResult.Success -> {}
        }

        // Validate audio
        when (val result = validateAudioFile(currentAudioFile)) {
            is ValidationResult.Error -> {
                Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
                // Reset audio state if file is invalid
                if (currentAudioFile?.exists() == true && currentAudioFile?.length() == 0L) {
                    currentAudioFile = null
                    btnPlayAudio.visibility = View.GONE
                    btnPlayAudio.isEnabled = false
                }
                return
            }
            ValidationResult.Success -> {}
        }

        // Show confirmation dialog with preview
        showConfirmationDialog(label)
    }

    /**
     * Show confirmation dialog with preview before saving
     */
    private fun showConfirmationDialog(label: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_confirm_button_config, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        // Set up preview elements
        val tvPreviewLabel = dialogView.findViewById<android.widget.TextView>(R.id.tvPreviewLabel)
        val imgPreview = dialogView.findViewById<android.widget.ImageView>(R.id.imgPreview)
        val btnPreviewAudio = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnPreviewAudio)
        val btnEditMore = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnEditMore)
        val btnConfirm = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnConfirm)

        // Load preview data
        tvPreviewLabel.text = label

        currentImageFile?.let { imageFile ->
            com.bumptech.glide.Glide.with(this)
                .load(imageFile)
                .into(imgPreview)
        }

        // Audio preview button
        btnPreviewAudio.setOnClickListener {
            currentAudioFile?.let { audioFile ->
                audioPlayer.playAudio(audioFile.absolutePath)
            }
        }

        // Edit more button - dismiss dialog and return to editing
        btnEditMore.setOnClickListener {
            dialog.dismiss()
        }

        // Confirm button - proceed with save
        btnConfirm.setOnClickListener {
            dialog.dismiss()
            performSave(label)
        }

        dialog.show()
    }

    /**
     * Perform the actual save operation after confirmation
     */
    private fun performSave(label: String) {
        // Mostrar indicador de carga
        uploadingLayout.visibility = View.VISIBLE
        btnSave.isEnabled = false

        lifecycleScope.launch {
            try {
                if (BuildConfig.DEBUG) {
                    Log.d("ConfigureButton", "Starting save operation for button $buttonId")
                }

                // Subir archivos a Google Drive
                val driveImageId = currentImageFile?.let { storageService.uploadImage(buttonId, it.toPlatformFile()) } ?: ""
                val driveAudioId = currentAudioFile?.let { storageService.uploadAudio(buttonId, it.toPlatformFile()) } ?: ""

                // Crear configuración del botón
                val config = ButtonConfig(
                    buttonId = buttonId,
                    label = label,
                    imageUrl = "", // No longer used
                    audioUrl = "", // No longer used
                    imagePath = currentImageFile?.absolutePath ?: "",
                    audioPath = currentAudioFile?.absolutePath ?: "",
                    driveImageId = driveImageId,
                    driveAudioId = driveAudioId
                )

                // Guardar configuración en Google Drive
                storageService.saveButtonConfig(config)

                if (BuildConfig.DEBUG) {
                    Log.d("ConfigureButton", "Configuration saved successfully")
                }

                runOnUiThread {
                    Toast.makeText(this@ConfigureButtonActivity, getString(R.string.config_saved), Toast.LENGTH_SHORT).show()
                    setResult(RESULT_CONFIGURED)
                    finish()
                }
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    Log.e("ConfigureButton", "Error saving configuration", e)
                }
                runOnUiThread {
                    uploadingLayout.visibility = View.GONE
                    btnSave.isEnabled = true
                    Toast.makeText(this@ConfigureButtonActivity, getString(R.string.save_error, e.message), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioPlayer.stopAudio()
        if (audioRecorder.isRecording()) {
            audioRecorder.cancelRecording()
        }
    }
}
