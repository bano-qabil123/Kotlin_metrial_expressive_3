package com.example.data.model

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

data class Comment(
    val id: String,
    val userId: String,
    val username: String,
    val fullName: String,
    val avatarUrl: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
)

object JsonParser {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val commentListType = Types.newParameterizedType(List::class.java, Comment::class.java)
    private val commentsAdapter = moshi.adapter<List<Comment>>(commentListType)

    fun commentsToJson(comments: List<Comment>): String {
        return try {
            commentsAdapter.toJson(comments)
        } catch (e: Exception) {
            "[]"
        }
    }

    fun jsonToComments(json: String?): List<Comment> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            commentsAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
