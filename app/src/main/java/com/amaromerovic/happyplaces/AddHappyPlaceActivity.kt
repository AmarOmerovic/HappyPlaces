package com.amaromerovic.happyplaces

import android.Manifest
import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Intent
import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.icu.util.Calendar
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
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
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class AddHappyPlaceActivity : AppCompatActivity() {
    private lateinit var currentImagePath: String
    private lateinit var binding: ActivityAddHappyPlaceBinding
    private var calendar = Calendar.getInstance()
    private lateinit var dateSetListener: DatePickerDialog.OnDateSetListener
    private var uri: Uri? = null
    private var imageUri: String = ""
    private lateinit var bitmap: Bitmap
    private var isDetails = false
    private var id = -1


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

        val happyPlaceDAO = (application as HappyPlaceApp).database.happyPlaceDAO()

        binding.toolbarAddPlace.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        isDetails = intent.getBooleanExtra("HappyPlaceDetails", false)
        if (isDetails) {
            id = intent.getIntExtra("HappyPlaceIdKey", -1)
            val title = intent.getStringExtra("HappyPlaceTitleKey")
            val description = intent.getStringExtra("HappyPlaceDescriptionKey")
            val date = intent.getStringExtra("HappyPlaceDateKey")
            val location = intent.getStringExtra("HappyPlaceLocationKey")
            val imageUriFromIntent = intent.getStringExtra("HappyPlaceImageKey")
            imageUri = imageUriFromIntent!!

            if (title!!.isNotEmpty() && description!!.isNotEmpty() && date!!.isNotEmpty() && imageUriFromIntent.isNotEmpty() && location!!.isNotEmpty()) {
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


        if (!isEmpty && !isDetails) {
            CoroutineScope(Dispatchers.IO).launch {
                happyPlaceDAO.addHappyPlace(
                    HappyPlaceModel(
                        title = title,
                        imageUri = imageUri,
                        description = description,
                        date = date,
                        location = location,
                        latitude = 12.3,
                        longitude = 23.4
                    )
                )
            }
            finish()
        } else if (!isEmpty) {
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
                            latitude = 12.3,
                            longitude = 23.4
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