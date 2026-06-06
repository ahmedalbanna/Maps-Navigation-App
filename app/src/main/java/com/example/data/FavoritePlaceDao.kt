package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoritePlaceDao {
    @Query("SELECT * FROM favorite_places ORDER BY timestamp DESC")
    fun getAllFavorites(): Flow<List<FavoritePlace>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(place: FavoritePlace)

    @Delete
    suspend fun deleteFavorite(place: FavoritePlace)

    @Query("DELETE FROM favorite_places WHERE id = :id")
    suspend fun deleteFavoriteById(id: Int)
}
