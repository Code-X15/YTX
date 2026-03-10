package com.test.ytx.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.test.ytx.data.SettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    val accentColor = repository.accentColor
    val serverUrl = repository.serverUrl.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "http://10.0.2.2:8000")
    val downloadPath = repository.downloadPath.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    private val _isServerOnline = MutableStateFlow(false)
    val isServerOnline: StateFlow<Boolean> = _isServerOnline.asStateFlow()

    private var heartbeatJob: Job? = null
    private val client = OkHttpClient()

    init {
        startHeartbeat()
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = viewModelScope.launch {
            while (true) {
                checkServerStatus()
                delay(5000)
            }
        }
    }

    private suspend fun checkServerStatus() {
        val url = serverUrl.value
        if (url.isBlank()) {
            _isServerOnline.value = false
            return
        }

        try {
            val request = Request.Builder().url("$url/metadata?url=test").build()
            client.newCall(request).execute().use { response ->
                _isServerOnline.value = response.isSuccessful
            }
        } catch (e: Exception) {
            _isServerOnline.value = false
        }
    }

    fun updateAccentColor(color: Int) = viewModelScope.launch { repository.updateAccentColor(color) }
    
    fun updateServerUrl(url: String) {
        viewModelScope.launch {
            repository.updateServerUrl(url)
            checkServerStatus()
        }
    }

    fun updateDownloadPath(path: String) = viewModelScope.launch { repository.updateDownloadPath(path) }

    override fun onCleared() {
        super.onCleared()
        heartbeatJob?.cancel()
    }
}
