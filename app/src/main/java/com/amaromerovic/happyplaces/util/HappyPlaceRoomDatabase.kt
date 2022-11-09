package com.amaromerovic.happyplaces.util

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.amaromerovic.happyplaces.data.HappyPlaceDAO
import com.amaromerovic.happyplaces.model.HappyPlaceModel

@Database(entities = [HappyPlaceModel::class], version = 1, exportSchema = false)
abstract class HappyPlaceRoomDatabase : RoomDatabase() {
    abstract fun happyPlaceDAO(): HappyPlaceDAO

    companion object {
        @Volatile
        private var INSTANCE: HappyPlaceRoomDatabase? = null

        fun getInstance(context: Context): HappyPlaceRoomDatabase {
            synchronized(this) {
                var instance = INSTANCE
                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        HappyPlaceRoomDatabase::class.java,
                        "HappyPlacesTable"
                    ).fallbackToDestructiveMigration().build()
                    INSTANCE = instance
                }
                return instance
            }
        }
    }

}