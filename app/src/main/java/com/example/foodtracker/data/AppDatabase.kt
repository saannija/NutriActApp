package com.example.foodtracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.foodtracker.model.SavedRecipe

@Database(entities = [SavedRecipe::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun savedRecipeDao(): SavedRecipeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nutriact_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}