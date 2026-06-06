package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_places")
data class FavoritePlace(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val placeType: String, // "مطعم", "فندق", "حديقة", "عمل", "منزل", "أخرى"
    val latitude: Double,
    val longitude: Double,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
