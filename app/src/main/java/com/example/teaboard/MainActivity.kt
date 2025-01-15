package com.example.teaboard

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.google.android.material.button.MaterialButton
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.teaboard.models.ButtonConfig
import com.example.teaboard.audio.AudioPlayer
import com.example.teaboard.audio.AndroidAudioPlayer
import com.example.teaboard.storage.StorageService
import com.example.teaboard.services.StorageServiceFactory
import com.example.teaboard.utils.toFile
import com.example.teaboard.utils.toPlatformFile
import com.example.teaboard.constants.PreferencesKeys
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var audioPlayer: AudioPlayer
    private lateinit var storageService: StorageService

    private var isEditMode = false
    private var buttonConfigs = listOf<ButtonConfig>()

    // ViewPager2 components for pagination
    private lateinit var viewPager: androidx.viewpager2.widget.ViewPager2
    private lateinit var pagerAdapter: com.example.teaboard.adapters.ButtonPageAdapter
    private lateinit var fabAddButton: com.google.android.material.floatingactionbutton.FloatingActionButton
    private lateinit var fabSettingsHidden: com.google.android.material.floatingactionbutton.FloatingActionButton
    private lateinit var btnExitEditMode: MaterialButton

    private var settingsIconTapCount = 0
    private var lastTapTime = 0L

    companion object {
        private const val REQUEST_CONFIGURE_BUTTON = 101
        private const val REQUEST_SETTINGS = 102
        private const val MAX_BUTTONS = 12  // TODO: Consider migrating to shared constant

        // Animation constants
        private const val CARD_ANIMATION_DURATION_MS = 400L
        private const val CARD_ANIMATION_DELAY_MS = 80L
        private const val CARD_ANIMATION_OVERSHOOT = 1.2f
        private const val CARD_ANIMATION_INITIAL_SCALE = 0.8f

        // Button press animation constants
        private const val BUTTON_PRESS_SCALE = 0.92f
        private const val BUTTON_PRESS_DURATION_MS = 50L

        // Settings access constants
        private const val SETTINGS_TAP_COUNT_REQUIRED = 3
        private const val SETTINGS_TAP_TIMEOUT_MS = 3000L

        // Edit mode constants
        private const val EDIT_MODE_STROKE_WIDTH = 8
        private const val EDIT_MODE_ELEVATION_DP = 8f

        // Image padding constants
        private const val IMAGE_PADDING_DP = 8
        private const val IMAGE_BOTTOM_PADDING_DP = 48

        // Button color cycle (extended for 12 buttons)
        // Note: First 6 colors should match ButtonConstants for consistency
        private val BUTTON_COLORS = listOf(
            "#4A90E2", "#72C604", "#B4A7D6", "#BCD19E", "#F4A582", "#8AB4D6",
            "#A8D8EA", "#FFD3B6", "#D4A5A5", "#9FD8CB", "#C5A3E0", "#F4C87A"
        )
    }

    private var currentLanguage: String = ""

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLanguage(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Configurar fullscreen edge-to-edge (después de setContentView)
        setupFullscreen()

        // Mantener pantalla encendida durante uso
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Save current language to detect changes
        currentLanguage = LocaleHelper.getSavedLanguage(this)

        initializeServices()
        initializeViews()
        setupListeners()
        loadButtonConfigurations()
    }

    override fun onResume() {
        super.onResume()
        // Check if language has changed
        val savedLanguage = LocaleHelper.getSavedLanguage(this)
        if (currentLanguage != savedLanguage) {
            // Language changed, recreate to reload default buttons with new language
            recreate()
        }
    }

    override fun onPause() {
        super.onPause()
        // Cancel all animations to prevent excessive binder traffic
        // adapter.cancelAllAnimations() // TODO: Implement for pager adapter if needed
    }

    private fun setupFullscreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ (API 30+) - Usar WindowInsetsController
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            // Android 10 y anteriores - Usar flags legacy
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            )
        }
    }

    private fun initializeServices() {
        audioPlayer = AndroidAudioPlayer(this)
        storageService = StorageServiceFactory.create(this)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Initialize Drive service only if sync is enabled
        val syncEnabled = getSharedPreferences("TeaBoardPrefs", MODE_PRIVATE)
            .getBoolean(PreferencesKeys.Sync.SYNC_ENABLED, PreferencesKeys.Defaults.SYNC_ENABLED)

        if (syncEnabled) {
            lifecycleScope.launch {
                try {
                    storageService.initialize()
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, getString(R.string.error_initializing_drive, e.message), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun initializeViews() {
        // Initialize UI components
        fabSettingsHidden = findViewById(R.id.fabSettingsHidden)
        fabAddButton = findViewById(R.id.fabAddButton)
        btnExitEditMode = findViewById(R.id.btnExitEditMode)
        viewPager = findViewById(R.id.viewPagerButtons)

        // Setup ViewPager2 with vertical orientation
        viewPager.orientation = androidx.viewpager2.widget.ViewPager2.ORIENTATION_VERTICAL

        // Initialize pager adapter with empty list (6 buttons per page)
        pagerAdapter = com.example.teaboard.adapters.ButtonPageAdapter(
            allButtons = emptyList(),
            isEditMode = false,
            onButtonClick = { config ->
                if (isEditMode) {
                    openConfigureDialog(config.buttonId)
                } else {
                    playButtonSound(config.buttonId)
                }
            },
            onDeleteClick = { config ->
                onDeleteButtonClick(config)
            }
        )
        viewPager.adapter = pagerAdapter

        // Setup Exit Edit Mode button
        btnExitEditMode.setOnClickListener {
            toggleEditMode()
        }

        // Setup Add Button FAB
        fabAddButton.setOnClickListener {
            onAddButtonClick()
        }
    }

    private fun setupListeners() {
        // Hidden settings icon requires multiple taps to access settings
        fabSettingsHidden.setOnClickListener {
            val currentTime = System.currentTimeMillis()

            // Reset counter if timeout has passed since last tap
            if (currentTime - lastTapTime > SETTINGS_TAP_TIMEOUT_MS) {
                settingsIconTapCount = 0
            }

            lastTapTime = currentTime
            settingsIconTapCount++

            if (settingsIconTapCount >= SETTINGS_TAP_COUNT_REQUIRED) {
                settingsIconTapCount = 0
                openSettings()
            } else {
                // Provide visual and haptic feedback with counter
                it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

                // Show progress: "Tap 1/3", "Tap 2/3"
                val remaining = SETTINGS_TAP_COUNT_REQUIRED - settingsIconTapCount
                Toast.makeText(
                    this,
                    "Toca $remaining vez/veces más para acceder a configuración",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        // Button listeners are now handled by the adapter
    }

    private fun toggleEditMode() {
        isEditMode = !isEditMode

        if (isEditMode) {
            Toast.makeText(this, getString(R.string.edit_mode_activated), Toast.LENGTH_LONG).show()
            // Show visual indicator that edit mode is active
            window.statusBarColor = getColor(android.R.color.holo_orange_dark)
            btnExitEditMode.visibility = View.VISIBLE
            fabAddButton.visibility = View.VISIBLE
        } else {
            Toast.makeText(this, getString(R.string.use_mode_activated), Toast.LENGTH_SHORT).show()
            // Restore normal status bar color
            window.statusBarColor = getColor(R.color.md_theme_background)
            btnExitEditMode.visibility = View.GONE
            fabAddButton.visibility = View.GONE
        }

        // Update pager adapter to reflect edit mode changes
        pagerAdapter.setEditMode(isEditMode)
    }

    private fun Float.dpToPx(): Float {
        return this * resources.displayMetrics.density
    }

    private fun openConfigureDialog(buttonId: Int) {
        val intent = Intent(this, ConfigureButtonActivity::class.java)
        intent.putExtra(ConfigureButtonActivity.EXTRA_BUTTON_ID, buttonId)
        startActivityForResult(intent, REQUEST_CONFIGURE_BUTTON)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CONFIGURE_BUTTON && resultCode == ConfigureButtonActivity.RESULT_CONFIGURED) {
            // Recargar configuraciones
            loadButtonConfigurations()
            Toast.makeText(this, getString(R.string.config_saved_short), Toast.LENGTH_SHORT).show()
        } else if (requestCode == REQUEST_SETTINGS && resultCode == SettingsActivity.RESULT_TOGGLE_EDIT) {
            // Toggle edit mode when coming back from settings
            data?.let {
                if (it.getBooleanExtra(SettingsActivity.EXTRA_TOGGLE_EDIT_MODE, false)) {
                    toggleEditMode()
                }
            }
        }
    }

    private fun loadButtonConfigurations() {
        lifecycleScope.launch {
            try {
                // Check if default buttons need to be created or recreated (language change)
                if (DefaultButtonsHelper.shouldRecreateDefaultButtons(this@MainActivity)) {
                    DefaultButtonsHelper.createDefaultButtons(this@MainActivity, storageService)
                }

                // Load all button configurations from storage
                val configs = storageService.getAllButtonConfigs()
                buttonConfigs = configs

                // Update pager adapter with new data
                pagerAdapter.updateData(configs)

                // Pre-load audio for instant playback
                configs.forEach { config ->
                    if (config.audioPath.isNotEmpty() && File(config.audioPath).exists()) {
                        audioPlayer.preloadAudio(config.audioPath)
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, getString(R.string.loading_configs_error, e.message), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkImageHasTransparency(file: File): Boolean {
        return try {
            // Use inSampleSize to reduce memory usage
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = false
                inSampleSize = 4 // Load a smaller version for transparency check
            }
            val bitmap = BitmapFactory.decodeFile(file.absolutePath, options)

            if (bitmap == null) return false

            // Check if image has alpha channel
            val hasAlpha = bitmap.hasAlpha()

            // If it has alpha channel, check if there's actual transparency
            if (hasAlpha) {
                // Sample a few pixels to check for transparency
                val width = bitmap.width
                val height = bitmap.height
                var foundTransparent = false

                // Check corners and center
                val checkPoints = listOf(
                    0 to 0,  // top-left
                    width - 1 to 0,  // top-right
                    0 to height - 1,  // bottom-left
                    width - 1 to height - 1,  // bottom-right
                    width / 2 to height / 2  // center
                )

                for ((x, y) in checkPoints) {
                    if (x < width && y < height) {
                        val pixel = bitmap.getPixel(x, y)
                        val alpha = android.graphics.Color.alpha(pixel)
                        if (alpha < 255) {
                            foundTransparent = true
                            break
                        }
                    }
                }

                bitmap.recycle()
                foundTransparent
            } else {
                false
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace()
            }
            false
        }
    }

    private fun playButtonSound(buttonId: Int) {
        val config = buttonConfigs.firstOrNull { it.buttonId == buttonId }

        if (BuildConfig.DEBUG) {
            android.util.Log.d("MainActivity", "playButtonSound called for button $buttonId")
        }

        if (config == null) {
            Toast.makeText(this, getString(R.string.configure_first), Toast.LENGTH_LONG).show()
            return
        }

        lifecycleScope.launch {
            var attempt = 0
            val maxRetries = 3
            var audioFile: File? = null

            while (attempt < maxRetries && audioFile == null) {
                try {
                    attempt++

                    audioFile = if (config.audioPath.isNotEmpty() && File(config.audioPath).exists()) {
                        File(config.audioPath)
                    } else if (config.audioUrl.isNotEmpty()) {
                        // Descargar audio si no existe localmente (con reintentos)
                        val audioDir = File(filesDir, "audio")
                        if (!audioDir.exists()) audioDir.mkdirs()

                        // Exponential backoff: 500ms, 1000ms, 2000ms
                        if (attempt > 1) {
                            kotlinx.coroutines.delay(500L * (1 shl (attempt - 1)))
                        }

                        storageService.downloadAudio(config.audioUrl, buttonId, audioDir.toPlatformFile()).toFile()
                    } else {
                        if (BuildConfig.DEBUG) {
                            android.util.Log.d("MainActivity", "No audio configured for button $buttonId")
                        }
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, getString(R.string.configure_first), Toast.LENGTH_SHORT).show()
                        }
                        null
                    }
                } catch (e: Exception) {
                    if (BuildConfig.DEBUG) {
                        android.util.Log.e("MainActivity", "Audio download attempt $attempt failed", e)
                    }

                    if (attempt >= maxRetries) {
                        // All retries failed - show retry dialog
                        runOnUiThread {
                            showAudioErrorDialog(buttonId)
                        }
                    }
                }
            }

            audioFile?.let { file ->
                runOnUiThread {
                    audioPlayer.playAudio(file.absolutePath)
                }
            }
        }
    }

    /**
     * Shows dialog when audio playback fails after all retries
     */
    private fun showAudioErrorDialog(buttonId: Int) {
        AlertDialog.Builder(this)
            .setTitle("Error de audio")
            .setMessage("No se pudo reproducir el audio. ¿Quieres volver a grabar el sonido?")
            .setPositiveButton("Volver a grabar") { _, _ ->
                openConfigureDialog(buttonId)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun openSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivityForResult(intent, REQUEST_SETTINGS)
    }

    /**
     * Get the next available button ID
     */
    private fun getNextButtonId(): Int {
        val usedIds = buttonConfigs.map { it.buttonId }.toSet()
        for (id in 1..MAX_BUTTONS) {
            if (id !in usedIds) {
                return id
            }
        }
        return -1 // No available ID
    }

    /**
     * Get color for a button based on its ID
     */
    private fun getButtonColor(buttonId: Int): String {
        val colorIndex = (buttonId - 1) % BUTTON_COLORS.size
        return BUTTON_COLORS[colorIndex]
    }

    /**
     * Handle add button click
     */
    private fun onAddButtonClick() {
        // Check if max buttons reached
        if (buttonConfigs.size >= MAX_BUTTONS) {
            Toast.makeText(this, getString(R.string.max_buttons_reached), Toast.LENGTH_LONG).show()
            return
        }

        // Get next available button ID
        val nextId = getNextButtonId()
        if (nextId == -1) {
            Toast.makeText(this, getString(R.string.max_buttons_reached), Toast.LENGTH_LONG).show()
            return
        }

        // Open configure dialog for the new button
        openConfigureDialog(nextId)
    }

    /**
     * Handle delete button click
     */
    private fun onDeleteButtonClick(config: ButtonConfig) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_button))
            .setMessage(getString(R.string.delete_confirmation))
            .setPositiveButton(android.R.string.ok) { _, _ ->
                deleteButton(config.buttonId)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    /**
     * Delete a button configuration
     */
    private fun deleteButton(buttonId: Int) {
        lifecycleScope.launch {
            try {
                // Delete from storage
                storageService.deleteButtonConfig(buttonId)

                // Reload configurations to update UI
                loadButtonConfigurations()

                Toast.makeText(this@MainActivity, "Botón eliminado", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error al eliminar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::audioPlayer.isInitialized) {
            audioPlayer.stopAudio()
            audioPlayer.releaseCache()
        }
    }
}
