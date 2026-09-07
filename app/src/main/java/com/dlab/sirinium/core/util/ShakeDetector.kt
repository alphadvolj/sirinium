package com.dlab.sirinium.core.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlin.math.sqrt

/**
 * Robust accelerometer-based shake detector.
 * Detects deliberate device shaking while ignoring casual pocket movements or walking.
 */
class ShakeDetector(
    private val context: Context,
    private val onShake: () -> Unit
) : SensorEventListener {

    companion object {
        // Minimum acceleration in Gs (1G = 9.80665 m/s^2)
        private const val SHAKE_THRESHOLD_G = 2.4f
        // Minimum interval between distinct shakes in a sequence
        private const val SHAKE_SLOP_TIME_MS = 250L
        // Timeout to reset shake counter
        private const val SHAKE_COUNT_RESET_TIME_MS = 1500L
        // Cooldown after successfully triggering shake action
        private const val ACTION_COOLDOWN_MS = 2500L
        // Required shakes in sequence to trigger
        private const val REQUIRED_SHAKE_COUNT = 2
    }

    private var shakeTimestamp: Long = 0L
    private var shakeCount: Int = 0
    private var lastActionTimestamp: Long = 0L

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        val now = System.currentTimeMillis()
        if (now - lastActionTimestamp < ACTION_COOLDOWN_MS) {
            return
        }

        val x = event.values[0] / SensorManager.GRAVITY_EARTH
        val y = event.values[1] / SensorManager.GRAVITY_EARTH
        val z = event.values[2] / SensorManager.GRAVITY_EARTH

        // Calculate total g-force magnitude
        val gForce = sqrt((x * x + y * y + z * z).toDouble()).toFloat()

        if (gForce > SHAKE_THRESHOLD_G) {
            if (shakeTimestamp + SHAKE_SLOP_TIME_MS > now) {
                return
            }

            if (shakeTimestamp + SHAKE_COUNT_RESET_TIME_MS < now) {
                shakeCount = 0
            }

            shakeTimestamp = now
            shakeCount++

            if (shakeCount >= REQUIRED_SHAKE_COUNT) {
                shakeCount = 0
                lastActionTimestamp = now
                vibrate()
                onShake()
            }
        }
    }

    private fun vibrate() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(
                    VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            }
        } catch (_: Exception) {
            // Ignore vibration errors on devices without vibrator
        }
    }
}
