package com.amaromerovic.happyplaces

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.withContext
import java.util.*

class GetAddressFromLatLng(
    context: Context,
    private val lat: Double,
    private val lng: Double
) {

    private val geocoder: Geocoder =
        Geocoder(context, Locale.getDefault())
    private lateinit var mAddressListener: AddressListener


    suspend fun launchBackgroundProcessForRequest() {
        val address = getAddress()

        withContext(Main) {
            if (address.isEmpty()) {
                mAddressListener.onError()
            } else {
                mAddressListener.onAddressFound(address)
            }
        }
    }


    private fun getAddress(): String {
        var location = ""
        try {
            if (Build.VERSION.SDK_INT >= 33) {
                geocoder.getFromLocation(lat, lng, 1) { addresses ->
                    if (addresses.isNotEmpty()) {
                        val address: Address = addresses[0]
                        val sb = StringBuilder()
                        for (i in 0..address.maxAddressLineIndex) {
                            sb.append(address.getAddressLine(i) + " ")
                        }
                        sb.deleteCharAt(sb.length - 1)

                        location = sb.toString()
                    }
                }
            } else {
                val addressList: List<Address>? = geocoder.getFromLocation(lat, lng, 1)
                if (!addressList.isNullOrEmpty()) {
                    val address: Address = addressList[0]
                    val sb = StringBuilder()
                    for (i in 0..address.maxAddressLineIndex) {
                        sb.append(address.getAddressLine(i) + " ")
                    }
                    sb.deleteCharAt(sb.length - 1)

                    location = sb.toString()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return location
    }

    fun setCustomAddressListener(addressListener: AddressListener) {
        this.mAddressListener = addressListener
    }

    interface AddressListener {
        fun onAddressFound(address: String)
        fun onError()
    }

}
