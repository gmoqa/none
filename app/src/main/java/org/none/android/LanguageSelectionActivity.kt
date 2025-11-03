package org.none.android

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import org.none.android.services.AnalyticsService
import java.util.Locale

class LanguageSelectionActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: LanguageAdapter
    private lateinit var btnContinue: MaterialButton
    private lateinit var tvWelcomeTitle: TextView
    private lateinit var analyticsService: AnalyticsService

    private var selectedLanguage: String? = null
    private var popMediaPlayer: MediaPlayer? = null

    private val languages = listOf(
        Language("en", R.string.lang_en, "EN"),
        Language("es", R.string.lang_es, "ES"),
        Language("fr", R.string.lang_fr, "FR"),
        Language("pt", R.string.lang_pt, "PT"),
        Language("de", R.string.lang_de, "DE")
    )

    data class Language(val code: String, val nameResId: Int, val flagCode: String)

    companion object {
        private const val PREFS_NAME = "TeaBoardPrefs"
        private const val KEY_LANGUAGE_SELECTED = "language_selected"

        // Animation constants
        private const val CARD_ANIMATION_DURATION_MS = 400L
        private const val CARD_ANIMATION_DELAY_MS = 80L
        private const val CARD_ANIMATION_OVERSHOOT = 1.2f
        private const val CARD_ANIMATION_INITIAL_SCALE = 0.8f

        fun isFirstLaunch(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return !prefs.getBoolean(KEY_LANGUAGE_SELECTED, false)
        }

        fun markLanguageSelected(context: Context) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_LANGUAGE_SELECTED, true).apply()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_language_selection)

        analyticsService = AnalyticsService.getInstance(this)

        // Log screen view
        analyticsService.logScreenView("Language Selection", "LanguageSelectionActivity")

        // Configure white status bar with dark icons
        window.statusBarColor = android.graphics.Color.WHITE
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        // Prevent back navigation - user must select a language
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Do nothing - prevent going back
            }
        })

        initializeViews()
        loadPopSound()
    }

    private fun initializeViews() {
        recyclerView = findViewById(R.id.languagesRecyclerView)
        btnContinue = findViewById(R.id.btnContinue)
        tvWelcomeTitle = findViewById(R.id.tvWelcomeTitle)

        // Calculate span count based on screen size and orientation
        val spanCount = calculateSpanCount()

        recyclerView.layoutManager = GridLayoutManager(this, spanCount)

        adapter = LanguageAdapter()
        recyclerView.adapter = adapter

        btnContinue.setOnClickListener {
            selectedLanguage?.let { lang ->
                saveLanguage(lang)
                finish()
            }
        }
    }

    private fun calculateSpanCount(): Int {
        val displayMetrics = resources.displayMetrics
        val screenWidthDp = displayMetrics.widthPixels / displayMetrics.density
        val orientation = resources.configuration.orientation

        return when {
            // Tablet landscape: 3 columns
            screenWidthDp >= 600 && orientation == Configuration.ORIENTATION_LANDSCAPE -> 3
            // Tablet portrait: 2 columns
            screenWidthDp >= 600 && orientation == Configuration.ORIENTATION_PORTRAIT -> 2
            // Phone landscape: 3 columns
            orientation == Configuration.ORIENTATION_LANDSCAPE -> 3
            // Phone portrait: 2 columns
            else -> 2
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Update grid when orientation changes
        val spanCount = calculateSpanCount()
        (recyclerView.layoutManager as? GridLayoutManager)?.spanCount = spanCount
    }

    private fun loadPopSound() {
        try {
            popMediaPlayer = MediaPlayer()
            val afd = assets.openFd("sounds/pop.mp3")
            popMediaPlayer?.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            popMediaPlayer?.setAudioStreamType(AudioManager.STREAM_MUSIC)
            popMediaPlayer?.prepare()
            afd.close()
        } catch (e: Exception) {
            Log.e("LanguageSelection", "Error loading pop sound: ${e.message}")
        }
    }

    private fun playPopSound() {
        try {
            popMediaPlayer?.let {
                if (it.isPlaying) {
                    it.seekTo(0)
                } else {
                    it.start()
                }
            }
        } catch (e: Exception) {
            Log.e("LanguageSelection", "Error playing pop sound: ${e.message}")
        }
    }

    private fun saveLanguage(languageCode: String) {
        // Language is already saved when user clicks, just mark as selected and navigate
        markLanguageSelected(this)

        // Navigate to MainActivity
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        popMediaPlayer?.release()
        popMediaPlayer = null
    }

    // RecyclerView Adapter
    inner class LanguageAdapter : RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder>() {

        private var selectedPosition = -1

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_language_card, parent, false)
            return LanguageViewHolder(view)
        }

        override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
            holder.bind(languages[position], position)
        }

        override fun getItemCount(): Int = languages.size

        inner class LanguageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val card: MaterialCardView = itemView.findViewById(R.id.languageCard)
            private val imgFlag: ImageView = itemView.findViewById(R.id.imgFlag)
            private val tvLanguageName: TextView = itemView.findViewById(R.id.tvLanguageName)

            init {
                // Initially hide card for animation
                card.alpha = 0f
                card.scaleX = CARD_ANIMATION_INITIAL_SCALE
                card.scaleY = CARD_ANIMATION_INITIAL_SCALE
            }

            fun bind(language: Language, position: Int) {
                tvLanguageName.text = getString(language.nameResId)

                // Animate card appearance with staggered delay
                card.postDelayed({
                    card.animate()
                        .alpha(1f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(CARD_ANIMATION_DURATION_MS)
                        .setInterpolator(android.view.animation.OvershootInterpolator(CARD_ANIMATION_OVERSHOOT))
                        .start()
                }, position * CARD_ANIMATION_DELAY_MS)

                // Load flag image from assets
                val flagPath = "file:///android_asset/images/flags/${language.flagCode}.png"

                Glide.with(itemView.context)
                    .load(flagPath)
                    .into(imgFlag)

                // Update card appearance based on selection
                if (selectedPosition == position) {
                    card.strokeColor = resources.getColor(R.color.md_theme_primary, null)
                    card.strokeWidth = 8
                } else {
                    card.strokeColor = resources.getColor(android.R.color.darker_gray, null)
                    card.strokeWidth = 4
                }

                card.setOnClickListener {
                    itemView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    playPopSound()

                    val previousPosition = selectedPosition
                    selectedPosition = position
                    selectedLanguage = language.code

                    // Update UI
                    notifyItemChanged(previousPosition)
                    notifyItemChanged(selectedPosition)

                    // Update title to show in selected language immediately
                    LocaleHelper.saveLanguage(itemView.context, language.code)

                    // Log language selection analytics
                    analyticsService.logLanguageSelected(language.code)

                    // Recreate the context with new locale and update title
                    val newContext = LocaleHelper.applyLanguage(itemView.context)
                    tvWelcomeTitle.text = newContext.getString(R.string.welcome_title)
                    findViewById<TextView>(R.id.tvWelcomeSubtitle)?.text = newContext.getString(R.string.welcome_subtitle)

                    // Enable continue button
                    btnContinue.isEnabled = true
                }
            }
        }
    }
}
