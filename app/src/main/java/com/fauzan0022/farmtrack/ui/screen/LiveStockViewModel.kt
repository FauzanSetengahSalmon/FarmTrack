package com.fauzan0022.farmtrack.ui.screen

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fauzan0022.farmtrack.localDatabase.AppDatabase
import com.fauzan0022.farmtrack.localDatabase.LivestockEntity
import com.fauzan0022.farmtrack.localDatabase.UserDataStore
import com.fauzan0022.farmtrack.model.User
import com.fauzan0022.farmtrack.network.FarmTrackApiService
import com.fauzan0022.farmtrack.network.NetworkMonitor
import com.fauzan0022.farmtrack.repository.LivestockRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface UiState {
    object Idle : UiState
    object Loading : UiState
    data class Success(val message: String) : UiState
    data class Error(val errorMessage: String) : UiState
}

class LivestockViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "LivestockViewModel"
    }

    private val database = AppDatabase.getDatabase(application)
    private val apiService = FarmTrackApiService.create()
    private val repository = LivestockRepository(application, database.livestockDao(), apiService)
    private val userPrefDataStore = UserDataStore(application)
    private val networkMonitor = NetworkMonitor(application)

    val currentUser: StateFlow<User> = userPrefDataStore.userFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = User()
        )

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val livestockList: StateFlow<List<LivestockEntity>> = currentUser
        .flatMapLatest { user ->
            if (user.isEmpty()) {
                flowOf(emptyList())
            } else {
                repository.getLivestockFlow(user.email)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    init {
        viewModelScope.launch {
            combine(currentUser, isOnline) { user, online -> Pair(user, online) }
                .collect { (user, online) ->
                    if (!user.isEmpty() && online) {
                        triggerSync()
                    }
                }
        }
    }

    fun triggerSync() {
        val email = currentUser.value.email
        val online = isOnline.value
        if (email.isNotEmpty() && online) {
            viewModelScope.launch {
                _isSyncing.value = true
                try {
                    repository.sync(email, true)
                } catch (e: Exception) {
                    Log.e(TAG, "Gagal menjalankan sinkronisasi otomatis", e)
                } finally {
                    _isSyncing.value = false
                }
            }
        }
    }

    fun loginWithGoogle(name: String, email: String, photoUrl: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            userPrefDataStore.saveData(User(name, email, photoUrl))
            _uiState.value = UiState.Success("Masuk berhasil sebagai $name")
            triggerSync()
        }
    }

    fun logout() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            userPrefDataStore.clearData()
            _uiState.value = UiState.Success("Berhasil keluar akun")
        }
    }

    fun addLivestock(
        name: String,
        type: String,
        age: Int,
        weight: Double,
        bitmap: Bitmap?
    ) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val email = currentUser.value.email
            if (email.isEmpty()) {
                _uiState.value = UiState.Error("Sesi pengguna hilang")
                return@launch
            }

            try {
                val photoPath = if (bitmap != null) {
                    repository.saveBitmapToCache(bitmap)
                } else {
                    repository.createPlaceholderImage(name)
                }

                val result = repository.addLivestock(
                    userEmail = email,
                    name = name,
                    type = type,
                    age = age,
                    weight = weight,
                    photoPath = photoPath,
                    isOnline = isOnline.value
                )

                if (result == "success" || result == "saved_offline") {
                    _uiState.value = UiState.Success("Ternak berhasil ditambahkan")
                    triggerSync()
                } else {
                    _uiState.value = UiState.Error(result)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gagal menambah data ternak", e)
                _uiState.value = UiState.Error(e.message ?: "Gagal menambahkan data ternak")
            }
        }
    }

    fun updateLivestock(
        entity: LivestockEntity,
        name: String,
        type: String,
        age: Int,
        weight: Double,
        bitmap: Bitmap? = null
    ) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val result = repository.updateLivestock(
                    entity = entity,
                    name = name,
                    type = type,
                    age = age,
                    weight = weight,
                    newBitmap = bitmap,
                    isOnline = isOnline.value
                )

                if (result == "success" || result == "saved_offline") {
                    _uiState.value = UiState.Success("Data ternak berhasil diperbarui")
                    triggerSync()
                } else {
                    _uiState.value = UiState.Error(result)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gagal memperbarui data ternak", e)
                _uiState.value = UiState.Error(e.message ?: "Gagal memperbarui data ternak")
            }
        }
    }

    fun deleteLivestock(entity: LivestockEntity) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val result = repository.deleteLivestock(
                    entity = entity,
                    isOnline = isOnline.value
                )

                if (result == "success" || result == "deleted_offline") {
                    _uiState.value = UiState.Success("Data ternak berhasil dihapus")
                    triggerSync()
                } else {
                    _uiState.value = UiState.Error(result)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gagal menghapus data ternak", e)
                _uiState.value = UiState.Error(e.message ?: "Gagal menghapus data ternak")
            }
        }
    }

    fun clearUiState() {
        _uiState.value = UiState.Idle
    }
}