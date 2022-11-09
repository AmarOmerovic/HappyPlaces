package com.amaromerovic.happyplaces.data

import android.app.Application
import com.amaromerovic.happyplaces.util.HappyPlaceRoomDatabase

class HappyPlaceApp : Application() {
    val database by lazy {
        HappyPlaceRoomDatabase.getInstance(this)
    }
}