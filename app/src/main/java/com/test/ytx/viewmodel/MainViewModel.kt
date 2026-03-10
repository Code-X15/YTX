package com.test.ytx.viewmodel

import android.app.Application
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.test.ytx.data.SettingsRepository
import com.test.ytx.model.VideoMetadata
import com.test.ytx.network.YtxApiService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException
import java.net.URLDecoder
import java.util.concurrent.TimeUnit

sealed class UiState {
    object Idle : UiState()
    object Loading : UiState()
    object Success : UiState()
    data class Error(val message: String) : UiState()
}

sealed class DownloadState {
    object Idle : DownloadState()
    object Downloading : DownloadState()
    data class Completed(val fileUri: Uri? = null) : DownloadState()
}

data class DownloadProgressInfo(
    val progress: Float = 0f,
    val speed: String = "",
    val eta: String = "",
    val isConverting: Boolean = false
)

class MainViewModel(application: Application, private val repository: SettingsRepository) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _metadata = MutableStateFlow<VideoMetadata?>(null)
    val metadata: StateFlow<VideoMetadata?> = _metadata.asStateFlow()

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private val _downloadInfo = MutableStateFlow(DownloadProgressInfo())
    val downloadInfo: StateFlow<DownloadProgressInfo> = _downloadInfo.asStateFlow()

    val isServerOnline: StateFlow<Boolean> = repository.isServerOnline

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var webSocket: WebSocket? = null
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    private val serverUrl = repository.serverUrl.stateIn(viewModelScope, SharingStarted.Eagerly, "http://10.0.2.2:8000")

    private var heartbeatJob: Job? = null
    private var isFinishingDownload = false

    init {
        startHeartbeat()
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = viewModelScope.launch {
            while (true) {
                checkServerStatusSilent()
                delay(5000)
            }
        }
    }

    private fun getBaseUrl(): String {
        val url = serverUrl.value
        return if (url.startsWith("http")) url else "http://$url"
    }

    fun checkServerStatus() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val baseUrl = getBaseUrl()
                val service = Retrofit.Builder()
                    .baseUrl(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
                    .addConverterFactory(MoshiConverterFactory.create(moshi))
                    .build()
                    .create(YtxApiService::class.java)
                service.ping()
                repository.setServerOnline(true)
            } catch (e: Exception) {
                repository.setServerOnline(false)
            } finally {
                delay(300)
                _isRefreshing.value = false
            }
        }
    }

    private suspend fun checkServerStatusSilent() {
        try {
            val baseUrl = getBaseUrl()
            val service = Retrofit.Builder()
                .baseUrl(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(YtxApiService::class.java)
            service.ping()
            repository.setServerOnline(true)
        } catch (e: Exception) {
            repository.setServerOnline(false)
        }
    }

    fun resetForNewDownload() {
        _metadata.value = null
        _uiState.value = UiState.Idle
        _downloadState.value = DownloadState.Idle
        _downloadInfo.value = DownloadProgressInfo()
        isFinishingDownload = false
        webSocket?.close(1000, "User reset")
    }

    fun fetchMetadata(url: String) {
        if (url.isBlank()) {
            _uiState.value = UiState.Error("Please enter a valid YouTube link")
            return
        }

        viewModelScope.launch {
            _uiState.value = UiState.Loading
            _metadata.value = null
            try {
                val baseUrl = getBaseUrl()
                val service = Retrofit.Builder()
                    .baseUrl(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
                    .addConverterFactory(MoshiConverterFactory.create(moshi))
                    .build()
                    .create(YtxApiService::class.java)
                val result = service.getMetadata(url)
                _metadata.value = result
                _uiState.value = UiState.Success
                repository.setServerOnline(true)
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Failed: ${e.message ?: "Unknown error"}")
                if (e is IOException) {
                    repository.setServerOnline(false)
                }
            }
        }
    }

    fun startDownload(url: String, format: String, quality: String = "best") {
        val wsBaseUrl = getBaseUrl().replace("http", "ws").let { if (it.endsWith("/")) it.dropLast(1) else it }
        val wsUrl = "$wsBaseUrl/download"
        
        val request = Request.Builder().url(wsUrl).build()
        val client = OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build()

        isFinishingDownload = false
        _downloadState.value = DownloadState.Downloading
        _downloadInfo.value = DownloadProgressInfo(0f, "Connecting...", "", false)
        _uiState.value = UiState.Success 

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                repository.setServerOnline(true)
                val payload = mapOf(
                    "url" to url, 
                    "format" to format.lowercase(),
                    "quality" to quality
                )
                val json = moshi.adapter(Map::class.java).toJson(payload)
                webSocket.send(json)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val adapter = moshi.adapter(Map::class.java)
                    @Suppress("UNCHECKED_CAST")
                    val data = adapter.fromJson(text) as? Map<String, Any>
                    
                    data?.let {
                        val status = it["status"] as? String
                        when (status) {
                            "downloading" -> {
                                val progressStr = it["progress"] as? String ?: "0%"
                                
                                // Robust extraction of progress
                                val percentMatch = Regex("""(\d+\.?\d*)%""").find(progressStr)
                                val progressVal = percentMatch?.groupValues?.get(1)?.toFloatOrNull()?.div(100f) ?: 0f
                                
                                // Extract downloaded amount and ETA for clear logs
                                val downloaded = Regex("""\]\s+([\d\.]+[a-zA-Z]+)""").find(progressStr)?.groupValues?.get(1) ?: ""
                                val eta = Regex("""ETA\s+([^\s]+)""").find(progressStr)?.groupValues?.get(1) ?: ""
                                    
                                val displayLog = buildString {
                                    if (downloaded.isNotEmpty()) append(downloaded).append(" ")
                                    if (eta.isNotEmpty()) append(eta)
                                    if (isEmpty()) append("Downloading...")
                                }.trim()
                                    
                                val isConverting = progressStr.contains("Converting") || progressStr.contains("Processing")
                                
                                _downloadInfo.update { current ->
                                    current.copy(
                                        progress = if (progressVal > current.progress) progressVal else current.progress,
                                        speed = displayLog,
                                        eta = "", 
                                        isConverting = isConverting
                                    )
                                }
                            }
                            "finished" -> {
                                isFinishingDownload = true
                                _downloadInfo.update { it.copy(progress = 1f, speed = "Finalizing...", isConverting = true) }
                                
                                val downloadUrl = it["download_url"] as? String
                                val filename = it["filename"] as? String ?: "download.${format.lowercase()}"
                                
                                @Suppress("UNCHECKED_CAST")
                                val serverMeta = it["metadata"] as? Map<String, Any>
                                serverMeta?.let { meta ->
                                    _metadata.value = VideoMetadata(
                                        title = meta["title"] as? String ?: _metadata.value?.title ?: "Unknown",
                                        thumbnail = meta["thumbnail"] as? String ?: _metadata.value?.thumbnail,
                                        author = meta["author_name"] as? String ?: _metadata.value?.author,
                                        duration = meta["duration"] as? String ?: _metadata.value?.duration ?: "Unknown"
                                    )
                                }

                                completeDownload(downloadUrl, filename, format)
                            }
                            "error" -> {
                                val message = it["message"] as? String ?: "Server Error"
                                _uiState.value = UiState.Error(message)
                                _downloadState.value = DownloadState.Idle
                                webSocket.close(1000, "Error")
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (!isFinishingDownload && _downloadState.value !is DownloadState.Completed && _downloadInfo.value.progress < 0.98f) {
                    _uiState.value = UiState.Error("Network Lost")
                    _downloadState.value = DownloadState.Idle
                }
            }
        })
    }

    private fun completeDownload(downloadUrl: String?, filename: String, format: String) {
        viewModelScope.launch {
            try {
                _downloadInfo.update { it.copy(progress = 1f, speed = "Finalizing...") }
                
                // Use the exact downloadUrl provided by the server or build a safe one
                val finalDownloadUrl = if (downloadUrl != null) {
                    if (downloadUrl.startsWith("/")) "${getBaseUrl()}$downloadUrl" else downloadUrl
                } else {
                    getBaseUrl().toHttpUrl().newBuilder()
                        .addPathSegment("download_file")
                        .addQueryParameter("filename", filename)
                        .build()
                        .toString()
                }
                
                val uri = downloadFileToPhone(finalDownloadUrl, filename)
                
                delay(1200)
                
                _downloadState.value = DownloadState.Completed(uri)
                _downloadInfo.update { it.copy(progress = 1f, speed = "Complete", eta = "", isConverting = false) }
            } catch (e: Exception) {
                if (!isFinishingDownload || _downloadState.value !is DownloadState.Completed) {
                    _uiState.value = UiState.Error("Transfer Error: ${e.message}")
                    _downloadState.value = DownloadState.Idle
                }
            }
        }
    }

    private suspend fun downloadFileToPhone(fileUrl: String, filename: String): Uri? = withContext(Dispatchers.IO) {
        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
            
        val request = Request.Builder().url(fileUrl).build()
        val response = client.newCall(request).execute()
        
        if (!response.isSuccessful) throw IOException("Server error ${response.code}: ${response.message}")
        
        val body = response.body ?: throw IOException("Empty body")
        
        // Decode filename only once for MediaStore
        val cleanName = try {
            URLDecoder.decode(filename, "UTF-8")
        } catch (e: Exception) {
            filename
        }.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        
        saveFileToMediaStore(cleanName, body)
    }

    private fun saveFileToMediaStore(filename: String, body: ResponseBody): Uri? {
        val context = getApplication<Application>()
        val contentResolver = context.contentResolver
        val isAudio = filename.lowercase().endsWith(".mp3")
        
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, if (isAudio) "audio/mpeg" else "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            if (isAudio) MediaStore.Audio.Media.EXTERNAL_CONTENT_URI 
            else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val uri = contentResolver.insert(collection, contentValues) ?: throw IOException("Entry failed")

        try {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                body.byteStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                contentResolver.update(uri, contentValues, null, null)
            }
            return uri
        } catch (e: Exception) {
            contentResolver.delete(uri, null, null)
            throw e
        }
    }

    override fun onCleared() {
        super.onCleared()
        heartbeatJob?.cancel()
        webSocket?.close(1000, "Cleared")
    }
}
