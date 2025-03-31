package com.example.foodtracker.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object MIGRATION_1_2 : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE saved_recipes ADD COLUMN servings TEXT")
    }
}