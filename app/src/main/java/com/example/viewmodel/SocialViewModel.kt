package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ActivityEntity
import com.example.data.local.DatabaseProvider
import com.example.data.local.PostEntity
import com.example.data.local.ProfileEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SocialViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DatabaseProvider.getRepository(application)
    val supabase = repository.supabaseClient

    // My active profile stream
    val myProfile: StateFlow<ProfileEntity?> = repository.myProfile
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val allProfiles: StateFlow<List<ProfileEntity>> = repository.allProfiles
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val prefs = application.getSharedPreferences("pulse_auth_prefs", Application.MODE_PRIVATE)
    private val _isLoggedIn = MutableStateFlow(prefs.getBoolean("is_logged_in", false))
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    fun logIn(email: String, password: String): Boolean {
        if (email.contains("@") && password.length >= 4) {
            prefs.edit().putBoolean("is_logged_in", true).apply()
            viewModelScope.launch {
                try {
                    repository.getMyProfileId()
                } catch (e: Exception) {
                    android.util.Log.e("SocialViewModel", "Login database profile setup failed: ${e.message}", e)
                } finally {
                    _isLoggedIn.value = true
                }
            }
            return true
        }
        return false
    }

    fun signUp(username: String, email: String, fullName: String, bio: String, avatarUrl: String): Boolean {
        if (username.isBlank() || email.isBlank() || fullName.isBlank()) return false
        viewModelScope.launch {
            try {
                repository.updateMyProfile(username, fullName, bio, avatarUrl)
            } catch (e: Exception) {
                android.util.Log.e("SocialViewModel", "Signup profile update failed: ${e.message}", e)
            } finally {
                prefs.edit().putBoolean("is_logged_in", true).apply()
                _isLoggedIn.value = true
            }
        }
        return true
    }

    fun logOut() {
        prefs.edit().putBoolean("is_logged_in", false).apply()
        _isLoggedIn.value = false
    }

    fun toggleFollow(targetUserId: String) {
        viewModelScope.launch {
            try {
                repository.toggleFollow(targetUserId)
            } catch (e: Exception) {
                android.util.Log.e("SocialViewModel", "toggleFollow failed: ${e.message}", e)
            }
        }
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Filtered/searched post stream
    val posts: StateFlow<List<PostEntity>> = combine(
        repository.allPosts,
        _searchQuery
    ) { postList, query ->
        if (query.isBlank()) {
            postList
        } else {
            postList.filter {
                it.content.contains(query, ignoreCase = true) ||
                it.userUsername.contains(query, ignoreCase = true) ||
                it.userFullName.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Notifications state stream
    val activities: StateFlow<List<ActivityEntity>> = repository.allActivities
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncSuccess = MutableSharedFlow<Boolean>()
    val syncSuccess: SharedFlow<Boolean> = _syncSuccess.asSharedFlow()

    private val _supabaseConfigured = MutableStateFlow(false)
    val supabaseConfigured: StateFlow<Boolean> = _supabaseConfigured.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                // First open populates database with rich M3 starter feeds
                repository.prePopulateIfEmpty()
            } catch (e: Exception) {
                android.util.Log.e("SocialViewModel", "Failed to pre-populate database: ${e.message}", e)
            }
            try {
                _supabaseConfigured.value = supabase.isConfigured()
                // Auto sync on start if configured
                if (supabase.isConfigured()) {
                    syncWithSupabase()
                }
            } catch (e: Exception) {
                android.util.Log.e("SocialViewModel", "Failed to configure or sync Supabase: ${e.message}", e)
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleLike(postId: String) {
        viewModelScope.launch {
            try {
                repository.toggleLike(postId)
            } catch (e: Exception) {
                android.util.Log.e("SocialViewModel", "toggleLike failed: ${e.message}", e)
            }
        }
    }

    fun addComment(postId: String, commentText: String) {
        viewModelScope.launch {
            try {
                repository.addComment(postId, commentText)
            } catch (e: Exception) {
                android.util.Log.e("SocialViewModel", "addComment failed: ${e.message}", e)
            }
        }
    }

    fun createPost(content: String, imageUrl: String?) {
        viewModelScope.launch {
            try {
                repository.createPost(content, imageUrl)
            } catch (e: Exception) {
                android.util.Log.e("SocialViewModel", "createPost failed: ${e.message}", e)
            }
        }
    }

    fun updateMyProfile(username: String, fullName: String, bio: String, avatarUrl: String) {
        viewModelScope.launch {
            try {
                repository.updateMyProfile(username, fullName, bio, avatarUrl)
            } catch (e: Exception) {
                android.util.Log.e("SocialViewModel", "updateMyProfile failed: ${e.message}", e)
            }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            try {
                repository.deletePost(postId)
            } catch (e: Exception) {
                android.util.Log.e("SocialViewModel", "deletePost failed: ${e.message}", e)
            }
        }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch {
            try {
                repository.markAllActivitiesRead()
            } catch (e: Exception) {
                android.util.Log.e("SocialViewModel", "markAllNotificationsRead failed: ${e.message}", e)
            }
        }
    }

    fun syncWithSupabase() {
        if (!supabase.isConfigured()) return
        viewModelScope.launch {
            _isSyncing.value = true
            val success = repository.syncWithSupabase()
            _isSyncing.value = false
            _syncSuccess.emit(success)
        }
    }

    fun saveConfig(url: String, key: String, enabled: Boolean) {
        supabase.url = url
        supabase.key = key
        supabase.isEnabled = enabled
        _supabaseConfigured.value = supabase.isConfigured()
        
        if (enabled && url.isNotEmpty() && key.isNotEmpty()) {
            syncWithSupabase()
        }
    }
}
