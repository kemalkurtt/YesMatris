package com.example.yesmatris

import android.content.Context
import android.media.SoundPool
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class SoundManager(context: Context) {
    private val soundPool: SoundPool = SoundPool.Builder().setMaxStreams(5).build()
    private var soundId: Int = 0

    var isSoundEnabled: Boolean = true
    var isVibrationEnabled: Boolean = true

    private val vibrator: Vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    init {
        // raw klasöründeki tıksesi.mp3 dosyasını yüklüyoruz
        try {
            soundId = soundPool.load(context, R.raw.tik_sesi, 1)        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Ses çalma fonksiyonu
    fun playPopSound() {
        if (isSoundEnabled && soundId != 0) {
            soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
        }
    }

    // Kısa titreşim fonksiyonu
    fun vibrate() {
        if (isVibrationEnabled) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(50)
            }
        }
    }
}