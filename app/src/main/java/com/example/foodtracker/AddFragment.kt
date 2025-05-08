package com.example.foodtracker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.foodtracker.databinding.FragmentAddBinding
import com.example.foodtracker.model.MasterProduct
import com.example.foodtracker.model.Product
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class AddFragment : Fragment() {

    private var _binding: FragmentAddBinding? = null
    private val binding get() = _binding!!

    private var quantity: Int = 1
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var barcodeResultLauncher: ActivityResultLauncher<Intent>
    private var hasScannedData: Boolean = false
    private var documentId: String? = null
    private var scannedBarcode: String? = null
    private var masterProductId: String? = null
    private var isAddingNewMasterProduct: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAddBinding.inflate(inflater, container, false)
        val view = binding.root

        // Restore the state of hasScannedData if it exists
        if (savedInstanceState != null) {
            hasScannedData = savedInstanceState.getBoolean(KEY_HAS_SCANNED_DATA, false)
        }

        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Set the icon
        val icon = ContextCompat.getDrawable(requireContext(), R.drawable.icon_barcode)
        binding.btnScanBarcode.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null)
        binding.btnScanBarcode.compoundDrawablePadding =
            resources.getDimensionPixelSize(R.dimen.drawable_padding)

        // Set click listeners for the buttons
        binding.btnScanBarcode.setOnClickListener {
            val intent = Intent(context, BarcodeScannerActivity::class.java)
            barcodeResultLauncher.launch(intent)
        }

        binding.btnAddManually.setOnClickListener {
            // Handle add manually button click
            hasScannedData = true
            showManualEntryForm()
        }

        // Set up the AutoCompleteTextViews
        setupCategoryAutoCompleteTextView()
        setupTypeAutoCompleteTextView()
        setupMasterCategoryAutoCompleteTextView()
        setupMasterTypeAutoCompleteTextView()

        // Set up the DatePicker
        setupDatePicker()

        // Set up the quantity stepper
        setupQuantityStepper()

        // Check if we're editing an existing product
        arguments?.let {
            documentId = it.getString("documentId")
            binding.layoutAddManually.productNameEditText.setText(it.getString("productName"))
            binding.layoutAddManually.categoryAutoCompleteTextView.setText(it.getString("category"), false)
            // Set other fields:
            // typeAutoCompleteTextView.setText(it.getString("type"), false)
            // Set expiration date:
            val expirationDateMillis = it.getLong("expirationDate", 0)
            if (expirationDateMillis > 0) {
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = expirationDateMillis
                binding.layoutAddManually.expirationDatePicker.updateDate(
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                )
            }

            binding.layoutAddManually.btnAddProduct.text = "Update Product"
            hasScannedData = it.getBoolean("hasScannedData", false) // Get hasScannedData from arguments
        }

        // Set up the add/update product button
        binding.layoutAddManually.btnAddProduct.setOnClickListener {
            if (documentId == null) {
                addProductToFirebase()
            } else {
                updateProductInFirebase(documentId!!)
            }
        }
        binding.layoutAddMasterProduct.btnAddMasterProduct.setOnClickListener {
            addNewMasterProductAndContinue()
        }

        // Initialize the ActivityResultLauncher
        barcodeResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val productNotFound = data?.getBooleanExtra("productNotFound", false) ?: false
                scannedBarcode = data?.getStringExtra("barcode") ?: ""
                masterProductId = data?.getStringExtra("masterProductId") ?: ""

                if (productNotFound) {
                    isAddingNewMasterProduct = true
                    showProductNotFoundDialog()
                } else {
                    val productName = data?.getStringExtra("productName") ?: ""
                    val brand = data?.getStringExtra("brand") ?: ""
                    val category = data?.getStringExtra("category") ?: ""
                    val quantity = data?.getStringExtra("quantity") ?: ""

                    // Pre-fill form
                    binding.layoutAddManually.productNameEditText.setText(productName)
                    binding.layoutAddManually.categoryAutoCompleteTextView.setText(category, false)
                    binding.layoutAddManually.quantityTextView.text = quantity

                    hasScannedData = true
                    showManualEntryForm()
                }
            } else {
                hasScannedData = false
                showAddOptions()
            }
        }

        // Show the correct layout based on hasScannedData
        if (hasScannedData) {
            showManualEntryForm()
        } else {
            showAddOptions()
        }

        // Initially hide the manual and master product layouts
        binding.layoutAddManually.root.visibility = View.GONE
        binding.layoutAddMasterProduct.root.visibility = View.GONE

        return view
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save the state of hasScannedData
        outState.putBoolean(KEY_HAS_SCANNED_DATA, hasScannedData)
    }

    private fun showManualEntryForm() {
        binding.layoutAddOptions.visibility = View.GONE
        if (isAddingNewMasterProduct) {
            binding.layoutAddManually.root.visibility = View.GONE
            binding.layoutAddMasterProduct.root.visibility = View.VISIBLE
        } else {
            binding.layoutAddManually.root.visibility = View.VISIBLE
            binding.layoutAddMasterProduct.root.visibility = View.GONE
        }
    }

    fun showAddOptions() {
        binding.layoutAddOptions.visibility = View.VISIBLE
        binding.layoutAddManually.root.visibility = View.GONE
        binding.layoutAddMasterProduct.root.visibility = View.GONE
    }

    private fun setupCategoryAutoCompleteTextView() {
        val categories = resources.getStringArray(R.array.product_categories)
        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.dropdown_item,
            categories
        )
        binding.layoutAddManually.categoryAutoCompleteTextView.setAdapter(adapter)
    }

    private fun setupTypeAutoCompleteTextView() {
        val types = resources.getStringArray(R.array.product_types)
        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.dropdown_item,
            types
        )
        binding.layoutAddManually.typeAutoCompleteTextView.setAdapter(adapter)
    }

    private fun setupMasterCategoryAutoCompleteTextView() {
        val categories = resources.getStringArray(R.array.product_categories)
        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.dropdown_item,
            categories
        )
        binding.layoutAddMasterProduct.masterCategoryAutoCompleteTextView.setAdapter(adapter)
    }

    private fun setupMasterTypeAutoCompleteTextView() {
        val types = resources.getStringArray(R.array.product_types)
        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.dropdown_item,
            types
        )
        binding.layoutAddMasterProduct.masterTypeAutoCompleteTextView.setAdapter(adapter)
    }

    private fun setupDatePicker() {
        val calendar = Calendar.getInstance()
        val today = calendar.timeInMillis
        binding.layoutAddManually.expirationDatePicker.minDate = today
    }

    private fun setupQuantityStepper() {
        binding.layoutAddManually.btnDecreaseQuantity.setOnClickListener {if (quantity > 1) {
            quantity--
            binding.layoutAddManually.quantityTextView.text = quantity.toString()
        }
        }

        binding.layoutAddManually.btnIncreaseQuantity.setOnClickListener {
            quantity++
            binding.layoutAddManually.quantityTextView.text = quantity.toString()
        }
    }

    private fun addProductToFirebase() {
        // Get the current user's ID
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(context, "User not logged in.", Toast.LENGTH_SHORT).show()
            return
        }

        // Get the data from the input fields
        val productName = binding.layoutAddManually.productNameEditText.text.toString().trim()
        val category = binding.layoutAddManually.categoryAutoCompleteTextView.text.toString().trim()
        val type = binding.layoutAddManually.typeAutoCompleteTextView.text.toString().trim()
        val expirationDate = getExpirationDate()
        val storageStatus = getStorageStatus()
        val unit = binding.layoutAddManually.unitEditText.text.toString().trim()
        val totalAmount = binding.layoutAddManually.totalAmountEditText.text.toString().toIntOrNull() ?: 0
        val notes = binding.layoutAddManually.notesEditText.text.toString().trim()
        val allergenAlert = binding.layoutAddManually.allergenAlertCheckBox.isChecked

        // Check if required fields are empty
        if (productName.isEmpty() || category.isEmpty() || type.isEmpty()) {
            Toast.makeText(context, "Please fill in all required fields.", Toast.LENGTH_SHORT).show()
            return
        }

        // Create a new product object
        val product = scannedBarcode?.let {
            Product(
                userId = userId,
                productName = productName,
                category = category,
                type = type,
                expirationDate = expirationDate,
                storageStatus = storageStatus,
                quantity = quantity,
                unit = unit,
                totalAmount = totalAmount,
                notes = notes,
                allergenAlert = allergenAlert,
                masterProductId = masterProductId,
                barcode = it
            )
        }
        // Add the product to Firestore
        if (product != null) {
            db.collection("products")
                .add(product)
                .addOnSuccessListener { documentReference ->
                    Log.d(TAG, "DocumentSnapshot added with ID: ${documentReference.id}")
                    Toast.makeText(context, "Product added successfully.", Toast.LENGTH_SHORT).show()
                    clearFields()
                    showAddOptions()
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Error adding document", e)
                    Toast.makeText(context, "Error adding product.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun updateProductInFirebase(documentId: String) {
        // Get the data from the input fields
        val productName = binding.layoutAddManually.productNameEditText.text.toString().trim()
        val category = binding.layoutAddManually.categoryAutoCompleteTextView.text.toString().trim()
        val type = binding.layoutAddManually.typeAutoCompleteTextView.text.toString().trim()
        val expirationDate = getExpirationDate()
        val storageStatus = getStorageStatus()
        val unit = binding.layoutAddManually.unitEditText.text.toString().trim()
        val totalAmount = binding.layoutAddManually.totalAmountEditText.text.toString().toIntOrNull() ?: 0
        val notes = binding.layoutAddManually.notesEditText.text.toString().trim()
        val allergenAlert = binding.layoutAddManually.allergenAlertCheckBox.isChecked

        // Check if required fields are empty
        if (productName.isEmpty() || category.isEmpty() || type.isEmpty()) {
            Toast.makeText(context, "Please fill in all required fields.", Toast.LENGTH_SHORT).show()
            return
        }

        // Create a map of the fields to update
        val updates = hashMapOf<String, Any>(
            "productName" to productName,
            "category" to category,
            "type" to type,
            "expirationDate" to expirationDate!!,
            "storageStatus" to storageStatus,
            "quantity" to quantity,
            "unit" to unit,
            "totalAmount" to totalAmount,
            "notes" to notes,
            "allergenAlert" to allergenAlert,
            "updatedDate" to Timestamp.now()
        )

        // Update the product in Firestore
        db.collection("products")
            .document(documentId)
            .update(updates)
            .addOnSuccessListener {
                Log.d(TAG, "DocumentSnapshot successfully updated!")
                Toast.makeText(context, "Product updated successfully.", Toast.LENGTH_SHORT).show()
                clearFields()
                showAddOptions()
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error updating document", e)
                Toast.makeText(context, "Error updating product.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun getExpirationDate(): Timestamp? {
        val year = binding.layoutAddManually.expirationDatePicker.year
        val month = binding.layoutAddManually.expirationDatePicker.month
        val day = binding.layoutAddManually.expirationDatePicker.dayOfMonth

        val calendar = Calendar.getInstance()
        calendar.set(year, month, day, 0, 0, 0)
        return Timestamp(calendar.time)
    }

    private fun getStorageStatus(): String {
        return if (binding.layoutAddManually.unopenedRadioButton.isChecked) "Unopened" else "Opened"
    }

    private fun clearFields() {
        binding.layoutAddManually.productNameEditText.text?.clear()
        binding.layoutAddManually.categoryAutoCompleteTextView.text.clear()
        binding.layoutAddManually.typeAutoCompleteTextView.text.clear()
        // Reset expiration date to today
        val calendar = Calendar.getInstance()
        binding.layoutAddManually.expirationDatePicker.updateDate(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        binding.layoutAddManually.unopenedRadioButton.isChecked = true
        quantity = 1
        binding.layoutAddManually.quantityTextView.text = "1"
        binding.layoutAddManually.unitEditText.text?.clear()
        binding.layoutAddManually.totalAmountEditText.text?.clear()
        binding.layoutAddManually.notesEditText.text?.clear()
        binding.layoutAddManually.allergenAlertCheckBox.isChecked = false
    }

    private fun showProductNotFoundDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Product Not Found")
            .setMessage("Would you like to add this product as a new master product?")
            .setPositiveButton("Yes") { dialog, which ->
                // Show the master product entry form
                showMasterProductEntryForm()
            }
            .setNegativeButton("No") { dialog, which ->
                // Go back to the add options
                showAddOptions()
            }
            .show()
    }

    private fun showMasterProductEntryForm() {
        binding.layoutAddOptions.visibility = View.GONE
        binding.layoutAddManually.root.visibility = View.GONE
        binding.layoutAddMasterProduct.root.visibility = View.VISIBLE
    }

    private fun addNewMasterProductAndContinue() {
        // Get the data from the input fields
        val masterProductName = binding.layoutAddMasterProduct.masterProductNameEditText.text.toString().trim()
        val masterProductBrand = binding.layoutAddMasterProduct.masterProductBrandEditText.text.toString().trim()
        val masterCategory = binding.layoutAddMasterProduct.masterCategoryAutoCompleteTextView.text.toString().trim()
        val masterType = binding.layoutAddMasterProduct.masterTypeAutoCompleteTextView.text.toString().trim()
        val masterProductQuantity = binding.layoutAddMasterProduct.masterProductQuantityEditText.text.toString().trim()
        val masterProductImageUrl = binding.layoutAddMasterProduct.masterProductImageUrlEditText.text.toString().trim()
        val masterProductDescription = binding.layoutAddMasterProduct.masterProductDescriptionEditText.text.toString().trim()
        // Convert comma-separated strings to lists
        val masterProductIngredients = binding.layoutAddMasterProduct.masterProductIngredientsEditText.text.toString().split(",").map { it.trim() }
        val masterProductAllergens = binding.layoutAddMasterProduct.masterProductAllergensEditText.text.toString().split(",").map { it.trim() }

        // Check if required fields are empty
        if (masterProductName.isEmpty() || masterProductBrand.isEmpty() || masterCategory.isEmpty() || masterType.isEmpty()) {
            Toast.makeText(context, "Please fill in all required fields forthe master product.", Toast.LENGTH_SHORT).show()
            return
        }

        // Create a new MasterProduct object
        val masterProduct = scannedBarcode?.let {
            MasterProduct(
                productName = masterProductName,
                brand = masterProductBrand,
                category = masterCategory,
                type = masterType,
                quantity = masterProductQuantity,
                imageUrl = masterProductImageUrl,
                description = masterProductDescription,
                ingredients = masterProductIngredients,
                allergens = masterProductAllergens,
                barcode = it
            )
        }

        // Add the master product to Firestore
        if (masterProduct != null) {
            db.collection("masterProducts")
                .add(masterProduct)
                .addOnSuccessListener { documentReference ->
                    Log.d(TAG, "Master Product added with ID: ${documentReference.id}")
                    Toast.makeText(context, "Master Product added successfully.", Toast.LENGTH_SHORT).show()
                    // Set the masterProductId to the newly created document ID
                    masterProductId = documentReference.id
                    // Clear the master product fields
                    clearMasterProductFields()
                    // Add the product to the user's products collection
                    addProductToFirebase()
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Error adding master product", e)
                    Toast.makeText(context, "Error adding master product.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun clearMasterProductFields() {
        binding.layoutAddMasterProduct.masterProductNameEditText.text?.clear()
        binding.layoutAddMasterProduct.masterProductBrandEditText.text?.clear()
        binding.layoutAddMasterProduct.masterCategoryAutoCompleteTextView.text.clear()
        binding.layoutAddMasterProduct.masterTypeAutoCompleteTextView.text.clear()
        binding.layoutAddMasterProduct.masterProductQuantityEditText.text?.clear()
        binding.layoutAddMasterProduct.masterProductImageUrlEditText.text?.clear()
        binding.layoutAddMasterProduct.masterProductDescriptionEditText.text?.clear()
        binding.layoutAddMasterProduct.masterProductIngredientsEditText.text?.clear()
        binding.layoutAddMasterProduct.masterProductAllergensEditText.text?.clear()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun isEditing(): Boolean {
        return documentId != null
    }

    companion object {
        private const val TAG = "AddFragment"
        private const val KEY_HAS_SCANNED_DATA = "hasScannedData"
    }
}