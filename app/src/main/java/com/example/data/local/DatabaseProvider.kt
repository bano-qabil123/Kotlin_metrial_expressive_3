package com.example.data.local

import android.content.Context
import androidx.room.Room
import com.example.data.remote.SupabaseClient
import com.example.data.repository.Repository

object DatabaseProvider {
    @Volatile
    private var database: AppDatabase? = null
    @Volatile
    private var repository: Repository? = null

    fun getDatabase(context: Context): AppDatabase {
        return database ?: synchronized(this) {
            database ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "pulse_social_db"
            )
            .fallbackToDestructiveMigration()
            .build().also { database = it }
        }
    }

    fun getRepository(context: Context): Repository {
        return repository ?: synchronized(this) {
            val db = getDatabase(context)
            val client = SupabaseClient(context.applicationContext)
            val repo = repository ?: Repository(context.applicationContext, db, client)
            repository = repo
            repo
        }
    }
}
