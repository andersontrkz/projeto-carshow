package com.example.myapitest

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.myapitest.service.RetrofitClient
import com.example.myapitest.service.RetrofitClient.safeApiCall
import com.example.myapitest.databinding.ActivityCarBinding
import com.example.myapitest.model.Car
import com.example.myapitest.model.CarDetails
import com.example.myapitest.model.Place
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.UUID

class CarActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var binding: ActivityCarBinding
    private lateinit var car: Car
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var imageUri: Uri
    private var imageFile: File? = null
    private var currentLocation: LatLng? = null
    private lateinit var googleMap: GoogleMap

    private val cameraLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            binding.etImageUrl.setText(imageUri.path)
            binding.btnCameraRemove.visibility = View.VISIBLE
            binding.btnCameraAdd.visibility = View.GONE
            uploadImageToFirebase()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCarBinding.inflate(layoutInflater)
        enableEdgeToEdge()

        setContentView(binding.root)
        setupView()
        loadCar()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    private fun setupView() {
        binding.btBack.setOnClickListener {
            onBackPressed()
        }
        binding.btDelete.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.deletar))
                .setMessage(getString(R.string.tem_certeza_que_deseja_realizar_esta_acao))
                .setPositiveButton(getString(R.string.sim)) { _, _ ->
                    deleteCar()
                }
                .setNegativeButton(getString(R.string.nao)) { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
        binding.btSave.setOnClickListener {
            saveCar()
        }
        binding.btnCameraAdd.setOnClickListener {
            takePicture()
        }
        binding.btnCameraRemove.setOnClickListener {
            binding.etImageUrl.setText("")
            binding.btnCameraRemove.visibility = View.GONE
            binding.btnCameraAdd.visibility = View.VISIBLE

            Picasso.get()
                .load(R.drawable.directions_car)
                .placeholder(R.drawable.downloading)
                .error(R.drawable.directions_car)
                .into(binding.ivImage)        }
        if (!intent.getStringExtra(ID).isNullOrBlank()) {
            binding.btDelete.visibility = View.VISIBLE
        }

        val fields = listOf(
            binding.etModel,
            binding.etYear,
            binding.etLicence,
            binding.etImageUrl
        )

        fields.forEach { field ->
            field.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(charSequence: CharSequence?, start: Int, count: Int, after: Int) {
                    // TODO
                }

                override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {
                    // TODO
                }

                override fun afterTextChanged(editable: Editable?) {
                    checkIfAllFieldsFilled()
                }
            })
        }

        checkIfAllFieldsFilled()
    }

    private fun loadCar() {
        val itemId = intent.getStringExtra(ID)

        itemId?.let {
            CoroutineScope(Dispatchers.IO).launch {
                val result = safeApiCall { RetrofitClient.apiService.getCar(itemId) }

                withContext(Dispatchers.Main) {
                    when (result) {
                        is RetrofitClient.Result.Error -> {}
                        is RetrofitClient.Result.Success<*> -> {
                            car = result.data as Car
                            handleSuccess()
                        }
                    }
                }
            }
        }
    }

    private fun saveCar() {
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            loadCurrentLocation()
        } else {
            requestLocationPermission()
        }
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    private fun saveCarToDatabase() {
        currentLocation?.let { location ->
            CoroutineScope(Dispatchers.IO).launch {
                val result = safeApiCall {
                    if (!intent.getStringExtra(ID).isNullOrBlank()) {
                        RetrofitClient.apiService.updateCar(
                            car.id,
                            car.value.copy(
                                name = binding.etModel.text.toString(),
                                licence = binding.etLicence.text.toString(),
                                year = binding.etYear.text.toString(),
                                imageUrl = binding.etImageUrl.text.toString(),
                                place = Place(location.latitude, location.longitude)
                            )
                        )
                    } else {
                        RetrofitClient.apiService.addCar(
                            CarDetails(
                                id = UUID.randomUUID().toString(),
                                name = binding.etModel.text.toString(),
                                year = binding.etYear.text.toString(),
                                licence = binding.etLicence.text.toString(),
                                imageUrl = binding.etImageUrl.text.toString(),
                                place = Place(location.latitude, location.longitude)
                            )
                        )
                    }
                }
                withContext(Dispatchers.Main) {
                    when (result) {
                        is RetrofitClient.Result.Error -> {
                            Toast.makeText(
                                this@CarActivity,
                                getString(R.string.operacao_falha),
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        is RetrofitClient.Result.Success<*> -> {
                            Toast.makeText(
                                this@CarActivity,
                                getString(R.string.operacao_sucesso),
                                Toast.LENGTH_SHORT
                            ).show()
                            finish()
                        }
                    }
                }
            }
        }
    }

    private fun deleteCar() {
        CoroutineScope(Dispatchers.IO).launch {
            val result = safeApiCall { RetrofitClient.apiService.deleteCar(car.id) }

            withContext(Dispatchers.Main) {
                when (result) {
                    is RetrofitClient.Result.Error -> {
                        Toast.makeText(
                            this@CarActivity,
                            getString(R.string.operacao_falha),
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    is RetrofitClient.Result.Success -> {
                        Toast.makeText(
                            this@CarActivity,
                            getString(R.string.operacao_sucesso),
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                }
            }
        }
    }

    private fun handleSuccess() {
        binding.etModel.setText(car.value.name)
        binding.etYear.setText(car.value.year)
        binding.etLicence.setText(car.value.licence)
        binding.etImageUrl.setText(car.value.imageUrl)

        if (binding.etImageUrl.text.isNotBlank()) {
            binding.btnCameraRemove.visibility = View.VISIBLE
            binding.btnCameraAdd.visibility = View.GONE

            Picasso.get()
                .load(car.value.imageUrl)
                .placeholder(R.drawable.downloading)
                .error(R.drawable.directions_car)
                .into(binding.ivImage)
        }

        setupGoogleMap()
    }

    private fun takePicture() {
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            openCamera()
        } else {
            requestCameraPermission()
        }
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        imageUri = createImageUri()
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        cameraLauncher.launch(intent)
    }

    private fun createImageUri(): Uri {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_${timeStamp}_"

        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        imageFile = File.createTempFile(
            imageFileName,
            ".jpg",
            storageDir
        )

        val uri = FileProvider.getUriForFile(
            this,
            "com.example.myapitest.fileprovider",
            imageFile!!
        )
        return uri
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.CAMERA),
            CAMERA_REQUEST_CODE
        )
    }

    @SuppressLint("MissingPermission")
    private fun loadCurrentLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            currentLocation = if (location != null) {
                LatLng(location.latitude, location.longitude)
            } else {
                LatLng(0.0, 0.0)
            }
            saveCarToDatabase()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                Toast.makeText(
                    this@CarActivity,
                    getString(R.string.permissao_negada),
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadCurrentLocation()
            } else {
                currentLocation = LatLng(0.0, 0.0)
                saveCarToDatabase()
            }
        }
    }

    private fun uploadImageToFirebase() {
        val storageRef = FirebaseStorage.getInstance().reference
        val imagesRef = storageRef.child("images/${UUID.randomUUID()}.jpg")
        val baos = ByteArrayOutputStream()
        val imageBitmap = BitmapFactory.decodeFile(imageFile!!.path)

        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)

        val data = baos.toByteArray()

        changeImageLoading(true)

        imagesRef.putBytes(data)
            .addOnFailureListener {
                changeImageLoading(false)
                Toast.makeText(this, getString(R.string.operacao_falha), Toast.LENGTH_SHORT).show()
            }
            .addOnSuccessListener {
                changeImageLoading(false)
                imagesRef.downloadUrl.addOnSuccessListener { uri ->
                    binding.etImageUrl.setText(getFirebaseStorageUrl(uri.path.toString()))

                    val imageUrl = getFirebaseStorageUrl(uri.path.toString())

                    Picasso.get()
                        .load(imageUrl)
                        .placeholder(R.drawable.downloading)
                        .error(R.drawable.directions_car)
                        .into(binding.ivImage)
                }
            }
    }

    private fun changeImageLoading(isLoading: Boolean) {
        if (isLoading) {
            binding.pbImage.visibility = View.VISIBLE
            binding.btnCameraAdd.isEnabled = false
            binding.btSave.isEnabled = false
            binding.ivImage.visibility = View.INVISIBLE

            Picasso.get()
                .load(R.drawable.directions_car)
                .placeholder(R.drawable.downloading)
                .error(R.drawable.directions_car)
                .into(binding.ivImage)
        } else {
            binding.pbImage.visibility = View.GONE
            binding.btnCameraAdd.isEnabled = true
            binding.btSave.isEnabled = true
            binding.ivImage.visibility = View.VISIBLE
        }
    }

    private fun getFirebaseStorageUrl(completeUrl: String): String {
        val token = "2c44bfbd-2e4c-4dca-906f-b9496ba5f189"
        val pathPrefix = "/o/images/"
        val startIndex = completeUrl.indexOf(pathPrefix) + pathPrefix.length

        val uuid = completeUrl.substring(startIndex)

        val encodedUuid = uuid.replace("/", "%2F")

        return "https://firebasestorage.googleapis.com/v0/b/my-api-test-bfbab.firebasestorage.app/o/images%2F$encodedUuid?alt=media&token=$token"
    }

    private fun setupGoogleMap() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(mapReady: GoogleMap) {
        googleMap = mapReady
        if (::car.isInitialized && car.value.place.long != 0.0 && car.value.place.long != 0.0) {
            loadItemLocationInGoogleMap()
        } else {
            Toast.makeText(this, getString(R.string.localizaco_indisponivel), Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadItemLocationInGoogleMap() {
        car.value.place.let {
            binding.googleMapContent.visibility = View.VISIBLE
            val latLng = LatLng(car.value.place.lat, car.value.place.long)

            googleMap.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(car.value.name)
            )

            googleMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    latLng,
                    17f
                )
            )
        }
    }

    private fun checkIfAllFieldsFilled() {
        val model = binding.etModel.text.toString().trim()
        val year = binding.etYear.text.toString().trim()
        val licence = binding.etLicence.text.toString().trim()
        val imageUrl = binding.etImageUrl.text.toString().trim()

        val isEnabled = model.isNotEmpty() && year.isNotEmpty() && licence.isNotEmpty() && imageUrl.isNotEmpty()

        if (isEnabled) {
            binding.btSave.setBackgroundColor(Color.parseColor("#228822"))  // original enabled color (greenish)
        } else {
            binding.btSave.setBackgroundColor(Color.parseColor("#B0B0B0"))  // disabled color (light gray)
        }

        binding.btSave.isEnabled = isEnabled
    }

    companion object {
        private const val ID = "ID"
        private const val LOCATION_PERMISSION_REQUEST_CODE = 100
        private const val CAMERA_REQUEST_CODE = 101
        fun newIntent(
            context: Context,
            itemId: String
        ) = Intent(context, CarActivity::class.java).apply {
            putExtra(ID, itemId)
        }
    }
}
