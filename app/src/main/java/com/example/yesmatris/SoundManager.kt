package com.example.yesmatris

import android.content.Context
import android.media.SoundPool
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class SoundManager(context: Context) {
    private val soundPool: SoundPool = SoundPool.Builder().setMaxStreams(5).build()

    private var popSoundId: Int = 0
    private var mergeSoundId: Int = 0
    private var recordSoundId: Int = 0 // YENİ: Rekor kırma sesi

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
        try {
            // Sadece kayma varsa (Puan yok, kaba ses)
            popSoundId = soundPool.load(context, R.raw.puansiz, 1)

            // Sayılar birleşip puan gelirse (Puan var, ince ses)
            mergeSoundId = soundPool.load(context, R.raw.puanli, 1)

            // Yeni rekor kırılırsa
            recordSoundId = soundPool.load(context, R.raw.rekor, 1)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playPopSound() {
        if (isSoundEnabled && popSoundId != 0) {
            soundPool.play(popSoundId, 1f, 1f, 1, 0, 1f)
        }
    }

    fun playMergeSound() {
        if (isSoundEnabled && mergeSoundId != 0) {
            soundPool.play(mergeSoundId, 1f, 1f, 1, 0, 1f)
        }
    }

    // YENİ: Rekor Sesi Çalma Fonksiyonu
    fun playRecordSound() {
        if (isSoundEnabled && recordSoundId != 0) {
            soundPool.play(recordSoundId, 1f, 1f, 1, 0, 1f)
        }
    }

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