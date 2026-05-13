package com.example.astryx.ui.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnticipateOvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import com.example.astryx.R
import com.example.astryx.AstryxApplication
import com.example.astryx.data.utils.hideSystemBars
import com.google.firebase.auth.FirebaseAuth

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        val settingsManager = (application as AstryxApplication).appContainer.settingsManager
        settingsManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        hideSystemBars()

        val logo = findViewById<View>(R.id.splashLogo)
        startAnimation(logo)
    }

    private fun startAnimation(logo: View) {
        logo.alpha = 0f
        logo.scaleX = 0.1f
        logo.scaleY = 0.1f

        logo.animate()
            .alpha(1f)
            .scaleX(2f)
            .scaleY(2f)
            .setDuration(1600)
            .setInterpolator(AnticipateOvershootInterpolator())
            .withEndAction {
                navigateToNextScreen()
            }
            .start()
    }

    private fun navigateToNextScreen() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val settingsManager = (application as AstryxApplication).appContainer.settingsManager
        val isLoggedIn = currentUser != null && !settingsManager.needsReAuthAfterPasswordReset

        val intent = if (isLoggedIn) {
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("IS_NEW_USER", false)
            }
        } else {
            Intent(this, AuthActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        }

        val options = ActivityOptionsCompat.makeCustomAnimation(
            this,
            R.anim.fade_in_slow,
            android.R.anim.fade_out
        )

        startActivity(intent, options.toBundle())
        finish()
    }
}

