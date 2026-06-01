package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.data.local.PostEntity
import com.example.data.local.ProfileEntity
import com.example.data.model.Comment
import com.example.data.model.JsonParser
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class SupabaseClient(private val context: Context) {

    private val sharedPrefs = context.getSharedPreferences("supabase_prefs", Context.MODE_PRIVATE)

    var url: String
        get() = sharedPrefs.getString("url", "https://ztffxkvewfutxqolkjit.supabase.co") ?: "https://ztffxkvewfutxqolkjit.supabase.co"
        set(value) = sharedPrefs.edit().putString("url", value.trim()).apply()

    var key: String
        get() = sharedPrefs.getString("key", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Inp0ZmZ4a3Zld2Z1dHhxb2xraml0Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3Nzk2NDE4ODksImV4cCI6MjA5NTIxNzg4OX0.B2so4HvwuNc7qR8FGhHewB98cyj4DW0_Ub-3p76Up_s") ?: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Inp0ZmZ4a3Zld2Z1dHhxb2xraml0Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3Nzk2NDE4ODksImV4cCI6MjA5NTIxNzg4OX0.B2so4HvwuNc7qR8FGhHewB98cyj4DW0_Ub-3p76Up_s"
        set(value) = sharedPrefs.edit().putString("key", value.trim()).apply()

    var isEnabled: Boolean
        get() = sharedPrefs.getBoolean("enabled", true)
        set(value) = sharedPrefs.edit().putBoolean("enabled", value).apply()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    fun isConfigured(): Boolean {
        return isEnabled && url.isNotEmpty() && key.isNotEmpty()
    }

    suspend fun testConnection(): Boolean = withContext(Dispatchers.IO) {
        if (url.isEmpty() || key.isEmpty()) return@withContext false
        val formattedUrl = if (url.endsWith("/")) url else "$url/"
        val requestUrl = "${formattedUrl}rest/v1/posts?select=id&limit=1"

        try {
            val request = Request.Builder()
                .url(requestUrl)
                .addHeader("apikey", key)
                .addHeader("Authorization", "Bearer $key")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                Log.d("SupabaseClient", "Test connection response code: ${response.code}")
                response.isSuccessful || response.code == 404 || response.code == 400 // schema matches of some kind
            }
        } catch (e: Exception) {
            Log.e("SupabaseClient", "Connection test failed", e)
            false
        }
    }

    suspend fun fetchPosts(): List<PostEntity> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext emptyList()
        val formattedUrl = if (url.endsWith("/")) url else "$url/"
        val requestUrl = "${formattedUrl}rest/v1/posts?select=*,profiles(username,full_name,avatar_url)&order=created_at.desc"

        try {
            val request = Request.Builder()
                .url(requestUrl)
                .addHeader("apikey", key)
                .addHeader("Authorization", "Bearer $key")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("SupabaseClient", "Fetch posts failed: ${response.code} ${response.message}")
                    return@withContext emptyList()
                }

                val bodyString = response.body?.string() ?: return@withContext emptyList()
                val jsonArray = JSONArray(bodyString)
                val posts = mutableListOf<PostEntity>()

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val id = obj.optString("id", "")
                    val content = obj.optString("content", "")
                    val imageUrl = obj.optString("image_url", null)
                    
                    // Supabase timestamp is often ISO string, convert to Long or use current
                    val createdAtStr = obj.optString("created_at", "")
                    val createdAt = parseIsoTimestamp(createdAtStr)
                    
                    val likesCount = obj.optInt("likes_count", 0)
                    val commentsJson = obj.optString("comments_json", "[]")

                    // Embedded Profile join
                    val profileObj = obj.optJSONObject("profiles")
                    val userId = obj.optString("user_id", "")
                    val username = profileObj?.optString("username", "anonymous") ?: "anonymous"
                    val fullName = profileObj?.optString("full_name", "Anonymous User") ?: "Anonymous User"
                    val avatarUrl = profileObj?.optString("avatar_url", "") ?: ""

                    posts.add(
                        PostEntity(
                            id = id,
                            userId = userId,
                            userUsername = username,
                            userFullName = fullName,
                            userAvatarUrl = avatarUrl,
                            content = content,
                            imageUrl = imageUrl,
                            createdAt = createdAt,
                            likesCount = likesCount,
                            commentsJson = commentsJson
                        )
                    )
                }
                posts
            }
        } catch (e: Exception) {
            Log.e("SupabaseClient", "Exception fetching remote posts", e)
            emptyList()
        }
    }

    suspend fun uploadPost(post: PostEntity, currentUserId: String): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext false
        val formattedUrl = if (url.endsWith("/")) url else "$url/"
        val requestUrl = "${formattedUrl}rest/v1/posts"

        try {
            val jsonBody = JSONObject().apply {
                put("id", post.id)
                put("user_id", currentUserId)
                put("content", post.content)
                put("image_url", post.imageUrl)
                put("likes_count", post.likesCount)
                put("comments_json", post.commentsJson)
            }.toString()

            val request = Request.Builder()
                .url(requestUrl)
                .addHeader("apikey", key)
                .addHeader("Authorization", "Bearer $key")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("SupabaseClient", "Upload post failed: ${response.code} ${response.message}")
                }
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e("SupabaseClient", "Exception uploading post", e)
            false
        }
    }

    suspend fun syncProfile(profile: ProfileEntity): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext false
        val formattedUrl = if (url.endsWith("/")) url else "$url/"
        val requestUrl = "${formattedUrl}rest/v1/profiles"

        try {
            val jsonBody = JSONObject().apply {
                put("id", profile.id)
                put("username", profile.username)
                put("full_name", profile.fullName)
                put("avatar_url", profile.avatarUrl)
                put("bio", profile.bio)
            }.toString()

            val request = Request.Builder()
                .url(requestUrl)
                .addHeader("apikey", key)
                .addHeader("Authorization", "Bearer $key")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e("SupabaseClient", "Exception syncing profile", e)
            false
        }
    }

    suspend fun fetchProfiles(): List<ProfileEntity> = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext emptyList()
        val formattedUrl = if (url.endsWith("/")) url else "$url/"
        val requestUrl = "${formattedUrl}rest/v1/profiles?select=*"
        try {
            val request = Request.Builder()
                .url(requestUrl)
                .addHeader("apikey", key)
                .addHeader("Authorization", "Bearer $key")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("SupabaseClient", "Fetch profiles failed: ${response.code} ${response.message}")
                    return@withContext emptyList()
                }
                val bodyString = response.body?.string() ?: return@withContext emptyList()
                val jsonArray = JSONArray(bodyString)
                val profiles = mutableListOf<ProfileEntity>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val id = obj.optString("id", "")
                    val username = obj.optString("username", "")
                    val fullName = obj.optString("full_name", "")
                    val avatarUrl = obj.optString("avatar_url", "")
                    val bio = obj.optString("bio", "")
                    val isVerified = obj.optBoolean("is_verified", false) || obj.optBoolean("verified", false)
                    val followersCount = obj.optInt("followers_count", 120)
                    val followingCount = obj.optInt("following_count", 85)
                    profiles.add(
                        ProfileEntity(
                            id = id,
                            username = username,
                            fullName = fullName,
                            avatarUrl = avatarUrl,
                            bio = bio,
                            isMe = false,
                            isVerified = isVerified,
                            followersCount = followersCount,
                            followingCount = followingCount
                        )
                    )
                }
                profiles
            }
        } catch (e: Exception) {
            Log.e("SupabaseClient", "Exception fetching profiles", e)
            emptyList()
        }
    }

    private fun parseIsoTimestamp(isoString: String): Long {
        return try {
            // Quick and clean ISO8601 parsing fallback
            if (isoString.isEmpty()) System.currentTimeMillis()
            else {
                // Remove millisecond and timezone parts for simple parse, or try standard formats
                // Jetpack core-ktx has instant/date helpers, let's use a standard pattern
                java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).parse(isoString)?.time 
                    ?: System.currentTimeMillis()
            }
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}
