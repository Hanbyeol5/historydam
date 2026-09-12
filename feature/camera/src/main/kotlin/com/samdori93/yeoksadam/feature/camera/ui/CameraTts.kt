package com.samdori93.yeoksadam.feature.camera.ui

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/** 카메라 해설 음성 재생용 TextToSpeech 래퍼. */
class CameraTts(context: Context) {
    private var ready = false
    private val tts = TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
            ready = true
        }
    }

    fun speak(text: String) {
        if (!ready) return
        tts.language = Locale.KOREAN
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "relic")
    }

    fun stop() {
        runCatching { tts.stop() }
    }

    fun shutdown() {
        runCatching {
            tts.stop()
            tts.shutdown()
        }
    }
}
