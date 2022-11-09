package com.amaromerovic.happyplaces

import android.Manifest
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.icu.util.Calendar
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.amaromerovic.happyplaces.data.HappyPlaceApp
import com.amaromerovic.happyplaces.data.HappyPlaceDAO
import com.amaromerovic.happyplaces.databinding.ActivityAddHappyPlaceBinding
import com.amaromerovic.happyplaces.model.HappyPlaceModel
import com.amaromerovic.happyplaces.util.Constants
import com.google.android.gms.location.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.properties.Delegates


class AddHappyPlaceActivity : AppCompatActivity() {
    private lateinit var currentImagePath: String
    private lateinit var binding: ActivityAddHappyPlaceBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var calendar = Calendar.getInstance()
    private lateinit var dateSetListener: DatePickerDialog.OnDateSetListener
    private var uri: Uri? = null
    private var imageUri: String = ""
    private lateinit var bitmap: Bitmap
    private var isDetails = false
    private var id = -1
    private var lat: Double? by Delegates.vetoable(null as Double?) { _, _, newValue ->
        newValue != null
    }
    private var long: Double? by Delegates.vetoable(null as Double?) { _, _, newValue ->
        newValue != null
    }

    private val readStoragePerm =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                openStorage()
            }
        }


    private val readCameraPerm =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                openCamera()
            }
        }

    private val locationPerm: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            permissions.entries.forEach {
                val isGranted = it.value
                if (isGranted) {
                    requestNewLocation()
                    Log.e("Lat", "$lat")
                    Log.e("Lat", "$lat")
                }
            }
        }

    private val getImageFromStorage =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val intent = result.data
                val uri = intent?.data
                if (uri != null) {
                    this.contentResolver.takePersistableUriPermission(
                        uri,
                        FLAG_GRANT_READ_URI_PERMISSION
                    )
                    binding.image.setImageURI(uri)
                    imageUri = uri.toString()
                }
            }
        }

    private val getPlace =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val intent = result.data
                val place = intent?.let { Autocomplete.getPlaceFromIntent(it) }
                if (place != null) {
                    binding.locationText.setText(place.address)
                    lat = place.latLng!!.latitude
                    long = place.latLng!!.longitude
                }
            }
        }

    private val getImageFromCamera =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                binding.image.background = null
                binding.image.setImageURI(uri)
                val source = uri?.let { ImageDecoder.createSource(this.contentResolver, it) }

                lifecycleScope.launch {
                    bitmap = source?.let { ImageDecoder.decodeBitmap(it) }!!
                    saveBitmap(UUID.randomUUID().toString(), bitmap)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddHappyPlaceBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbarAddPlace)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(this@AddHappyPlaceActivity)

        val happyPlaceDAO = (application as HappyPlaceApp).database.happyPlaceDAO()

        binding.toolbarAddPlace.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        if (!Places.isInitialized()) {
            Places.initialize(this@AddHappyPlaceActivity, resources.getString(R.string.apiKey))
        }

        isDetails = intent.getBooleanExtra(Constants.DETAILS_LEY, false)
        if (isDetails) {
            val happyPlace = Constants.getSerializable(
                this@AddHappyPlaceActivity,
                Constants.OBJECT_KEY,
                HappyPlaceModel::class.java
            )

            id = happyPlace.id
            val title = happyPlace.title
            val description = happyPlace.description
            val date = happyPlace.date
            val location = happyPlace.location
            val imageUriFromIntent = happyPlace.imageUri
            lat = happyPlace.latitude
            long = happyPlace.longitude
            imageUri = imageUriFromIntent

            if (title.isNotEmpty() && description.isNotEmpty() && date.isNotEmpty() && imageUriFromIntent.isNotEmpty() && location.isNotEmpty()) {
                binding.titleText.setText(title)
                binding.descriptionText.setText(description)
                binding.dateText.setText(date)
                binding.image.setImageURI(Uri.parse(imageUriFromIntent))
                binding.locationText.setText(location)
                imageUri = imageUriFromIntent
            }
        }



        dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, day)

            binding.dateText.setText(
                SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(
                    calendar.time
                ).toString()
            )
        }

        binding.dateText.setOnClickListener {
            DatePickerDialog(
                this@AddHappyPlaceActivity,
                dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }


        binding.locationText.setOnClickListener {
            try {
                val fields = listOf(
                    Place.Field.ID,
                    Place.Field.NAME,
                    Place.Field.LAT_LNG,
                    Place.Field.ADDRESS
                )

                val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
                    .build(this@AddHappyPlaceActivity)
                getPlace.launch(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        binding.currentLocationButton.setOnClickListener {
            if (!isLocationEnabled()) {
                Toast.makeText(
                    this,
                    "Your location provider is turned off. Please turn it on",
                    Toast.LENGTH_SHORT
                ).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)

            } else {
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                ) {
                    showRationalDialogForPermissions("Location")
                } else {
                    locationPerm.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            }
        }

        binding.addImageButton.setOnClickListener {
            val dialog = AlertDialog.Builder(this)
            dialog.setTitle("Select Action")
            val dialogItems = arrayOf("Select photo from gallery", "Capture photo from camera")
            dialog.setItems(dialogItems) { dialogInterface, i ->
                when (i) {
                    0 -> {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(
                                this,
                                Manifest.permission.READ_EXTERNAL_STORAGE
                            )
                        ) {
                            showRationalDialogForPermissions("Files and media")
                        } else {
                            readStoragePerm.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                        }
                    }
                    1 -> {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(
                                this,
                                Manifest.permission.CAMERA
                            )
                        ) {
                            showRationalDialogForPermissions("Camera")
                        } else {
                            readCameraPerm.launch(Manifest.permission.CAMERA)
                        }
                    }
                }
                dialogInterface.dismiss()
            }
            dialog.show()
        }

        binding.saveButton.setOnClickListener {
            saveHappyPlace(happyPlaceDAO)
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }


    @SuppressLint("MissingPermission")
    private fun requestNewLocation() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(100)
            .setMaxUpdateDelayMillis(500)
            .setMaxUpdates(1)
            .build()


        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.myLooper()
        )
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locatinResult: LocationResult) {
            val lastLocation = locatinResult.lastLocation

            if (lastLocation != null) {
                lat = lastLocation.latitude
            }
            if (lastLocation != null) {
                long = lastLocation.longitude
            }

            val getAddress = GetAddressFromLatLng(this@AddHappyPlaceActivity, lat!!, long!!)
            getAddress.setCustomAddressListener(object :
                GetAddressFromLatLng.AddressListener {
                override fun onAddressFound(address: String) {
                    binding.locationText.setText(address)
                }

                override fun onError() {
                    Log.e("Get address:: ", "onError: Something went wrong")
                }
            })

            lifecycleScope.launch(Dispatchers.IO) {
                getAddress.launchBackgroundProcessForRequest()
            }
        }
    }

    private fun saveHappyPlace(happyPlaceDAO: HappyPlaceDAO) {
        var isEmpty = false
        var title = ""
        var description = ""
        var date = ""
        var location = ""

        if (!checkIsEmpty(binding.titleText.text.toString(), binding.titleTextInputLayout)) {
            title = binding.titleText.text.toString().trim()
        } else {
            isEmpty = true
        }

        if (!checkIsEmpty(
                binding.descriptionText.text.toString(),
                binding.descriptionTextInputLayout
            )
        ) {
            description = binding.descriptionText.text.toString().trim()
        } else {
            isEmpty = true
        }

        if (!checkIsEmpty(binding.dateText.text.toString(), binding.dateTextInputLayout)) {
            date = binding.dateText.text.toString().trim()
        } else {
            isEmpty = true
        }

        if (!checkIsEmpty(binding.locationText.text.toString(), binding.locationTextInputLayout)) {
            location = binding.locationText.text.toString().trim()
        } else {
            isEmpty = true
        }

        if (!TextUtils.isEmpty(imageUri)) {
            binding.image.setBackgroundResource(R.drawable.image_view_border)
        } else {
            binding.image.setBackgroundResource(R.drawable.image_view_border_error)
            isEmpty = true
        }


        if (!isEmpty && !isDetails && lat != null && long != null) {
            CoroutineScope(Dispatchers.IO).launch {
                happyPlaceDAO.addHappyPlace(
                    HappyPlaceModel(
                        title = title,
                        imageUri = imageUri,
                        description = description,
                        date = date,
                        location = location,
                        latitude = lat!!.toDouble(),
                        longitude = long!!.toDouble()
                    )
                )
            }
            finish()
        } else if (!isEmpty && lat != null && long != null) {
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    happyPlaceDAO.updateHappyPlace(
                        HappyPlaceModel(
                            id = id,
                            title = title,
                            imageUri = imageUri,
                            description = description,
                            date = date,
                            location = location,
                            latitude = lat!!.toDouble(),
                            longitude = long!!.toDouble()
                        )
                    )
                }
            }

            Log.e("test", "saveHappyPlace: $title")
            finish()
        }
    }

    private fun openStorage() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.type = "image/*"
        getImageFromStorage.launch(intent)
    }

    private fun checkIsEmpty(text: String, textInputLayout: TextInputLayout): Boolean {
        return if (text.isEmpty()) {
            textInputLayout.isErrorEnabled = true
            textInputLayout.error = "Field is empty!"
            true
        } else {
            textInputLayout.isErrorEnabled = false
            false
        }
    }

    private fun showRationalDialogForPermissions(permissionName: String) {
        AlertDialog.Builder(this@AddHappyPlaceActivity)
            .setTitle("Open Settings")
            .setMessage("It looks like you have turned off the required permission for this feature. It can be enabled under the Applications Settings/Permissions/$permissionName.")
            .setPositiveButton("Go to settings") { _, _ ->
                try {
                    goToSettings()
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }.setNegativeButton("Cancel") { dialogInterface, _ ->
                dialogInterface.dismiss()
            }.show()
    }

    private fun goToSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    private fun openCamera() {
        val file = createImageFile()

        try {
            uri = FileProvider.getUriForFile(this, "com.amaromerovic.happyplaces.provider", file)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        getImageFromCamera.launch(uri)
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("HappyPlace_$timeStamp", ".jpg", imageDirectory).apply {
            currentImagePath = absolutePath
        }
    }

    private suspend fun saveBitmap(name: String, bitmap: Bitmap?) {
        withContext(Dispatchers.IO) {
            val image = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val contentValue = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "$name.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (bitmap != null) {
                    put(MediaStore.Images.Media.WIDTH, bitmap.width)
                    put(MediaStore.Images.Media.HEIGHT, bitmap.height)
                }
            }
            try {
                contentResolver.insert(image, contentValue).also {
                    if (it != null) {
                        contentResolver.openOutputStream(it).use { outputStream ->
                            if (bitmap != null) {
                                if (!bitmap.compress(
                                        Bitmap.CompressFormat.JPEG,
                                        80,
                                        outputStream
                                    )
                                ) {
                                    runOnUiThread {
                                        Toast.makeText(
                                            this@AddHappyPlaceActivity,
                                            "Something went wrong saving the image!",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                } else {
                                    runOnUiThread {
                                        imageUri = uri.toString()
                                        Toast.makeText(
                                            this@AddHappyPlaceActivity,
                                            "Image saved successfully!",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            } else {
                                runOnUiThread {
                                    Toast.makeText(
                                        this@AddHappyPlaceActivity,
                                        "Something went wrong saving the image!",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(
                                this@AddHappyPlaceActivity,
                                "Something went wrong saving the image!",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } ?: run {
                    runOnUiThread {
                        Toast.makeText(
                            this@AddHappyPlaceActivity,
                            "Something went wrong saving the image!",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(
                        this@AddHappyPlaceActivity,
                        "Something went wrong saving the image!",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}
