package com.example.data

import kotlinx.coroutines.flow.Flow

class FavoritePlaceRepository(private val favoritePlaceDao: FavoritePlaceDao) {
    val allFavorites: Flow<List<FavoritePlace>> = favoritePlaceDao.getAllFavorites()

    suspend fun insert(place: FavoritePlace) {
        favoritePlaceDao.insertFavorite(place)
    }

    suspend fun delete(place: FavoritePlace) {
        favoritePlaceDao.deleteFavorite(place)
    }

    suspend fun deleteById(id: Int) {
        favoritePlaceDao.deleteFavoriteById(id)
    }
}
