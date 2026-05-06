package com.ctis487.smartwardrobe.utils

import android.content.Context
import android.media.MediaPlayer
import com.ctis487.smartwardrobe.R

object SoundHelper {
    fun playSuccessSound(context: Context) {
        try {
            val mediaPlayer = MediaPlayer.create(context, R.raw.success_sound)
            mediaPlayer?.setVolume(1.0f, 1.0f)
            mediaPlayer?.setOnCompletionListener { mp -> 
                mp.release() 
            }
            mediaPlayer?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playStartupSound(context: Context) {
        try {
            val mediaPlayer = MediaPlayer.create(context, R.raw.startup_sound)
            mediaPlayer?.setVolume(1.0f, 1.0f)
            mediaPlayer?.setOnCompletionListener { mp -> 
                mp.release() 
            }
            mediaPlayer?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
