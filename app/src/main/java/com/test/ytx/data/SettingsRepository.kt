package com.test.ytx.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository private constructor(context: Context) {

    private val dataStore = context.applicationContext.dataStore
    private val scope = CoroutineScope(Dispatchers.IO)

    private val accentColorKey = intPreferencesKey("accent_color")
    private val serverUrlKey = stringPreferencesKey("server_url")
    private val downloadPathKey = stringPreferencesKey("download_path")
    private val setupCompletedKey = booleanPreferencesKey("setup_completed")

    // In-memory state for zero-lag UI updates
    private val _accentColor = MutableStateFlow<Int?>(null)
    val accentColor: StateFlow<Int?> = _accentColor.asStateFlow()

    private val _isServerOnline = MutableStateFlow(true)
    val isServerOnline: StateFlow<Boolean> = _isServerOnline.asStateFlow()

    init {
        scope.launch {
            dataStore.data.map { it[accentColorKey] }.distinctUntilChanged().collect { color ->
                if (_accentColor.value == null) {
                    _accentColor.value = color
                }
            }
        }
    }

    val serverUrl: Flow<String> = dataStore.data.map { it[serverUrlKey] ?: "http://10.0.2.2:8000" }
    val downloadPath: Flow<String?> = dataStore.data.map { it[downloadPathKey] }
    val isSetupCompleted: Flow<Boolean> = dataStore.data.map { it[setupCompletedKey] ?: false }

    fun updateAccentColor(color: Int) {
        if (_accentColor.value == color) return
        _accentColor.value = color
        scope.launch {
            dataStore.edit { it[accentColorKey] = color }
        }
    }

    suspend fun updateServerUrl(url: String) {
        dataStore.edit { it[serverUrlKey] = url }
    }

    suspend fun updateDownloadPath(path: String) {
        dataStore.edit { it[downloadPathKey] = path }
    }

    suspend fun setSetupCompleted(completed: Boolean) {
        dataStore.edit { it[setupCompletedKey] = completed }
    }

    fun setServerOnline(online: Boolean) {
        _isServerOnline.value = online
    }

    companion object {
        @Volatile
        private var INSTANCE: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsRepository(context).also { INSTANCE = it }
            }
        }
    }
}
