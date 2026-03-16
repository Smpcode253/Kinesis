package com.aura.assistant.executor

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import com.aura.assistant.domain.model.AuraIntent
import com.aura.assistant.domain.model.ExecutorResult
import com.aura.assistant.domain.model.UserContext

/**
 * Executor responsible for navigation actions.
 * Launches Google Maps or a compatible navigation app with the requested destination.
 */
class NavExecutor(private val context: Context) {

    /**
     * Starts navigation to the given destination.
     * Prefers Google Maps but falls back to a generic geo intent.
     */
    fun navigate(intent: AuraIntent.Navigate, userContext: UserContext): ExecutorResult {
        if (intent.destination.isBlank()) {
            return ExecutorResult.Failure("Navigation destination cannot be empty.")
        }

        val destination = Uri.encode(intent.destination)
        val mapsUri = Uri.parse("google.navigation:q=$destination&mode=${navMode(intent.mode)}")

        return if (isGoogleMapsInstalled()) {
            launchGoogleMaps(mapsUri)
            ExecutorResult.Success("Starting navigation to ${intent.destination}.")
        } else {
            launchFallbackMaps(intent.destination)
            ExecutorResult.Success("Opening maps for ${intent.destination}.")
        }
    }

    private fun launchGoogleMaps(uri: Uri) {
        val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(mapIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch Google Maps", e)
        }
    }

    private fun launchFallbackMaps(destination: String) {
        val encoded = Uri.encode(destination)
        val geoIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("geo:0,0?q=$encoded")
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(geoIntent)
        } catch (e: Exception) {
            Log.e(TAG, "No maps application found to handle navigation", e)
        }
    }

    private fun isGoogleMapsInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo("com.google.android.apps.maps", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /** Maps a natural language mode string to a Google Maps navigation mode character. */
    private fun navMode(mode: String): String = when (mode.lowercase()) {
        "walking", "walk" -> "w"
        "cycling", "bike", "bicycle" -> "b"
        "transit", "public transport" -> "r"
        else -> "d" // driving
    }

    companion object {
        private const val TAG = "NavExecutor"
    }
}
