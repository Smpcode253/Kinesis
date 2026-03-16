package com.aura.assistant.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.aura.assistant.AuraApplication
import com.aura.assistant.domain.model.UserContext

/**
 * ViewModel for [com.aura.assistant.MainActivity].
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as AuraApplication

    private val _isServiceRunning = MutableLiveData(false)
    val isServiceRunning: LiveData<Boolean> = _isServiceRunning

    private val _currentContext = MutableLiveData(app.contextManager.currentContext)
    val currentContext: LiveData<UserContext> = _currentContext

    fun setServiceRunning(running: Boolean) {
        _isServiceRunning.value = running
        if (running) {
            app.contextManager.startMonitoring { context ->
                _currentContext.postValue(context)
            }
        } else {
            app.contextManager.stopMonitoring()
        }
    }
}
