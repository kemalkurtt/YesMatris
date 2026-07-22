package com.example.yesmatris

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "game_prefs")

class ScoreManager(private val context: Context) {

    companion object {
        val HIGH_SCORE_KEY = intPreferencesKey("high_score")
    }

    val highScoreFlow: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[HIGH_SCORE_KEY] ?: 0
        }

    suspend fun saveHighScore(score: Int) {
        context.dataStore.edit { preferences ->
            val currentHigh = preferences[HIGH_SCORE_KEY] ?: 0
            if (score > currentHigh) {
                preferences[HIGH_SCORE_KEY] = score
            }
        }
    }
}