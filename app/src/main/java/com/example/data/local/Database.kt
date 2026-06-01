package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// --- Entities ---

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val id: String,
    val username: String,
    val fullName: String,
    val avatarUrl: String,
    val bio: String,
    val isMe: Boolean = false,
    val isVerified: Boolean = false,
    val isFollowedByMe: Boolean = false,
    val followersCount: Int = 120,
    val followingCount: Int = 85
)

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val userUsername: String,
    val userFullName: String,
    val userAvatarUrl: String,
    val content: String,
    val imageUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val likesCount: Int = 0,
    val isLikedByMe: Boolean = false,
    val commentsJson: String = "[]" // JSON array of Comments
)

@Entity(tableName = "activities")
data class ActivityEntity(
    @PrimaryKey val id: String,
    val type: String, // "like", "comment", "mention"
    val actorId: String,
    val actorUsername: String,
    val actorAvatarUrl: String,
    val receiverId: String,
    val postId: String?,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

// --- DAOs ---

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles WHERE id = :id LIMIT 1")
    suspend fun getProfileById(id: String): ProfileEntity?

    @Query("SELECT * FROM profiles WHERE isMe = 1 LIMIT 1")
    fun getMyProfileFlow(): Flow<ProfileEntity?>

    @Query("SELECT * FROM profiles WHERE isMe = 1 LIMIT 1")
    suspend fun getMyProfile(): ProfileEntity?

    @Query("SELECT * FROM profiles ORDER BY username ASC")
    fun getAllProfilesFlow(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles ORDER BY username ASC")
    suspend fun getAllProfiles(): List<ProfileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfiles(profiles: List<ProfileEntity>)

    @Update
    suspend fun updateProfile(profile: ProfileEntity)
    
    @Query("DELETE FROM profiles")
    suspend fun clearAll()
}

@Dao
interface PostDao {
    @Query("SELECT * FROM posts ORDER BY createdAt DESC")
    fun getAllPostsFlow(): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE id = :id LIMIT 1")
    suspend fun getPostById(id: String): PostEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: PostEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<PostEntity>)

    @Update
    suspend fun updatePost(post: PostEntity)

    @Query("DELETE FROM posts WHERE id = :postId")
    suspend fun deletePostById(postId: String)

    @Query("DELETE FROM posts")
    suspend fun clearAll()
}

@Dao
interface ActivityDao {
    @Query("SELECT * FROM activities ORDER BY createdAt DESC")
    fun getAllActivitiesFlow(): Flow<List<ActivityEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: ActivityEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivities(activities: List<ActivityEntity>)

    @Query("UPDATE activities SET isRead = 1")
    suspend fun markAllAsRead()

    @Query("DELETE FROM activities WHERE id = :id")
    suspend fun deleteActivityById(id: String)

    @Query("DELETE FROM activities")
    suspend fun clearAll()
}

// --- AppDatabase ---

@Database(entities = [ProfileEntity::class, PostEntity::class, ActivityEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun postDao(): PostDao
    abstract fun activityDao(): ActivityDao
}
