package com.example.astryx

import android.app.Application
import android.content.Context
import com.example.astryx.data.repository.firebase.FirebaseDestinationRepository
import com.example.astryx.data.repository.firebase.FirebasePostRepository
import com.example.astryx.data.repository.firebase.FirebaseReviewRepository
import com.example.astryx.data.repository.firebase.FirebaseTripRepository
import com.example.astryx.data.repository.firebase.FirebaseUserRepository
import com.example.astryx.data.repository.firebase.interfaces.IDestinationRepository
import com.example.astryx.data.repository.firebase.interfaces.IPostRepository
import com.example.astryx.data.repository.firebase.interfaces.IReviewRepository
import com.example.astryx.data.repository.firebase.interfaces.ITripRepository
import com.example.astryx.data.repository.firebase.interfaces.IUserRepository
import com.example.astryx.data.utils.SettingsManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class AstryxApplication : Application() {

    lateinit var appContainer: AppContainer

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)

        val settingsManager = appContainer.settingsManager
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser != null) {
            settingsManager.switchUser(currentUser.uid)
        } else {
            settingsManager.applyDarkMode(settingsManager.isDarkMode)
        }
    }

    class AppContainer(context: Context) {
        // Firebase Instances
        private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
        private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
        private val storage: FirebaseStorage by lazy { FirebaseStorage.getInstance() }

        // Settings Manager
        val settingsManager: SettingsManager by lazy { SettingsManager(context) }

        // Repositories
        val tripRepository: ITripRepository by lazy {
            FirebaseTripRepository(db)
        }

        val postRepository: IPostRepository by lazy {
            FirebasePostRepository(db)
        }

        val userRepository: IUserRepository by lazy {
            FirebaseUserRepository(auth, db, storage)
        }

        val destinationRepository: IDestinationRepository by lazy {
            FirebaseDestinationRepository(db)
        }

        val reviewRepository: IReviewRepository by lazy {
            FirebaseReviewRepository(db)
        }
    }
}
