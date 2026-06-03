package com.example.viewmodel

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
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

    // Active Heads-up Push Notification System state
    private val _currentPushNotification = MutableStateFlow<ActivityEntity?>(null)
    val currentPushNotification: StateFlow<ActivityEntity?> = _currentPushNotification.asStateFlow()

    fun sendNativeSystemNotification(title: String, message: String) {
        val context = getApplication<Application>()
        val channelId = "pulse_local_notifs"
        val notificationId = System.currentTimeMillis().toInt()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Pulse Feed Dynamics"
            val descriptionText = "Triggers local notifications for likes, comments and milestones"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        // Action intent to launch the app
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 0, intent ?: Intent(),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            // Check if permission is granted for Android 13+ is done in UI, so safe to notify
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            android.util.Log.e("SocialViewModel", "SecurityException: cannot trigger notification due to missing permission", e)
        } catch (e: Exception) {
            android.util.Log.e("SocialViewModel", "Failed to trigger native notification", e)
        }
    }

    fun triggerLocalPushNotification(activity: ActivityEntity) {
        viewModelScope.launch {
            _currentPushNotification.value = activity

            val title = when (activity.type) {
                "like" -> "❤️ New Like from @${activity.actorUsername}"
                "comment" -> "💬 Comment from @${activity.actorUsername}"
                "follow" -> "👤 New Follower @${activity.actorUsername}"
                "milestone" -> "🏆 Dynamic Milestone Achieved!"
                else -> "🔔 Pulse Activity Update"
            }
            sendNativeSystemNotification(title, activity.content)

            kotlinx.coroutines.delay(4500)
            if (_currentPushNotification.value?.id == activity.id) {
                _currentPushNotification.value = null
            }
        }
    }

    fun dismissPushNotification() {
        _currentPushNotification.value = null
    }

    fun triggerRandomSimNotification() {
        viewModelScope.launch {
            val me = repository.myProfile.firstOrNull() ?: return@launch
            val profiles = repository.allProfiles.firstOrNull() ?: emptyList()
            if (profiles.isEmpty()) return@launch
            val candidates = profiles.filter { !it.isMe }
            if (candidates.isEmpty()) return@launch
            val actor = candidates.random()
            
            val dynamicInteractions = listOf(
                "like" to listOf(
                    "liked your trending Expression description! ✨🚀",
                    "reacted with ❤️ to your recent photography entry.",
                    "absolutely adored your thoughts on design restraint.",
                    "spent 5 minutes reading your travel logs and double-tapped!"
                ),
                "follow" to listOf(
                    "started following your design ledger.",
                    "is now charting your workspace feed updates.",
                    "added you to their elite creator circle.",
                    "is looking forward to collaborating on your next feed theme!"
                ),
                "mention" to listOf(
                    "mentioned you: '@${me.username} check out this awesome layout!'",
                    "commented: 'Highly recommend @${me.username} for Material 3 design systems.'",
                    "sent a shoutout to @${me.username} on their profile wall."
                ),
                "comment" to listOf(
                    "replied: 'This has beautiful dynamic symmetry!'",
                    "replied: 'Brilliant perspective. What lens is this?'",
                    "replied: 'Inspiring ideas, keep doing outstanding database work!'",
                    "replied: 'Simple, direct, and absolutely stunning!'"
                )
            )
            
            val selectedGroup = dynamicInteractions.random()
            val type = selectedGroup.first
            val contentChoice = selectedGroup.second.random()
            
            val activity = ActivityEntity(
                id = java.util.UUID.randomUUID().toString(),
                type = type,
                actorId = actor.id,
                actorUsername = actor.username,
                actorAvatarUrl = actor.avatarUrl,
                receiverId = me.id,
                postId = null,
                content = contentChoice,
                createdAt = System.currentTimeMillis(),
                isRead = false
            )
            repository.insertLocalActivity(activity)
            triggerLocalPushNotification(activity)

            // Activity milestones trigger (every 4 notifications, trigger a dynamic progress milestone!)
            val allNotifs = repository.allActivities.firstOrNull() ?: emptyList()
            val totalCount = allNotifs.size + 1
            if (totalCount > 0 && totalCount % 3 == 0) {
                kotlinx.coroutines.delay(5000)
                val achievements = listOf(
                    "You have crossed $totalCount total interaction logs on your offline Pulse feed! 🔥✨",
                    "Your local database has logged $totalCount synchronized activity records successfully! 💻💾",
                    "Your content analytics are breaking records with a surge in local engagements!",
                    "Your localized profile network score has unlocked landmark level ${totalCount / 3}! 📈📊"
                )
                val achievementMsg = achievements.random()
                val milestoneActivity = ActivityEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    type = "milestone",
                    actorId = "system_pulse",
                    actorUsername = "pulse_system",
                    actorAvatarUrl = "",
                    receiverId = me.id,
                    postId = null,
                    content = achievementMsg,
                    createdAt = System.currentTimeMillis(),
                    isRead = false
                )
                repository.insertLocalActivity(milestoneActivity)
                triggerLocalPushNotification(milestoneActivity)
            }
        }
    }

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
                repository.updateMyProfile(username, fullName, bio, avatarUrl, "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=1000")
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
                
                // Simulate follow-back reaction loop from the creator
                val profiles = repository.allProfiles.firstOrNull() ?: emptyList()
                val targetCreator = profiles.find { it.id == targetUserId }
                if (targetCreator != null && !targetCreator.isFollowedByMe) { // We just followed them
                    kotlinx.coroutines.delay(4000)
                    val me = repository.myProfile.firstOrNull() ?: return@launch
                    val followActivity = ActivityEntity(
                        id = java.util.UUID.randomUUID().toString(),
                        type = "follow",
                        actorId = targetCreator.id,
                        actorUsername = targetCreator.username,
                        actorAvatarUrl = targetCreator.avatarUrl,
                        receiverId = me.id,
                        postId = null,
                        content = "followed you back! 🌟 Let's collaborate soon.",
                        createdAt = System.currentTimeMillis(),
                        isRead = false
                    )
                    repository.insertLocalActivity(followActivity)
                    triggerLocalPushNotification(followActivity)
                }
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
                
                // Simulate reply/reaction from author of post or other creators after comment
                kotlinx.coroutines.delay(3500)
                val profiles = repository.allProfiles.firstOrNull() ?: emptyList()
                val candidate = profiles.filter { !it.isMe }.randomOrNull()
                if (candidate != null) {
                    val me = repository.myProfile.firstOrNull() ?: return@launch
                    val replyActivity = ActivityEntity(
                        id = java.util.UUID.randomUUID().toString(),
                        type = "comment",
                        actorId = candidate.id,
                        actorUsername = candidate.username,
                        actorAvatarUrl = candidate.avatarUrl,
                        receiverId = me.id,
                        postId = postId,
                        content = "liked your comment and replied 'Agree entirely with @Jordan!'",
                        createdAt = System.currentTimeMillis(),
                        isRead = false
                    )
                    repository.insertLocalActivity(replyActivity)
                    triggerLocalPushNotification(replyActivity)
                }
            } catch (e: Exception) {
                android.util.Log.e("SocialViewModel", "addComment failed: ${e.message}", e)
            }
        }
    }

    fun deleteComment(postId: String, commentId: String) {
        viewModelScope.launch {
            try {
                repository.deleteComment(postId, commentId)
            } catch (e: Exception) {
                android.util.Log.e("SocialViewModel", "deleteComment failed: ${e.message}", e)
            }
        }
    }

    fun createPost(content: String, imageUrl: String?) {
        viewModelScope.launch {
            try {
                repository.createPost(content, imageUrl)
                
                // Multi-step incoming notifications after Jordan makes a post!
                kotlinx.coroutines.delay(5000)
                val me = repository.myProfile.firstOrNull() ?: return@launch
                val profiles = repository.allProfiles.firstOrNull() ?: emptyList()
                val elena = profiles.find { it.id == "user_elena" }
                if (elena != null) {
                    val actId1 = java.util.UUID.randomUUID().toString()
                    val likeActivity = ActivityEntity(
                        id = actId1,
                        type = "like",
                        actorId = elena.id,
                        actorUsername = elena.username,
                        actorAvatarUrl = elena.avatarUrl,
                        receiverId = me.id,
                        postId = null,
                        content = "liked your newly shared Expression! ✨🏞️",
                        createdAt = System.currentTimeMillis(),
                        isRead = false
                    )
                    repository.insertLocalActivity(likeActivity)
                    triggerLocalPushNotification(likeActivity)
                }
                
                kotlinx.coroutines.delay(4000)
                val kaito = profiles.find { it.id == "user_kaito" }
                if (kaito != null) {
                    val actId2 = java.util.UUID.randomUUID().toString()
                    val commentActivity = ActivityEntity(
                        id = actId2,
                        type = "comment",
                        actorId = kaito.id,
                        actorUsername = kaito.username,
                        actorAvatarUrl = kaito.avatarUrl,
                        receiverId = me.id,
                        postId = null,
                        content = "commented: 'Unbelievable sense of visual balance Jordan! Keep detailing!'",
                        createdAt = System.currentTimeMillis(),
                        isRead = false
                    )
                    repository.insertLocalActivity(commentActivity)
                    triggerLocalPushNotification(commentActivity)
                }
            } catch (e: Exception) {
                android.util.Log.e("SocialViewModel", "createPost failed: ${e.message}", e)
            }
        }
    }

    fun updateMyProfile(username: String, fullName: String, bio: String, avatarUrl: String, bannerUrl: String) {
        viewModelScope.launch {
            try {
                repository.updateMyProfile(username, fullName, bio, avatarUrl, bannerUrl)
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
