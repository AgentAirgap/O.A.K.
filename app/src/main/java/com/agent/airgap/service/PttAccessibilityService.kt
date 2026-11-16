package com.agent.airgap.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.agent.airgap.audio.AudioRecordStreamer
import com.agent.airgap.cpp.CactusBridge
import com.agent.airgap.tools.ToolRegistry
import java.util.concurrent.Executors

class PttAccessibilityService : AccessibilityService() {

    private var isCapturing = false
    private lateinit var wakeLock: PowerManager.WakeLock
    private val inferenceExecutor = Executors.newSingleThreadExecutor()

    override fun onCreate() {
        super.onCreate()
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AirgapAgent::PttLock")
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        // Cat S22 Flip Orange Key: scancode 0x23 or keycode 281 / KEYCODE_NUMERIC_B
        val isOrangeKey = event.scanCode == 0x23 || event.keyCode == KeyEvent.KEYCODE_NUMERIC_B

        if (!isOrangeKey) {
            return super.onKeyEvent(event)
        }

        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                if (event.repeatCount == 0 && !isCapturing) {
                    isCapturing = true
                    if (!wakeLock.isHeld) wakeLock.acquire(10000)

                    vibrateTick(35) // Haptic feedback: listening started
                    AudioRecordStreamer.startRecording()
                }
                return true
            }

            KeyEvent.ACTION_UP -> {
                if (isCapturing) {
                    isCapturing = false
                    val pcmBytes = AudioRecordStreamer.stopRecording()

                    if (pcmBytes.isNotEmpty()) {
                        inferenceExecutor.execute {
                            try {
                                // 1. Run inference directly across JNI into native memory
                                val jsonResult = CactusBridge.processAudio(pcmBytes)

                                // 2. Dispatch the parsed tool execution
                                val success = ToolRegistry.dispatch(this, jsonResult)

                                if (success) {
                                    vibrateConfirmation() // Double pulse: executed
                                } else {
                                    vibrateError()
                                }
                            } finally {
                                if (wakeLock.isHeld) wakeLock.release()
                            }
                        }
                    } else {
                        if (wakeLock.isHeld) wakeLock.release()
                    }
                }
                return true
            }
        }
        return super.onKeyEvent(event)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    private fun vibrateTick(ms: Long) {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun vibrateConfirmation() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 40, 70, 40), -1))
    }

    private fun vibrateError() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 150, 80, 150), -1))
    }
}