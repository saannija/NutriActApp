package com.example.foodtracker.model

import android.os.Parcel
import android.os.Parcelable

data class Ingredient(
    val name: String = "",
    val amount: Double = 0.0,
    val unit: String = ""
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readDouble(),
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeDouble(amount)
        parcel.writeString(unit)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Ingredient> {
        override fun createFromParcel(parcel: Parcel): Ingredient {
            return Ingredient(parcel)
        }

        override fun newArray(size: Int): Array<Ingredient?> {
            return arrayOfNulls(size)
        }
    }
}

data class Recipe(
    val title: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val category: String = "",
    val cuisine: String = "",
    val tags: List<String> = emptyList(),
    val prepTime: Int = 0,
    val cookingTime: Int = 0,
    val totalTime: Int = 0,
    val servings: Int = 0,
    val dateAdded: com.google.firebase.Timestamp? = null,
    val ingredients: List<Ingredient> = emptyList(),
    val instructions: List<String> = emptyList()
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.createStringArrayList() ?: emptyList(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readParcelable(com.google.firebase.Timestamp::class.java.classLoader),
        parcel.createTypedArrayList(Ingredient.CREATOR) ?: emptyList(),
        parcel.createStringArrayList() ?: emptyList()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(title)
        parcel.writeString(description)
        parcel.writeString(imageUrl)
        parcel.writeString(category)
        parcel.writeString(cuisine)
        parcel.writeStringList(tags)
        parcel.writeInt(prepTime)
        parcel.writeInt(cookingTime)
        parcel.writeInt(totalTime)
        parcel.writeInt(servings)
        parcel.writeParcelable(dateAdded, flags)
        parcel.writeTypedList(ingredients)
        parcel.writeStringList(instructions)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Recipe> {
        override fun createFromParcel(parcel: Parcel): Recipe {
            return Recipe(parcel)
        }

        override fun newArray(size: Int): Array<Recipe?> {
            return arrayOfNulls(size)
        }
    }
}
