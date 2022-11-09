package com.amaromerovic.happyplaces.data

import androidx.room.*
import com.amaromerovic.happyplaces.model.HappyPlaceModel
import kotlinx.coroutines.flow.Flow

@Dao
interface HappyPlaceDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addHappyPlace(happyPlace: HappyPlaceModel)

    @Delete
    fun deleteHappyPlace(happyPlace: HappyPlaceModel)

    @Update
    fun updateHappyPlace(happyPlace: HappyPlaceModel)

    @Query("SELECT * FROM `HappyPlacesTable` ORDER BY id ASC")
    fun getAllHappyPlaces(): Flow<List<HappyPlaceModel>>

    @Query("SELECT * FROM `HappyPlacesTable` WHERE HappyPlacesTable.id = :id")
    fun getHappyPlace(id: Int): Flow<HappyPlaceModel>

}