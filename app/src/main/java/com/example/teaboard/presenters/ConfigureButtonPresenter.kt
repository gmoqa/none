package com.example.teaboard.presenters

import android.content.Context
import android.net.Uri
import com.example.teaboard.models.ButtonConfig
import com.example.teaboard.storage.StorageService
import com.example.teaboard.audio.AudioPlayer
import com.example.teaboard.audio.AudioRecorder
import com.example.teaboard.utils.toPlatformFile
import com.example.teaboard.utils.toFile
import com.example.teaboard.constants.ValidationConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * View contract for ConfigureButtonActivity
 */
interface ConfigureButtonView {
    fun showRecordingStarted()
    fun showRecordingCompleted()
    fun showRecordingStopped()
    fun showRecordingError()
    fun showPlaybackCompleted()
    fun showPlaybackError(message: String)
    fun showNoAudioMessage()
    fun showAudioFileNotExist()
    fun showAudioFileEmpty()
    fun showAudioPermissionNeeded()
    fun showValidationError(message: String)
    fun showConfigSaved()
    fun showSaveError(message: String)
    fun showImageCopyError(message: String)
    fun showConfirmationDialog(label: String)
    fun updateRecordingUI(isRecording: Boolean)
    fun updatePlayButtonVisibility(visible: Boolean, enabled: Boolean)
    fun showUploadingIndicator(visible: Boolean)
    fun enableSaveButton(enabled: Boolean)
    fun finishWithResult()
    fun getFilesDir(): File
    fun getContentResolverInputStream(uri: Uri): java.io.InputStream?
}

/**
 * Validation result sealed class
 */
sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val message: String) : ValidationResult()
}

/**
 * Presenter for ConfigureButtonActivity - Contains all business logic
 * Platform-independent and ready for KMP migration
 */
class ConfigureButtonPresenter(
    private val view: ConfigureButtonView,
    private val storageService: StorageService,
    private val audioRecorder: AudioRecorder,
    private val audioPlayer: AudioPlayer,
    private val context: Context, // Will be replaced with expect/actual in KMP
    private val coroutineScope: CoroutineScope
) {
    // Validation constants now sourced from shared module ValidationConstants

    private var buttonId: Int = 0
    private var currentAudioFile: File? = null
    private var currentImageFile: File? = null

    /**
     * Initialize presenter with button ID
     */
    fun initialize(buttonId: Int) {
        this.buttonId = buttonId
    }

    /**
     * Validate button label input
     */
    fun validateLabel(label: String, errorMessages: LabelErrors): ValidationResult {
        return when {
            label.isBlank() -> ValidationResult.Error(errorMessages.enterLabel)
            label.length > ValidationConstants.Label.MAX_LENGTH -> ValidationResult.Error("Label is too long (max ${ValidationConstants.Label.MAX_LENGTH} characters)")
            label.length < ValidationConstants.Label.MIN_LENGTH -> ValidationResult.Error("Label is too short")
            else -> ValidationResult.Success
        }
    }

    /**
     * Validate image file
     */
    fun validateImageFile(file: File?, errorMessages: ImageErrors): ValidationResult {
        return when {
            file == null -> ValidationResult.Error(errorMessages.selectImagePrompt)
            !file.exists() -> ValidationResult.Error(errorMessages.imageFileNotExist)
            file.length() == 0L -> ValidationResult.Error("Image file is empty")
            file.length() > ValidationConstants.Image.MAX_SIZE_BYTES -> ValidationResult.Error("Image is too large (max ${ValidationConstants.Image.MAX_SIZE_BYTES / 1024 / 1024}MB)")
            else -> ValidationResult.Success
        }
    }

    /**
     * Validate audio file
     */
    fun validateAudioFile(file: File?, errorMessages: AudioErrors): ValidationResult {
        return when {
            file == null -> ValidationResult.Error(errorMessages.recordSoundPrompt)
            !file.exists() -> ValidationResult.Error(errorMessages.audioFileNotExist)
            file.length() < ValidationConstants.Audio.MIN_DURATION_MS -> ValidationResult.Error(errorMessages.audioFileEmptyRerecord)
            file.length() > ValidationConstants.Audio.MAX_SIZE_BYTES -> ValidationResult.Error("Audio is too large (max ${ValidationConstants.Audio.MAX_SIZE_BYTES / 1024 / 1024}MB)")
            else -> ValidationResult.Success
        }
    }

    /**
     * Start recording audio
     */
    fun startRecording(hasPermission: Boolean) {
        if (!hasPermission) {
            view.showAudioPermissionNeeded()
            return
        }

        val audioFile = audioRecorder.startRecording(buttonId)?.toFile()
        if (audioFile != null) {
            currentAudioFile = audioFile
            view.updateRecordingUI(isRecording = true)
            view.showRecordingStarted()
        } else {
            view.showRecordingError()
        }
    }

    /**
     * Stop recording audio
     */
    fun stopRecording() {
        val audioFile = audioRecorder.stopRecording()?.toFile()
        if (audioFile != null && audioFile.exists()) {
            coroutineScope.launch {
                try {
                    val trimmedFile = audioRecorder.trimSilence(audioFile.toPlatformFile())?.toFile()

                    withContext(Dispatchers.Main) {
                        if (trimmedFile != null && trimmedFile.exists()) {
                            currentAudioFile = trimmedFile
                            view.updateRecordingUI(isRecording = false)
                            view.updatePlayButtonVisibility(visible = true, enabled = true)
                            view.showRecordingCompleted()
                        } else {
                            view.showRecordingError()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        // If trimming fails, keep the original file
                        currentAudioFile = audioFile
                        view.updateRecordingUI(isRecording = false)
                        view.updatePlayButtonVisibility(visible = true, enabled = true)
                        view.showRecordingCompleted()
                    }
                }
            }
        } else {
            view.showRecordingError()
        }
    }

    /**
     * Play recorded audio
     */
    fun playAudio() {
        currentAudioFile?.let { audioFile ->
            if (!audioFile.exists()) {
                view.showAudioFileNotExist()
                return
            }

            if (audioFile.length() == 0L) {
                view.showAudioFileEmpty()
                return
            }

            audioPlayer.playAudio(
                audioPath = audioFile.absolutePath,
                onComplete = {
                    view.showPlaybackCompleted()
                },
                onError = { error ->
                    view.showPlaybackError(error)
                }
            )
        } ?: run {
            view.showNoAudioMessage()
        }
    }

    /**
     * Set current image file (after photo taken)
     */
    fun setCurrentImageFile(file: File) {
        currentImageFile = file
    }

    /**
     * Copy image from URI to local storage
     */
    fun copyImageToLocal(uri: Uri) {
        try {
            val imageDir = File(view.getFilesDir(), "images")
            if (!imageDir.exists()) {
                imageDir.mkdirs()
            }

            val imageFile = File(imageDir, "button_${buttonId}_${System.currentTimeMillis()}.jpg")
            view.getContentResolverInputStream(uri)?.use { input ->
                imageFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            currentImageFile = imageFile
        } catch (e: Exception) {
            view.showImageCopyError(e.message ?: "Unknown error")
        }
    }

    /**
     * Validate all fields and initiate save if valid
     */
    fun saveConfiguration(label: String, errorMessages: ValidationErrorMessages) {
        // Validate label
        when (val result = validateLabel(label, errorMessages.labelErrors)) {
            is ValidationResult.Error -> {
                view.showValidationError(result.message)
                return
            }
            ValidationResult.Success -> {}
        }

        // Validate image
        when (val result = validateImageFile(currentImageFile, errorMessages.imageErrors)) {
            is ValidationResult.Error -> {
                view.showValidationError(result.message)
                return
            }
            ValidationResult.Success -> {}
        }

        // Validate audio
        when (val result = validateAudioFile(currentAudioFile, errorMessages.audioErrors)) {
            is ValidationResult.Error -> {
                view.showValidationError(result.message)
                // Reset audio state if file is invalid
                if (currentAudioFile?.exists() == true && currentAudioFile?.length() == 0L) {
                    currentAudioFile = null
                    view.updatePlayButtonVisibility(visible = false, enabled = false)
                }
                return
            }
            ValidationResult.Success -> {}
        }

        // Show confirmation dialog
        view.showConfirmationDialog(label)
    }

    /**
     * Perform the actual save operation after confirmation
     */
    fun performSave(label: String) {
        view.showUploadingIndicator(true)
        view.enableSaveButton(false)

        coroutineScope.launch {
            try {
                // Upload files to Google Drive
                val driveImageId = currentImageFile?.let { storageService.uploadImage(buttonId, it.toPlatformFile()) } ?: ""
                val driveAudioId = currentAudioFile?.let { storageService.uploadAudio(buttonId, it.toPlatformFile()) } ?: ""

                // Create button configuration
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

                // Save configuration to Google Drive
                storageService.saveButtonConfig(config)

                withContext(Dispatchers.Main) {
                    view.showConfigSaved()
                    view.finishWithResult()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    view.showUploadingIndicator(false)
                    view.enableSaveButton(true)
                    view.showSaveError(e.message ?: "Unknown error")
                }
            }
        }
    }

    /**
     * Get current audio file for preview
     */
    fun getCurrentAudioFile(): File? = currentAudioFile

    /**
     * Get current image file for preview
     */
    fun getCurrentImageFile(): File? = currentImageFile

    /**
     * Clean up resources
     */
    fun onDestroy() {
        audioPlayer.stopAudio()
        if (audioRecorder.isRecording()) {
            audioRecorder.cancelRecording()
        }
    }
}

/**
 * Error messages container for validation
 */
data class ValidationErrorMessages(
    val labelErrors: LabelErrors,
    val imageErrors: ImageErrors,
    val audioErrors: AudioErrors
)

data class LabelErrors(
    val enterLabel: String
)

data class ImageErrors(
    val selectImagePrompt: String,
    val imageFileNotExist: String
)

data class AudioErrors(
    val recordSoundPrompt: String,
    val audioFileNotExist: String,
    val audioFileEmptyRerecord: String
)
