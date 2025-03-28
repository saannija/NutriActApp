package com.example.foodtracker

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class BarcodeScannerActivity : AppCompatActivity() {

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var barcodeScanner: BarcodeScanner
    private lateinit var previewView: PreviewView
//    private lateinit var btnScanExpiry: Button
    private lateinit var barcodeAnalyzer: BarcodeAnalyzer

    private val CAMERA_PERMISSION_REQUEST_CODE = 100
    private val EXPIRATION_SCAN_REQUEST_CODE = 201

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_barcode_scanning)

        previewView = findViewById(R.id.previewView)
//        btnScanExpiry = findViewById(R.id.btnScanExpiry)

        // Request camera permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE
            )
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
        barcodeScanner = BarcodeScanning.getClient()

//        btnScanExpiry.setOnClickListener {
//            val intent = Intent(this, ExpirationScannerActivity::class.java)
//            startActivityForResult(intent, EXPIRATION_SCAN_REQUEST_CODE)
//        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            // Image analyzer
            barcodeAnalyzer = BarcodeAnalyzer()
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, barcodeAnalyzer)
                }

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
                )

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        barcodeScanner.close()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "Camera permission not granted.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    private fun fetchProductDetails(barcode: String) {
        val apiUrl = "$OPEN_FOOD_FACTS_API_BASE_URL$barcode.json"

        val request = StringRequest(
            Request.Method.GET,
            apiUrl,
            { response ->
                try {
                    val jsonObject = JSONObject(response)
                    if (jsonObject.has("product")) {
                        val product = jsonObject.getJSONObject("product")
                        val productName = product.optString("product_name", "Unknown Product")
                        val brand = product.optString("brands", "Unknown Brand")
                        val category = product.optString("categories", "Unknown Category")
                        val quantity = product.optString("quantity", "1")
                        val expirationDate = product.optString("expiration_date", "")

                        // Show a confirmation dialog with Add/Cancel options
                        showProductDialog(productName, brand, category, quantity, expirationDate)

                    } else {
                        showToast("Product Not Found")
                        setResult(Activity.RESULT_CANCELED)
                        finish()
                    }
                } catch (e: JSONException) {
                    Log.e(TAG, "JSON Parsing Error: ${e.message}")
                    showToast("Error Parsing Product Data")
                    setResult(Activity.RESULT_CANCELED)
                    finish()
                }
            },
            { error ->
                Log.e(TAG, "API Request Failed: ${error.message}")
                showToast("Error Fetching Product")
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
        )

        val requestQueue = Volley.newRequestQueue(this)
        requestQueue.add(request)
    }

    private fun showProductDialog(
        productName: String, brand: String, category: String,
        quantity: String, expirationDate: String
    ) {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Product Found")
        builder.setMessage(
            "Name: $productName\nBrand: $brand\nCategory: $category\nQuantity: $quantity\nExpiration Date: $expirationDate"
        )

        // "Add" button
        builder.setPositiveButton("Add") { _, _ ->
            val intent= Intent()
            intent.putExtra("productName", productName)
            intent.putExtra("brand", brand)
            intent.putExtra("category", category)
            intent.putExtra("quantity", quantity)
            intent.putExtra("expirationDate", expirationDate)
            setResult(Activity.RESULT_OK, intent)
            finish() // Finish after setting the result
        }

        // "Cancel" button
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        builder.show()
    }

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    inner class BarcodeAnalyzer : ImageAnalysis.Analyzer {
        private var scanningPaused = false

        @androidx.annotation.OptIn(ExperimentalGetImage::class)
        override fun analyze(imageProxy: ImageProxy) {
            if (scanningPaused) {
                imageProxy.close()
                return
            }

            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

                barcodeScanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        for (barcode in barcodes) {
                            if (barcode.valueType == Barcode.TYPE_PRODUCT) {
                                val barcodeValue = barcode.rawValue
                                if (!barcodeValue.isNullOrEmpty()) {
                                    Log.d(TAG, "Barcode Value: $barcodeValue")
                                    fetchProductDetails(barcodeValue)
                                    scanningPaused = true // Prevent scanning another product immediately
                                    return@addOnSuccessListener
                                }
                            }
                        }
                    }
                    .addOnFailureListener {
                        Log.e(TAG, "Barcode scanning failed: ${it.message}")
                        showToast("Barcode scanning failed")
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            } else {
                imageProxy.close()
            }
        }

        fun pauseScanning() {
            scanningPaused = true
        }

        fun resumeScanning() {
            scanningPaused = false
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == EXPIRATION_SCAN_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                val expirationDate = data?.getStringExtra("expirationDate")
                if (expirationDate != null) {
                    Toast.makeText(this, "Expiration Date: $expirationDate", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "No expiration date found", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Expiration scan cancelled", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val TAG = "BarcodeScanning"
        private const val OPEN_FOOD_FACTS_API_BASE_URL = "https://world.openfoodfacts.org/api/v0/product/"
    }
}