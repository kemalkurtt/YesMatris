package com.example.yesmatris

import androidx.lifecycle.ViewModel
import com.example.yesmatris.utils.FirebaseRemoteConfigManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MainViewModel(
    private val remoteConfigManager: FirebaseRemoteConfigManager
) : ViewModel() {

    private val _minRequiredVersion = MutableStateFlow(1)
    val minRequiredVersion = _minRequiredVersion.asStateFlow()

    fun fetchRemoteConfig() {
        remoteConfigManager.fetch { success ->
            if (!success) return@fetch
            _minRequiredVersion.update {
                remoteConfigManager.getForceUpdateVersion()
            }
        }
    }
}
