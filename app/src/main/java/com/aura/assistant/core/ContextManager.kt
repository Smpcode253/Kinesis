package com.aura.assistant.core

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import com.aura.assistant.domain.model.UserContext
import kotlin.math.sqrt

/**
 * Monitors sensors and device state to infer the user's current context
 * (e.g., Driving, Walking, Home).
 *
 * Uses the accelerometer to detect motion levels as a proxy for activity.
 * A real implementation would use ActivityRecognitionClient.
 */
class ContextManager(private val appContext: Context) : SensorEventListener {

    private val sensorManager: SensorManager by lazy {
        appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    private var _currentContext: UserContext = UserContext.UNKNOWN
    val currentContext: UserContext get() = _currentContext

    private var motionMagnitude = 0f
    private var listener: ((UserContext) -> Unit)? = null

    fun startMonitoring(onContextChanged: (UserContext) -> Unit) {
        listener = onContextChanged
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        } else {
            Log.w(TAG, "Accelerometer not available — context inference limited")
        }
    }

    fun stopMonitoring() {
        sensorManager.unregisterListener(this)
        listener = null
    }

    /**
     * Allows the user or another system component to manually override the context.
     */
    fun setContext(context: UserContext) {
        if (_currentContext != context) {
            _currentContext = context
            listener?.invoke(context)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        motionMagnitude = sqrt(x * x + y * y + z * z)

        val newContext = when {
            motionMagnitude > DRIVING_THRESHOLD -> UserContext.DRIVING
            motionMagnitude > WALKING_THRESHOLD -> UserContext.WALKING
            else -> {
                // Keep existing context if it's manually set to HOME or WORK
                if (_currentContext == UserContext.HOME || _currentContext == UserContext.WORK) {
                    _currentContext
                } else {
                    UserContext.UNKNOWN
                }
            }
        }

        if (newContext != _currentContext) {
            _currentContext = newContext
            listener?.invoke(newContext)
            Log.d(TAG, "Context changed to: $newContext (magnitude=$motionMagnitude)")
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    companion object {
        private const val TAG = "ContextManager"
        private const val DRIVING_THRESHOLD = 2.5f
        private const val WALKING_THRESHOLD = 0.8f
    }
}
