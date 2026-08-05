package com.example.yesmatris.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "game_prefs")

class ScoreManager(private val context: Context) {

    // İŞTE BÜTÜN HATALARI ÇÖZEN O SİHİRLİ SATIR BURADA:
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("game_state_prefs", Context.MODE_PRIVATE)

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

    // --- OYUN KAYIT SİSTEMİ ---
    fun saveGameState(currentScore: Int, boardState: String) {
        sharedPreferences.edit()
            .putInt("saved_score", currentScore)
            .putString("saved_board", boardState)
            .apply()
    }

    fun getSavedScore(): Int {
        return sharedPreferences.getInt("saved_score", 0)
    }

    fun getSavedBoard(): String? {
        return sharedPreferences.getString("saved_board", null)
    }

    fun clearGameState() {
        sharedPreferences.edit()
            .remove("saved_score")
            .remove("saved_board")
            .apply()
    }
}