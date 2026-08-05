package com.example.yesmatris.di

import com.example.yesmatris.MainViewModel
import com.example.yesmatris.utils.ScoreManager
import com.example.yesmatris.utils.SoundManager
import com.example.yesmatris.utils.FirebaseRemoteConfigManager
import com.example.yesmatris.utils.InAppUpdateHandler
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    single { FirebaseRemoteConfigManager() }
    single { InAppUpdateHandler() }
    single { ScoreManager(androidContext()) }
    single { SoundManager(androidContext()) }

    viewModelOf(::MainViewModel)
}
