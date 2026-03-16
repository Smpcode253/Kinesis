package com.aura.assistant

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.aura.assistant.core.AuraCoreService
import com.aura.assistant.databinding.ActivityMainBinding
import com.aura.assistant.settings.SettingsActivity
import com.aura.assistant.ui.MainViewModel

/**
 * The main entry point of the Aura application.
 * Displays the assistant status and provides controls to start/stop listening.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            startAuraService()
        } else {
            Toast.makeText(this, R.string.permissions_required_message, Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        setupButtons()
        observeViewModel()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            R.id.action_notification_access -> {
                openNotificationAccessSettings()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupButtons() {
        binding.btnStartListening.setOnClickListener {
            if (viewModel.isServiceRunning.value == true) {
                sendServiceAction(AuraCoreService.ACTION_START_LISTENING)
            } else {
                requestPermissionsAndStart()
            }
        }

        binding.btnStopService.setOnClickListener {
            stopAuraService()
        }
    }

    private fun observeViewModel() {
        viewModel.isServiceRunning.observe(this) { running ->
            binding.btnStartListening.isEnabled = true
            binding.btnStopService.isEnabled = running
            binding.tvStatus.text = if (running) {
                getString(R.string.status_running)
            } else {
                getString(R.string.status_stopped)
            }
        }

        viewModel.currentContext.observe(this) { context ->
            binding.tvContext.text = getString(R.string.context_label, context.label)
        }
    }

    private fun requestPermissionsAndStart() {
        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_CONTACTS
        )
        requestPermissionsLauncher.launch(permissions)
    }

    private fun startAuraService() {
        val intent = Intent(this, AuraCoreService::class.java)
        startForegroundService(intent)
        viewModel.setServiceRunning(true)
    }

    private fun stopAuraService() {
        sendServiceAction(AuraCoreService.ACTION_STOP_SERVICE)
        viewModel.setServiceRunning(false)
    }

    private fun sendServiceAction(action: String) {
        val intent = Intent(this, AuraCoreService::class.java).apply {
            this.action = action
        }
        startService(intent)
    }

    private fun openNotificationAccessSettings() {
        AlertDialog.Builder(this)
            .setTitle(R.string.notification_access_title)
            .setMessage(R.string.notification_access_message)
            .setPositiveButton(R.string.open_settings) { _, _ ->
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
