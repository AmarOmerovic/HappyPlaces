package com.amaromerovic.happyplaces.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "HappyPlacesTable")
data class HappyPlaceModel(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "ID")
    val id: Int = 0,
    @ColumnInfo(name = "Title")
    var title: String,
    @ColumnInfo(name = "ImageURI")
    var imageUri: String,
    @ColumnInfo(name = "Description")
    var description: String,
    @ColumnInfo(name = "Date")
    var date: String,
    @ColumnInfo(name = "Location")
    var location: String,
    @ColumnInfo(name = "Latitude")
    var latitude: Double,
    @ColumnInfo(name = "Longitude")
    var longitude: Double
) : Serializable {
}