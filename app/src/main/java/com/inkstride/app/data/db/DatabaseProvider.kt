package com.inkstride.app.data.db

import android.content.Context
import androidx.room.Room

object DatabaseProvider {
    @Volatile
    private var instance: InkstrideDatabase? = null

    fun getDatabase(context: Context): InkstrideDatabase {
        return instance ?: synchronized(this) {
            val newInstance = Room.databaseBuilder(
                context.applicationContext,
                InkstrideDatabase::class.java,
                "inkstride_database"
            )
                .fallbackToDestructiveMigration(true)
                .build()
            instance = newInstance
            newInstance
        }
    }
}