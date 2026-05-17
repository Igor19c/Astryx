package com.example.astryx.ui.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import com.example.astryx.R
import com.example.astryx.AstryxApplication
import com.example.astryx.data.utils.hideLoading
import com.example.astryx.data.viewmodels.AuthViewModel
import com.example.astryx.data.viewmodels.AuthViewModel.AuthState
import com.example.astryx.data.viewmodels.UserViewModel
import com.example.astryx.data.viewmodels.ViewModelFactory
import com.example.astryx.data.utils.hideSystemBars
import com.example.astryx.data.utils.showCustomMessage
import com.example.astryx.data.utils.showLoading
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract

class AuthActivity : AppCompatActivity() {
    private val authViewModel: AuthViewModel by viewModels {
        ViewModelFactory(application as AstryxApplication)
    }

    private val userViewModel: UserViewModel by viewModels {
        ViewModelFactory(application as AstryxApplication)
    }

    private var isNavigating = false

    private val signInLauncher = registerForActivityResult(
        FirebaseAuthUIActivityResultContract(),
    ) { res ->
        val success = res.resultCode == RESULT_OK
        val isCanceled = res.idpResponse == null
        val isNewUser = res.idpResponse?.isNewUser ?: false

        authViewModel.handleSignInResult(success, isCanceled, isNewUser)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val settingsManager = (application as AstryxApplication).appContainer.settingsManager
        settingsManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        hideSystemBars()

        setupObservers()
        val forceRelogin = intent.getBooleanExtra("force_relogin", false)
        authViewModel.checkAuthState(forceRelogin)
    }

    private fun setupObservers() {
        authViewModel.authState.observe(this) { state ->
            when (state) {
                is AuthState.Loading -> {
                    showLoading("Scanning credentials...")
                }

                is AuthState.Authenticated -> {
                    onAuthenticated(state.isNewUser)
                }

                is AuthState.RequiresLogin -> {
                    launchSignInFlow()
                    hideLoading()
                }

                is AuthState.Error -> {
                    onError(state.message)
                    hideLoading()
                }

                is AuthState.Idle -> {
                    finish()
                    hideLoading()
                }
            }
        }

        userViewModel.userData.observe(this) { user ->
            if (user != null && !isNavigating) {
                isNavigating = true
                val isNewUser =
                    (authViewModel.authState.value as? AuthState.Authenticated)?.isNewUser ?: false

                val message = if (isNewUser) "Preparing your space..." else "Welcome back!"
                showLoading(message)
                navigateToMainWithDelay(isNewUser, 1000L)
            }
        }

        userViewModel.error.observe(this)
        { error ->
            error?.let {
                hideLoading()
                isNavigating = false
                onError(it)
            }
        }
    }

    private fun launchSignInFlow() {
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().setRequireName(true).setAllowNewAccounts(true).build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )

        val signInIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .setLogo(R.drawable.logo_auth)
            .setCredentialManagerEnabled(false)
            .setTheme(R.style.Theme_Astryx_Auth)
            .build()

        signInLauncher.launch(signInIntent)
    }

    private fun navigateToMainWithDelay(isNewUser: Boolean, delay: Long) {
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("IS_NEW_USER", isNewUser)
            }

            val options =
                ActivityOptionsCompat.makeCustomAnimation(
                    this,
                    R.anim.fade_in_slow,
                    R.anim.fade_out
                )


            startActivity(intent, options.toBundle())
            hideLoading()
            finish()
        }, delay)
    }

    private fun onAuthenticated(isNewUser: Boolean) {
        val uid = userViewModel.getCurrentUid() ?: return

        if (isNewUser) {
            userViewModel.createProfileFromFirebase()
        } else {
            userViewModel.startListening(uid)
        }
    }

    private fun onError(message: String) {
        showCustomMessage(
            title = "Connection Error",
            body = message,
            duration = 4000
        )
        authViewModel.checkAuthState(forceRelogin = false)
    }

}
