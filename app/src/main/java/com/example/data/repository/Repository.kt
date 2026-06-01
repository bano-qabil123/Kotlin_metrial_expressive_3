package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.*
import com.example.data.model.Comment
import com.example.data.model.JsonParser
import com.example.data.remote.SupabaseClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.util.UUID

class Repository(
    private val context: Context,
    private val db: AppDatabase,
    val supabaseClient: SupabaseClient
) {
    private val profileDao = db.profileDao()
    private val postDao = db.postDao()
    private val activityDao = db.activityDao()

    val myProfile: Flow<ProfileEntity?> = profileDao.getMyProfileFlow()
    val allPosts: Flow<List<PostEntity>> = postDao.getAllPostsFlow()
    val allActivities: Flow<List<ActivityEntity>> = activityDao.getAllActivitiesFlow()
    val allProfiles: Flow<List<ProfileEntity>> = profileDao.getAllProfilesFlow()

    // Generate or get my local Profile id
    suspend fun getMyProfileId(): String = withContext(Dispatchers.IO) {
        val existing = profileDao.getMyProfile()
        if (existing != null) {
            existing.id
        } else {
            val newId = UUID.randomUUID().toString()
            val defaultMe = ProfileEntity(
                id = newId,
                username = "creative_mind",
                fullName = "Jordan Sparks",
                avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
                bio = "Exploring the boundaries of Material 3 Expressive Design. Interaction designer, visual poet, coffee lover. ☕📱",
                isMe = true
            )
            profileDao.insertProfile(defaultMe)
            newId
        }
    }

    suspend fun prePopulateIfEmpty() = withContext(Dispatchers.IO) {
        // Create basic mock creator profiles if missing
        val elenaProfile = profileDao.getProfileById("user_elena")
        if (elenaProfile == null) {
            profileDao.insertProfiles(
                listOf(
                    ProfileEntity(
                        id = "user_elena",
                        username = "mountain_explorer",
                        fullName = "Elena Vance",
                        avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
                        bio = "Wilderness photographer & climber. Capturing raw beauty of remote summits. 🏔️🌲",
                        isMe = false,
                        isVerified = true,
                        isFollowedByMe = true,
                        followersCount = 1450,
                        followingCount = 210
                    ),
                    ProfileEntity(
                        id = "user_kaito",
                        username = "arch_form",
                        fullName = "Kaito Sato",
                        avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150",
                        bio = "Brutalist architect & traveler. Restraint. Texture. Symmetry. Berlin 🖤",
                        isMe = false,
                        isVerified = true,
                        isFollowedByMe = false,
                        followersCount = 980,
                        followingCount = 145
                    ),
                    ProfileEntity(
                        id = "user_alex",
                        username = "compose_dev",
                        fullName = "Alex Mercer",
                        avatarUrl = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150",
                        bio = "Jetpack Compose engineer. Material 3 designer. Crafting animations & tactile layouts. 🚀🎨",
                        isMe = false,
                        isVerified = false,
                        isFollowedByMe = false,
                        followersCount = 340,
                        followingCount = 390
                    )
                )
            )
        }

        // Check if DB is empty
        val currentPosts = postDao.getAllPostsFlow().firstOrNull() ?: emptyList()
        val currentMe = profileDao.getMyProfile()
        
        val myId = if (currentMe == null) {
            getMyProfileId()
        } else {
            currentMe.id
        }

        if (currentPosts.isEmpty()) {
            val starterPosts = listOf(
                PostEntity(
                    id = "post_1",
                    userId = "user_elena",
                    userUsername = "mountain_explorer",
                    userFullName = "Elena Vance",
                    userAvatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
                    content = "Chile's striking dramatic landscape! Traveled across the Andes, through deep salt flat deserts, and wild Patagonia. Absolute silence except for the wind. Best retreat ever. 🏔️🌲✨",
                    imageUrl = "https://images.unsplash.com/photo-1476514525535-07fb3b4ae5f1?w=800",
                    createdAt = System.currentTimeMillis() - 3600000 * 2, // 2 hours ago
                    likesCount = 124,
                    isLikedByMe = false,
                    commentsJson = JsonParser.commentsToJson(
                        listOf(
                            Comment("c_1", "user_kaito", "arch_form", "Kaito Sato", "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150", "Incredible sense of space here. What lens did you use Elena?", System.currentTimeMillis() - 3600000),
                            Comment("c_2", "user_elena", "mountain_explorer", "Elena Vance", "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150", "@arch_form Shot on a prime 24mm f/1.4 lens! Kept it simple.", System.currentTimeMillis() - 1800000)
                        )
                    )
                ),
                PostEntity(
                    id = "post_2",
                    userId = "user_kaito",
                    userUsername = "arch_form",
                    userFullName = "Kaito Sato",
                    userAvatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150",
                    content = "Symmetry, form restraint, and texture. Exploring raw Brutalist concrete forms in Berlin today. Dramatic shadows make for amazing, pure highlights. 📐🏛️🖤",
                    imageUrl = "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?w=800",
                    createdAt = System.currentTimeMillis() - 3600000 * 6, // 6 hours ago
                    likesCount = 89,
                    isLikedByMe = true,
                    commentsJson = "[]"
                ),
                PostEntity(
                    id = "post_3",
                    userId = "user_alex",
                    userUsername = "compose_dev",
                    userFullName = "Alex Mercer",
                    userAvatarUrl = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150",
                    content = "Integrating Material 3 Expressive Design with Compose. Loving the extra-curved cards, dynamic shape boundaries, and adaptive side rails! Android UI development has never felt this tactile and custom. 🚀📱🎨 #AndroidDev #M3Design",
                    imageUrl = null,
                    createdAt = System.currentTimeMillis() - 3600000 * 24, // 1 day ago
                    likesCount = 247,
                    isLikedByMe = false,
                    commentsJson = JsonParser.commentsToJson(
                        listOf(
                            Comment("c_3", "user_elena", "mountain_explorer", "Elena Vance", "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150", "The fluidity of Compose is unmatched. Love the subtle spring physics!", System.currentTimeMillis() - 3600000 * 5)
                        )
                    )
                )
            )
            postDao.insertPosts(starterPosts)
        }

        val activityCount = activityDao.getAllActivitiesFlow().firstOrNull() ?: emptyList()
        if (activityCount.isEmpty()) {
            val starterActivities = listOf(
                ActivityEntity(
                    id = "act_1",
                    type = "comment",
                    actorId = "user_kaito",
                    actorUsername = "arch_form",
                    actorAvatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150",
                    receiverId = myId,
                    postId = "post_1",
                    content = "commented on post: 'Outstanding perspective! Is this taken on medium format?'",
                    createdAt = System.currentTimeMillis() - 1000 * 60 * 45 // 45m ago
                ),
                ActivityEntity(
                    id = "act_2",
                    type = "like",
                    actorId = "user_elena",
                    actorUsername = "mountain_explorer",
                    actorAvatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
                    receiverId = myId,
                    postId = "post_1",
                    content = "liked your recent adventure update.",
                    createdAt = System.currentTimeMillis() - 1000 * 60 * 120 // 2h ago
                ),
                ActivityEntity(
                    id = "act_3",
                    type = "mention",
                    actorId = "user_alex",
                    actorUsername = "compose_dev",
                    actorAvatarUrl = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150",
                    receiverId = myId,
                    postId = "post_3",
                    content = "mentioned you in a post: '@creative_mind have you tried setting dynamic light scheme values on Android 14?'",
                    createdAt = System.currentTimeMillis() - 3600000 * 5 // 5h ago
                )
            )
            activityDao.insertActivities(starterActivities)
        }
    }

    // Fetch from Supabase, merge into local DB
    suspend fun syncWithSupabase(): Boolean = withContext(Dispatchers.IO) {
        if (!supabaseClient.isConfigured()) return@withContext false
        try {
            // Push current profile
            profileDao.getMyProfile()?.let {
                supabaseClient.syncProfile(it)
            }

            // Sync down remote profiles first
            val remoteProfiles = supabaseClient.fetchProfiles()
            if (remoteProfiles.isNotEmpty()) {
                val me = profileDao.getMyProfile()
                val mergedProfiles = remoteProfiles.map { remote ->
                    if (me != null && remote.id == me.id) {
                        // Keep our local isMe flag and isFollowedByMe status
                        me.copy(
                            username = remote.username,
                            fullName = remote.fullName,
                            avatarUrl = remote.avatarUrl,
                            bio = remote.bio,
                            isVerified = remote.isVerified,
                            followersCount = remote.followersCount,
                            followingCount = remote.followingCount
                        )
                    } else {
                        val localEntry = profileDao.getProfileById(remote.id)
                        if (localEntry != null) {
                            remote.copy(isFollowedByMe = localEntry.isFollowedByMe)
                        } else {
                            remote
                        }
                    }
                }
                profileDao.insertProfiles(mergedProfiles)
            }

            // Sync down remote posts
            val remotePosts = supabaseClient.fetchPosts()
            if (remotePosts.isNotEmpty()) {
                // To preserve "isLikedByMe" local state, we can map existing local fields or merge gracefully
                val mergedPosts = remotePosts.map { remote ->
                    val localEntry = postDao.getPostById(remote.id)
                    if (localEntry != null) {
                        remote.copy(isLikedByMe = localEntry.isLikedByMe)
                    } else {
                        remote
                    }
                }
                postDao.insertPosts(mergedPosts)
            }
            true
        } catch (e: Exception) {
            Log.e("Repository", "Sync error", e)
            false
        }
    }

    suspend fun createPost(content: String, imageUrl: String?) = withContext(Dispatchers.IO) {
        val me = profileDao.getMyProfile() ?: return@withContext
        val postId = UUID.randomUUID().toString()
        val localPost = PostEntity(
            id = postId,
            userId = me.id,
            userUsername = me.username,
            userFullName = me.fullName,
            userAvatarUrl = me.avatarUrl,
            content = content,
            imageUrl = if (imageUrl.isNullOrBlank()) null else imageUrl.trim(),
            createdAt = System.currentTimeMillis(),
            likesCount = 0,
            isLikedByMe = false,
            commentsJson = "[]"
        )
        // Store locally
        postDao.insertPost(localPost)
        Log.d("Repository", "Inserted post local: $postId")

        // Try syncing remote
        if (supabaseClient.isConfigured()) {
            supabaseClient.uploadPost(localPost, me.id)
        }
    }

    suspend fun toggleLike(postId: String) = withContext(Dispatchers.IO) {
        val post = postDao.getPostById(postId) ?: return@withContext
        val newIsLiked = !post.isLikedByMe
        val newCount = if (newIsLiked) post.likesCount + 1 else maxOf(0, post.likesCount - 1)
        val updated = post.copy(
            isLikedByMe = newIsLiked,
            likesCount = newCount
        )
        postDao.insertPost(updated)

        // Try syncing remote if configured
        if (supabaseClient.isConfigured()) {
            val myId = getMyProfileId()
            supabaseClient.uploadPost(updated, myId)
            
            // Also generate an activity notification if liked
            if (newIsLiked) {
                val me = profileDao.getMyProfile()
                if (me != null && post.userId != me.id) {
                    val activityId = UUID.randomUUID().toString()
                    val remoteActivityActivity = ActivityEntity(
                        id = activityId,
                        type = "like",
                        actorId = me.id,
                        actorUsername = me.username,
                        actorAvatarUrl = me.avatarUrl,
                        receiverId = post.userId,
                        postId = postId,
                        content = "liked your photo post.",
                        createdAt = System.currentTimeMillis()
                    )
                    // (Optional) sync activity or store locally
                    activityDao.insertActivity(remoteActivityActivity)
                }
            }
        }
    }

    suspend fun addComment(postId: String, commentText: String) = withContext(Dispatchers.IO) {
        if (commentText.isBlank()) return@withContext
        val post = postDao.getPostById(postId) ?: return@withContext
        val me = profileDao.getMyProfile() ?: return@withContext

        val currentComments = JsonParser.jsonToComments(post.commentsJson).toMutableList()
        val newComment = Comment(
            id = UUID.randomUUID().toString(),
            userId = me.id,
            username = me.username,
            fullName = me.fullName,
            avatarUrl = me.avatarUrl,
            content = commentText.trim(),
            createdAt = System.currentTimeMillis()
        )
        currentComments.add(newComment)
        val updated = post.copy(
            commentsJson = JsonParser.commentsToJson(currentComments)
        )
        postDao.insertPost(updated)

        // Try syncing remote if configured
        if (supabaseClient.isConfigured()) {
            supabaseClient.uploadPost(updated, post.userId)
            
            // Generate comment activity notification
            if (post.userId != me.id) {
                val activityId = UUID.randomUUID().toString()
                val commentActivity = ActivityEntity(
                    id = activityId,
                    type = "comment",
                    actorId = me.id,
                    actorUsername = me.username,
                    actorAvatarUrl = me.avatarUrl,
                    receiverId = post.userId,
                    postId = postId,
                    content = "commented: '${newComment.content}'",
                    createdAt = System.currentTimeMillis()
                )
                activityDao.insertActivity(commentActivity)
            }
        }
    }

    suspend fun updateMyProfile(username: String, fullName: String, bio: String, avatarUrl: String) = withContext(Dispatchers.IO) {
        val me = profileDao.getMyProfile()
        val updated = if (me != null) {
            me.copy(
                username = username.trim().lowercase(),
                fullName = fullName.trim(),
                bio = bio.trim(),
                avatarUrl = avatarUrl.trim()
            )
        } else {
            ProfileEntity(
                id = UUID.randomUUID().toString(),
                username = username.trim().lowercase(),
                fullName = fullName.trim(),
                bio = bio.trim(),
                avatarUrl = avatarUrl.trim(),
                isMe = true
            )
        }
        profileDao.insertProfile(updated)

        if (supabaseClient.isConfigured()) {
            supabaseClient.syncProfile(updated)
        }
    }

    suspend fun deletePost(postId: String) = withContext(Dispatchers.IO) {
        postDao.deletePostById(postId)
        // If supabase supports deleting, could hit remote, but we are keeping it simple!
    }

    suspend fun markAllActivitiesRead() = withContext(Dispatchers.IO) {
        activityDao.markAllAsRead()
    }

    suspend fun toggleFollow(targetUserId: String) = withContext(Dispatchers.IO) {
        val current = profileDao.getProfileById(targetUserId) ?: return@withContext
        val newIsFollowing = !current.isFollowedByMe
        val newFollowersCount = if (newIsFollowing) current.followersCount + 1 else maxOf(0, current.followersCount - 1)
        val updatedProfile = current.copy(
            isFollowedByMe = newIsFollowing,
            followersCount = newFollowersCount
        )
        profileDao.insertProfile(updatedProfile)

        // Update my own profile following count
        val me = profileDao.getMyProfile()
        if (me != null) {
            val newMyFollowingCount = if (newIsFollowing) me.followingCount + 1 else maxOf(0, me.followingCount - 1)
            profileDao.insertProfile(me.copy(followingCount = newMyFollowingCount))
        }

        // Add follow notification
        if (newIsFollowing) {
            val me = profileDao.getMyProfile()
            if (me != null) {
                activityDao.insertActivity(
                    ActivityEntity(
                        id = UUID.randomUUID().toString(),
                        type = "follow",
                        actorId = me.id,
                        actorUsername = me.username,
                        actorAvatarUrl = me.avatarUrl,
                        receiverId = targetUserId,
                        postId = null,
                        content = "started following you.",
                        createdAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }
}
