package com.aura.assistant.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Receives the BOOT_COMPLETED broadcast to restart the Aura core service
 * after the device reboots.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val serviceIntent = Intent(context, AuraCoreService::class.java)
            context.startForegroundService(serviceIntent)
        }
    }
}
