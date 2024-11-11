package com.example.myapitest

import CarAdapter
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapitest.service.RetrofitClient
import com.example.myapitest.service.RetrofitClient.safeApiCall
import com.example.myapitest.databinding.ActivityMainBinding
import com.example.myapitest.model.CarDetails
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        requestLocationPermission()
        setupView()
    }

    override fun onResume() {
        super.onResume()
        fetchItems()
    }

    private fun setupView() {
        binding.rvCars.layoutManager = LinearLayoutManager(this)

        binding.srlCars.setOnRefreshListener {
            binding.srlCars.isRefreshing = true
            fetchItems()
        }

        binding.btAddCar.setOnClickListener {
            startActivity(CarActivity.newIntent(this, ""))
        }
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    private fun fetchItems() {
        CoroutineScope(Dispatchers.IO).launch {
            val result = safeApiCall { RetrofitClient.apiService.getCars() }
            withContext(Dispatchers.Main) {
                binding.srlCars.isRefreshing = false
                when (result) {
                    is RetrofitClient.Result.Error -> {}
                    is RetrofitClient.Result.Success -> handleOnSuccess(result.data)
                }
            }
        }
    }

    private fun handleOnSuccess(data: List<CarDetails>) {
        val adapter = CarAdapter(data) {
            startActivity(
                CarActivity.newIntent(
                    this,
                    it.id
                )
            )
        }
        binding.rvCars.adapter = adapter
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.iLogout -> {
                AlertDialog.Builder(this)
                    .setTitle(getString(R.string.sair))
                    .setMessage(getString(R.string.tem_certeza_que_deseja_realizar_esta_acao))
                    .setPositiveButton(getString(R.string.sim)) { _, _ ->
                        onLogout()
                    }
                    .setNegativeButton(getString(R.string.nao)) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun onLogout() {
        FirebaseAuth.getInstance().signOut()
        val intent = LoginActivity.newIntent(this)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 100
        fun newIntent(context: Context) = Intent(context, MainActivity::class.java)
    }
}
