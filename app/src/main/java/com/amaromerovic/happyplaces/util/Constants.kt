package com.amaromerovic.happyplaces.util

import android.app.Activity
import android.os.Build
import java.io.Serializable

object Constants {
    const val OBJECT_KEY = "HappyPlaceObjectKey"
    const val DETAILS_LEY = "HappyPlaceDetails"

    fun <T : Serializable?> getSerializable(activity: Activity, name: String, clazz: Class<T>): T {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            activity.intent.getSerializableExtra(name, clazz)!!
        else
            activity.intent.getSerializableExtra(name) as T
    }
}