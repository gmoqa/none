package org.none.android

import android.animation.Animator
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView

class SplashActivity : AppCompatActivity() {

    private lateinit var lottieAnimationView: LottieAnimationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Configure white status bar with dark icons
        window.statusBarColor = android.graphics.Color.WHITE
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        // Keep screen on during splash
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        lottieAnimationView = findViewById(R.id.lottieAnimationView)

        // Listen for animation end
        lottieAnimationView.addAnimatorListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {
                // Animation started
            }

            override fun onAnimationEnd(animation: Animator) {
                // Animation completed, navigate to next screen
                navigateToNextScreen()
            }

            override fun onAnimationCancel(animation: Animator) {
                // Animation was cancelled
                navigateToNextScreen()
            }

            override fun onAnimationRepeat(animation: Animator) {
                // Animation repeated (won't happen since loop=false)
            }
        })
    }

    private fun navigateToNextScreen() {
        // Check if this is first launch
        if (LanguageSelectionActivity.isFirstLaunch(this)) {
            // First time - show language selection
            val intent = Intent(this, LanguageSelectionActivity::class.java)
            startActivity(intent)
        } else {
            // Not first time - go to main screen
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        // Close splash activity with no animation to prevent "jump"
        finish()
        overridePendingTransition(0, 0) // No animation on exit
    }

    override fun onDestroy() {
        super.onDestroy()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
