package com.example.foodtracker.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.example.foodtracker.model.Recipe
import kotlinx.coroutines.tasks.await

class FirestoreService {

    private val db = FirebaseFirestore.getInstance()

    suspend fun getAllRecipes(): List<Recipe> {
        return try {
            val snapshot = db.collection("recipes").get().await()
            snapshot.documents.mapNotNull { it.toObject<Recipe>() }
        } catch (e: Exception) {
            emptyList()
        }
    }
}