package com.example.foodtracker

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ExpirationScannerActivity : AppCompatActivity() {

    private lateinit var btnCapture: Button
    private lateinit var btnConfirm: Button
    private lateinit var imageView: ImageView
    private lateinit var previewView: PreviewView
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null
    private var capturedImageFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expiration_scanner)

        btnCapture = findViewById(R.id.btnCapture)
        btnConfirm = findViewById(R.id.btnConfirm)
        imageView = findViewById(R.id.imageView)
        previewView = findViewById(R.id.previewView)

        cameraExecutor = Executors.newSingleThreadExecutor()

        startCamera()

        btnCapture.setOnClickListener {
            takePhoto()
        }

        btnConfirm.setOnClickListener {
            capturedImageFile?.let { file ->
                val image = InputImage.fromFilePath(this, file.toUri())
                processImage(image)
            } ?: run {
                Toast.makeText(this, "No image captured", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (e: Exception) {
                Log.e(TAG, "Use case binding failed", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        }

        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(
                contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            .build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                    runOnUiThread {
                        Toast.makeText(
                            this@ExpirationScannerActivity,
                            "Photo capture failed: ${exc.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val msg = "Photo capture succeeded: ${output.savedUri}"
                    Log.d(TAG, msg)
                    runOnUiThread {
                        Toast.makeText(
                            this@ExpirationScannerActivity,
                            msg,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    capturedImageFile = File(output.savedUri?.path)
                }
            }
        )
    }

    private fun processImage(image: InputImage) {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val expirationDate = extractExpirationDate(visionText)
                if (expirationDate != null) {
                    val resultIntent = Intent()
                    resultIntent.putExtra("expirationDate", expirationDate)
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                } else {
                    Toast.makeText(this, "No expiration date found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("OCR", "Text recognition failed: ${e.message}")
                Toast.makeText(this, "Failed to recognize text", Toast.LENGTH_SHORT).show()
            }
    }

    private fun extractExpirationDate(visionText: Text): String? {
        val regex = Regex("\\b(\\d{2}/\\d{2}/\\d{4}|\\d{2}-\\d{2}-\\d{4})\\b") // Example: 12/05/2025 or 12-05-2025
        for (block in visionText.textBlocks) {
            val text = block.text
            val match = regex.find(text)
            if (match != null) {
                return match.value
            }
        }
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "ExpirationScanner"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }
}