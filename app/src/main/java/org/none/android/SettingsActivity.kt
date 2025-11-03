package org.none.android

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.button.MaterialButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.card.MaterialCardView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.launch
import org.none.android.constants.PreferencesKeys
import org.none.android.services.AnalyticsService

class SettingsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SettingsAdapter
    private lateinit var btnClose: MaterialButton

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var analyticsService: AnalyticsService

    companion object {
        private const val RC_SIGN_IN = 9001
        private const val PREFS_NAME = "TeaBoardPrefs"
        // Using centralized PreferencesKeys instead of hardcoded strings
        const val EXTRA_TOGGLE_EDIT_MODE = "toggle_edit_mode"
        const val RESULT_TOGGLE_EDIT = 200
        const val RESULT_LANGUAGE_CHANGED = 201
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLanguage(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        analyticsService = AnalyticsService.getInstance(this)

        // Log settings screen opened
        analyticsService.logSettingsOpened()
        analyticsService.logScreenView("Settings", "SettingsActivity")

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .requestScopes(
                com.google.android.gms.common.api.Scope(DriveScopes.DRIVE_FILE),
                com.google.android.gms.common.api.Scope(DriveScopes.DRIVE_APPDATA)
            )
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        initializeViews()
    }

    private fun initializeViews() {
        recyclerView = findViewById(R.id.settingsRecyclerView) ?: throw IllegalStateException("RecyclerView not found")
        btnClose = findViewById(R.id.btnClose) ?: throw IllegalStateException("Close button not found")

        // Use different layout managers for mobile vs tablet
        setupLayoutManager()

        adapter = SettingsAdapter()
        recyclerView.adapter = adapter

        btnClose.setOnClickListener {
            finish()
        }

        updateUI()
    }

    private fun isTablet(): Boolean {
        val displayMetrics = resources.displayMetrics
        val screenWidthDp = displayMetrics.widthPixels / displayMetrics.density
        return screenWidthDp >= 600
    }

    private fun setupLayoutManager() {
        if (isTablet()) {
            // Tablet: Grid layout with flexible height cards
            val spanCount = calculateSpanCount()
            val gridLayoutManager = GridLayoutManager(this, spanCount)
            recyclerView.layoutManager = gridLayoutManager
        } else {
            // Mobile: Always use linear layout (vertical list) with rectangular cards
            // Even in landscape, vertical scrolling list works better than grid on small screens
            recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        }
    }

    private fun calculateSpanCount(): Int {
        val orientation = resources.configuration.orientation
        return when {
            // Tablet landscape: 3 columns (to fit all options without scrolling)
            orientation == Configuration.ORIENTATION_LANDSCAPE -> 3
            // Tablet portrait: 2 columns
            else -> 2
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Update layout when orientation changes
        setupLayoutManager()
        adapter.notifyDataSetChanged()
    }

    private fun updateUI() {
        // Notify adapter to refresh all cards
        adapter.notifyDataSetChanged()
    }

    private fun enableSync() {
        val account = GoogleSignIn.getLastSignedInAccount(this)

        if (account == null) {
            // Need to sign in first
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        } else {
            // Already signed in
            sharedPreferences.edit().putBoolean(PreferencesKeys.Sync.SYNC_ENABLED, true).apply()
            updateUI()
            Toast.makeText(this, getString(R.string.sync_enabled), Toast.LENGTH_SHORT).show()

            // Log drive sync enabled
            analyticsService.logDriveSyncEnabled()
        }
    }

    private fun disableSync() {
        sharedPreferences.edit().putBoolean(PreferencesKeys.Sync.SYNC_ENABLED, false).apply()
        updateUI()
        Toast.makeText(this, getString(R.string.sync_disabled), Toast.LENGTH_LONG).show()

        // Log drive sync disabled
        analyticsService.logDriveSyncDisabled()
    }

    private fun signOut() {
        googleSignInClient.signOut().addOnCompleteListener(this) {
            sharedPreferences.edit()
                .putBoolean(PreferencesKeys.Sync.SYNC_ENABLED, PreferencesKeys.Defaults.SYNC_ENABLED)
                .putString(PreferencesKeys.Sync.USER_EMAIL, PreferencesKeys.Defaults.USER_EMAIL)
                .putString(PreferencesKeys.Sync.USER_NAME, PreferencesKeys.Defaults.USER_NAME)
                .putBoolean(PreferencesKeys.Sync.IS_LOGGED_IN, PreferencesKeys.Defaults.IS_LOGGED_IN)
                .apply()

            updateUI()
            Toast.makeText(this, getString(R.string.session_closed), Toast.LENGTH_SHORT).show()
        }
    }

    private fun syncNow() {
        Toast.makeText(this, getString(R.string.syncing), Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                // Trigger a sync by notifying MainActivity
                setResult(RESULT_OK)
                Toast.makeText(this@SettingsActivity, getString(R.string.sync_complete), Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@SettingsActivity, getString(R.string.sync_error, e.message), Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)!!

                // Save login state
                sharedPreferences.edit().apply {
                    putString(PreferencesKeys.Sync.USER_EMAIL, account.email ?: PreferencesKeys.Defaults.USER_EMAIL)
                    putString(PreferencesKeys.Sync.USER_NAME, account.displayName ?: PreferencesKeys.Defaults.USER_NAME)
                    putBoolean(PreferencesKeys.Sync.IS_LOGGED_IN, true)
                    putBoolean(PreferencesKeys.Sync.SYNC_ENABLED, true)
                    apply()
                }

                updateUI()
                Toast.makeText(this, getString(R.string.connected_with, account.email), Toast.LENGTH_SHORT).show()

                // Log drive sync enabled after successful sign-in
                analyticsService.logDriveSyncEnabled()
            } catch (e: com.google.android.gms.common.api.ApiException) {
                updateUI()
                Toast.makeText(this, getString(R.string.sign_in_error, e.message), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showLanguageDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_language_selection, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.languagesRecyclerView)

        // Configure grid layout (2 columns for dialog)
        recyclerView.layoutManager = GridLayoutManager(this, 2)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        // Apply rounded corners to dialog
        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_rounded_background)

        // Create adapter for language selection
        val languageAdapter = LanguageDialogAdapter { selectedLanguage ->
            LocaleHelper.saveLanguage(this, selectedLanguage)
            dialog.dismiss()

            // Recreate Settings activity to reload with new language
            recreate()
        }

        recyclerView.adapter = languageAdapter
        dialog.show()
    }

    // Data class for language items
    data class LanguageItem(val code: String, val nameResId: Int, val flagCode: String)

    // Adapter for language selection dialog
    inner class LanguageDialogAdapter(
        private val onLanguageSelected: (String) -> Unit
    ) : RecyclerView.Adapter<LanguageDialogAdapter.LanguageViewHolder>() {

        private val languages = listOf(
            LanguageItem("en", R.string.lang_en, "EN"),
            LanguageItem("es", R.string.lang_es, "ES"),
            LanguageItem("fr", R.string.lang_fr, "FR"),
            LanguageItem("pt", R.string.lang_pt, "PT"),
            LanguageItem("de", R.string.lang_de, "DE")
        )

        private val currentLanguage = LocaleHelper.getSavedLanguage(this@SettingsActivity)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_language_card_dialog, parent, false)
            return LanguageViewHolder(view)
        }

        override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
            holder.bind(languages[position])
        }

        override fun getItemCount(): Int = languages.size

        inner class LanguageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val card: MaterialCardView = itemView.findViewById(R.id.languageCard)
            private val imgFlag: android.widget.ImageView = itemView.findViewById(R.id.imgFlag)
            private val tvLanguageName: TextView = itemView.findViewById(R.id.tvLanguageName)

            fun bind(language: LanguageItem) {
                tvLanguageName.text = getString(language.nameResId)

                // Load flag image from assets
                val flagPath = "file:///android_asset/images/flags/${language.flagCode}.png"
                com.bumptech.glide.Glide.with(itemView.context)
                    .load(flagPath)
                    .into(imgFlag)

                // Highlight if this is the current language
                if (currentLanguage == language.code) {
                    card.strokeColor = resources.getColor(R.color.md_theme_primary, null)
                    card.strokeWidth = 8
                } else {
                    card.strokeColor = resources.getColor(android.R.color.darker_gray, null)
                    card.strokeWidth = 4
                }

                card.setOnClickListener {
                    onLanguageSelected(language.code)
                }
            }
        }
    }

    private fun updateLanguageDisplay() {
        val currentLanguage = LocaleHelper.getSavedLanguage(this)
        // This will be updated through the adapter
    }

    // RecyclerView Adapter
    inner class SettingsAdapter : RecyclerView.Adapter<SettingsAdapter.SettingViewHolder>() {

        private val items = listOf("edit_mode", "sync", "language")

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SettingViewHolder {
            // Use different layouts for mobile vs tablet
            val layoutRes = if (isTablet()) {
                R.layout.item_settings_card_tablet
            } else {
                R.layout.item_settings_card_mobile
            }

            val view = LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
            return SettingViewHolder(view)
        }

        override fun onBindViewHolder(holder: SettingViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size

        inner class SettingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val card: MaterialCardView = itemView.findViewById(R.id.settingCard)
            private val tvEmoji: TextView = itemView.findViewById(R.id.tvEmoji)
            private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
            private val contentContainer: ViewGroup = itemView.findViewById(R.id.cardSpecificContent)

            fun bind(itemType: String) {
                contentContainer.removeAllViews()

                when (itemType) {
                    "edit_mode" -> setupEditModeCard()
                    "sync" -> setupSyncCard()
                    "language" -> setupLanguageCard()
                }
            }

            private fun setupEditModeCard() {
                tvEmoji.text = "✎"
                tvTitle.text = getString(R.string.edit_mode_section)

                val button = MaterialButton(itemView.context, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                    text = getString(R.string.activate_edit_mode)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    // Apply secondary button style
                    textSize = 16f
                    isAllCaps = false
                    cornerRadius = resources.getDimensionPixelSize(R.dimen.corner_radius_small)
                    strokeWidth = resources.getDimensionPixelSize(R.dimen.spacing_tiny) / 2
                    strokeColor = android.content.res.ColorStateList.valueOf(0xFFE0E0E0.toInt())
                    setOnClickListener {
                        val resultIntent = Intent()
                        resultIntent.putExtra(EXTRA_TOGGLE_EDIT_MODE, true)
                        setResult(RESULT_TOGGLE_EDIT, resultIntent)
                        finish()
                    }
                }
                contentContainer.addView(button)
            }

            private fun setupSyncCard() {
                tvEmoji.text = "☁"
                tvTitle.text = getString(R.string.sync_section_title)

                val layout = LinearLayout(itemView.context).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                // Sync toggle
                val toggleLayout = LinearLayout(itemView.context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                val textLayout = LinearLayout(itemView.context).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    )
                }

                textLayout.addView(TextView(itemView.context).apply {
                    text = getString(R.string.enable_sync)
                    textSize = 14f
                })

                val tvStatus = TextView(itemView.context).apply {
                    textSize = 12f
                    val syncEnabled = sharedPreferences.getBoolean(PreferencesKeys.Sync.SYNC_ENABLED, PreferencesKeys.Defaults.SYNC_ENABLED)
                    text = if (syncEnabled) getString(R.string.sync_status_enabled) else getString(R.string.sync_status_disabled)
                }
                textLayout.addView(tvStatus)

                val switchSync = SwitchMaterial(itemView.context).apply {
                    isChecked = sharedPreferences.getBoolean(PreferencesKeys.Sync.SYNC_ENABLED, PreferencesKeys.Defaults.SYNC_ENABLED)

                    // Set thumb and track colors to blue 700
                    val blue700 = 0xFF1976D2.toInt()
                    val colorStateList = android.content.res.ColorStateList(
                        arrayOf(
                            intArrayOf(android.R.attr.state_checked),
                            intArrayOf()
                        ),
                        intArrayOf(
                            blue700,  // Checked state
                            0xFFBDBDBD.toInt()  // Unchecked state (gray)
                        )
                    )
                    thumbTintList = colorStateList
                    trackTintList = colorStateList

                    setOnCheckedChangeListener { _, isChecked ->
                        if (isChecked) {
                            enableSync()
                        } else {
                            disableSync()
                        }
                    }
                }

                toggleLayout.addView(textLayout)
                toggleLayout.addView(switchSync)
                layout.addView(toggleLayout)

                contentContainer.addView(layout)
            }

            private fun setupLanguageCard() {
                tvEmoji.text = "◉"
                tvTitle.text = getString(R.string.language_section_title)

                val button = MaterialButton(itemView.context, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                    val currentLanguage = LocaleHelper.getSavedLanguage(itemView.context)
                    text = LocaleHelper.getLanguageDisplayNameWithFlag(itemView.context, currentLanguage)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    // Apply secondary button style
                    textSize = 16f
                    isAllCaps = false
                    cornerRadius = resources.getDimensionPixelSize(R.dimen.corner_radius_small)
                    strokeWidth = resources.getDimensionPixelSize(R.dimen.spacing_tiny) / 2
                    strokeColor = android.content.res.ColorStateList.valueOf(0xFFE0E0E0.toInt())
                    setOnClickListener {
                        showLanguageDialog()
                    }
                }

                contentContainer.addView(button)
            }

        }
    }
}
