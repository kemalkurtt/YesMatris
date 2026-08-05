package com.example.yesmatris.utils

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings

class FirebaseRemoteConfigManager {

    private val remoteConfig: FirebaseRemoteConfig by lazy {
        FirebaseRemoteConfig.getInstance().also { config ->
            val settings = FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(FETCH_INTERVAL_SECONDS)
                .build()
            config.setConfigSettingsAsync(settings)
        }
    }

    fun fetch(onComplete: (success: Boolean) -> Unit = {}) {
        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            onComplete(task.isSuccessful)
        }
    }

    fun getForceUpdateVersion(): Int =
        remoteConfig.getLong(KEY_FORCE_UPDATE_VERSION).toInt()

    companion object {
        private const val FETCH_INTERVAL_SECONDS = 0L

        const val KEY_FORCE_UPDATE_VERSION = "android_min_required_version"
    }
}
