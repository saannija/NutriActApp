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
import com.example.foodtracker.model.MasterProduct
import com.google.firebase.firestore.FirebaseFirestore
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
    private lateinit var barcodeAnalyzer: BarcodeAnalyzer
    private lateinit var db: FirebaseFirestore

    private val CAMERA_PERMISSION_REQUEST_CODE = 100
    private val EXPIRATION_SCAN_REQUEST_CODE = 201

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_barcode_scanning)

        previewView = findViewById(R.id.previewView)
        db = FirebaseFirestore.getInstance()

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
            barcodeAnalyzer = BarcodeAnalyzer { barcode ->
                searchProduct(barcode)
            }
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

    private fun searchProduct(barcode: String) {
        searchProductInApi(barcode) { productFromApi ->if (productFromApi != null) {
            returnProduct(productFromApi)
        } else {
            searchProductInDatabase(barcode) { productFromDatabase ->
                if (productFromDatabase != null) {
                    returnProduct(productFromDatabase)
                } else {
                    returnProductNotFound(barcode)
                }
            }
        }
        }
    }

    private fun searchProductInApi(barcode: String, callback: (MasterProduct?) -> Unit) {
        // Replace with your actual API endpoint and logic
        val url = "https://world.openfoodfacts.org/api/v2/product/$barcode"

        val queue = Volley.newRequestQueue(this)
        val stringRequest = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    val jsonResponse = JSONObject(response)
                    val productJson = jsonResponse.getJSONObject("product")

                    val productName = productJson.getString("product_name")
                    val brand = productJson.getString("brands")
                    val category = productJson.getString("categories")
                    val quantity = productJson.getString("quantity")

                    val product = MasterProduct(
                        productName = productName,
                        brand = brand,
                        category = category,
                        quantity = quantity,
                        barcode = barcode
                    )
                    callback(product)
                } catch (e: JSONException) {
                    Log.e(TAG, "Error parsing JSON", e)
                    callback(null)
                }
            },
            { error ->
                Log.e(TAG, "Error fetching product from API", error)
                callback(null)
            })

        queue.add(stringRequest)
    }

    private fun searchProductInDatabase(barcode: String, callback: (MasterProduct?) -> Unit) {
        db.collection("masterProducts")
            .whereEqualTo("barcode", barcode)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val document = documents.documents[0]
                    val product = document.toObject(MasterProduct::class.java)
                    callback(product)
                } else {
                    callback(null)
                }
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting documents: ", exception)
                callback(null)
            }
    }

    private fun returnProduct(product: MasterProduct) {
        val intent = Intent()
        intent.putExtra("productName", product.productName)
        intent.putExtra("brand", product.brand)
        intent.putExtra("category", product.category)
        intent.putExtra("quantity", product.quantity)
        intent.putExtra("barcode", product.barcode)
        intent.putExtra("masterProductId", product.id)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    private fun returnProductNotFound(barcode: String) {
        val intent = Intent()
        intent.putExtra("productNotFound", true)
        intent.putExtra("barcode", barcode)
        setResult(Activity.RESULT_OK, intent)
        finish()
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
                Toast.makeText(this, "Camera permission is required to use the barcode scanner.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    inner class BarcodeAnalyzer(private val onBarcodeDetected: (String) -> Unit) : ImageAnalysis.Analyzer {
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
                                    onBarcodeDetected(barcodeValue)
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

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val TAG = "BarcodeScanning"
    }
}