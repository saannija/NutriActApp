package com.example.foodtracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.foodtracker.model.SavedRecipe
import com.example.foodtracker.util.Converters

@Database(
    entities = [SavedRecipe::class, IngredientCheckboxState::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao
    // abstract fun ingredientCheckboxStateDao(): IngredientCheckboxStateDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `ingredient_checkbox_states` (" +
                            "`id` TEXT NOT NULL, " +
                            "`userId` TEXT NOT NULL, " +
                            "`recipeTitle` TEXT NOT NULL, " +
                            "`ingredientName` TEXT NOT NULL, " +
                            "`isChecked` INTEGER NOT NULL, " +
                            "PRIMARY KEY(`id`))"
                )
            }
        }

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