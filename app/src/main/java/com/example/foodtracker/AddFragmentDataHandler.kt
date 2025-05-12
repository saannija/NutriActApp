package com.example.foodtracker

import android.util.Log
import android.view.View
import android.widget.Toast
import com.example.foodtracker.databinding.FragmentAddBinding
import com.example.foodtracker.model.MasterProduct
import com.example.foodtracker.model.Product
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class AddFragmentDataHandler(
    private val fragment: AddFragment,
    private val binding: FragmentAddBinding,
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) {

    fun addProductToFirebase(quantity: Int, scannedBarcode: String?, masterProductId: String?) {
        // Get the current user's ID
        val userId = auth.currentUser?.uid
        Log.d(TAG, "Current user ID: $userId")
        if (userId == null) {
            Toast.makeText(fragment.context, "User not logged in.", Toast.LENGTH_SHORT).show()
            return
        }

        // Get the data from the input fields
        val productName = binding.layoutAddManually.productNameEditText.text.toString().trim()
        // Get category from the AutoCompleteTextView, check for "Other"
        val category = if (binding.layoutAddManually.otherCategoryInputLayout.visibility == View.VISIBLE) {
            binding.layoutAddManually.otherCategoryEditText.text.toString().trim()
        } else {
            binding.layoutAddManually.categoryAutoCompleteTextView.text.toString().trim()
        }
        // Get type from the AutoCompleteTextView, check for "Other"
        val type = if (binding.layoutAddManually.otherTypeInputLayout.visibility == View.VISIBLE) {
            binding.layoutAddManually.otherTypeEditText.text.toString().trim()
        } else {
            binding.layoutAddManually.typeAutoCompleteTextView.text.toString().trim()
        }
        val expirationDate = getExpirationDate()
        val storageStatus = getStorageStatus()
        // Get quantity from the EditText
        val productQuantity = binding.layoutAddManually.quantityEditText.text.toString().toIntOrNull() ?: 1
        // Get unit from the AutoCompleteTextView, check for "Other"
        val unit = if (binding.layoutAddManually.otherUnitInputLayout.visibility == View.VISIBLE) {
            binding.layoutAddManually.otherUnitEditText.text.toString().trim()
        } else {
            binding.layoutAddManually.unitAutoCompleteTextView.text.toString().trim()
        }
        val totalAmount = binding.layoutAddManually.totalAmountEditText.text.toString().toIntOrNull() ?: 0
        val notes = binding.layoutAddManually.notesEditText.text.toString().trim()
        val allergenAlert = binding.layoutAddManually.allergenAlertCheckBox.isChecked

        // Check if required fields are empty
        if (productName.isEmpty() || category.isEmpty() || type.isEmpty() || productQuantity <= 0 || unit.isEmpty()) {
            Toast.makeText(fragment.context, "Please fill in all required fields.", Toast.LENGTH_SHORT).show()
            return
        }

        // Create a new product object regardless of scannedBarcode
        val product = Product(
            userId = userId,
            productName = productName,
            category = category,
            type = type,
            expirationDate = expirationDate,
            storageStatus = storageStatus,
            quantity = productQuantity,
            unit = unit,
            totalAmount = totalAmount,
            notes = notes,
            allergenAlert = allergenAlert,
            masterProductId = masterProductId,
            barcode = scannedBarcode
        )

        Log.d(TAG, "Attempting to add product: $product")
        // Add the product to Firestore
        db.collection("products")
            .add(product)
            .addOnSuccessListener {
                Toast.makeText(fragment.context, "Product added successfully.", Toast.LENGTH_SHORT).show()
                fragment.clearFields()
                fragment.showAddOptions()
            }
            .addOnFailureListener {
                Toast.makeText(fragment.context, "Error adding product", Toast.LENGTH_LONG).show()
            }
    }

    fun updateProductInFirebase(documentId: String, quantity: Int) {
        // Get the data from the input fields
        val productName = binding.layoutAddManually.productNameEditText.text.toString().trim()
        // Get category from the AutoCompleteTextView, check for "Other"
        val category = if (binding.layoutAddManually.otherCategoryInputLayout.visibility == View.VISIBLE) {
            binding.layoutAddManually.otherCategoryEditText.text.toString().trim()
        } else {
            binding.layoutAddManually.categoryAutoCompleteTextView.text.toString().trim()
        }
        // Get type from the AutoCompleteTextView, check for "Other"
        val type = if (binding.layoutAddManually.otherTypeInputLayout.visibility == View.VISIBLE) {
            binding.layoutAddManually.otherTypeEditText.text.toString().trim()
        } else {
            binding.layoutAddManually.typeAutoCompleteTextView.text.toString().trim()
        }
        val expirationDate = getExpirationDate()
        val storageStatus = getStorageStatus()
        // Get quantity from the EditText
        val productQuantity = binding.layoutAddManually.quantityEditText.text.toString().toIntOrNull() ?: 1
        // Get unit from the AutoCompleteTextView, check for "Other"
        val unit = if (binding.layoutAddManually.otherUnitInputLayout.visibility == View.VISIBLE) {
            binding.layoutAddManually.otherUnitEditText.text.toString().trim()
        } else {
            binding.layoutAddManually.unitAutoCompleteTextView.text.toString().trim()
        }
        val totalAmount = binding.layoutAddManually.totalAmountEditText.text.toString().toIntOrNull() ?: 0
        val notes = binding.layoutAddManually.notesEditText.text.toString().trim()
        val allergenAlert = binding.layoutAddManually.allergenAlertCheckBox.isChecked

        // Check if required fields are empty
        if (productName.isEmpty() || category.isEmpty() || type.isEmpty() || productQuantity <= 0 || unit.isEmpty()) {
            Toast.makeText(fragment.context, "Please fill in all required fields.", Toast.LENGTH_SHORT).show()
            return
        }

        // Create a map of the fields to update
        val updates = hashMapOf<String, Any>(
            "productName" to productName,
            "category" to category,
            "type" to type,
            "expirationDate" to expirationDate!!,
            "storageStatus" to storageStatus,
            "quantity" to productQuantity,
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
                Toast.makeText(fragment.context, "Product updated successfully.", Toast.LENGTH_SHORT).show()
                // Navigate back to the inventory list
                fragment.parentFragmentManager.popBackStack()
            }
            .addOnFailureListener {
                Toast.makeText(fragment.context, "Error updating product.", Toast.LENGTH_SHORT).show()
            }
    }

    fun addNewMasterProductAndContinue(scannedBarcode: String?) {
        // Get the data from the input fields for the master product
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

        // Check if required fields are empty for the master product
        if (masterProductName.isEmpty() || masterProductBrand.isEmpty() || masterCategory.isEmpty() || masterType.isEmpty()) {
            Toast.makeText(fragment.context, "Please fill in all required fields for the master product.", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(fragment.context, "Master Product added successfully.", Toast.LENGTH_SHORT).show()

                    // After adding the master product, pre-fill the manual entry form with data from the newly created master product and show it
                    binding.layoutAddManually.productNameEditText.setText(masterProductName)
                    binding.layoutAddManually.categoryAutoCompleteTextView.setText(masterCategory, false)
                    binding.layoutAddManually.typeAutoCompleteTextView.setText(masterType, false)
                    binding.layoutAddManually.unitAutoCompleteTextView.text?.clear()
                    binding.layoutAddManually.otherUnitEditText.text?.clear()
                    binding.layoutAddManually.otherUnitInputLayout.visibility = View.GONE
                    binding.layoutAddManually.totalAmountEditText.text?.clear()
                    binding.layoutAddManually.notesEditText.text?.clear()
                    binding.layoutAddManually.allergenAlertCheckBox.isChecked = false

                    // Set quantity from master product if available, otherwise default to 1
                    val quantityFromMaster = masterProductQuantity.toIntOrNull() ?: 1
                    binding.layoutAddManually.quantityEditText.setText(quantityFromMaster.toString())

                    // Set the masterProductId in the fragment
                    fragment.masterProductId = documentReference.id
                    // Set scannedBarcode in the fragment
                    fragment.scannedBarcode = scannedBarcode

                    // Clear the master product fields
                    fragment.clearMasterProductFields()

                    // Show the manual entry form
                    fragment.isAddingNewMasterProduct = false
                    fragment.hasScannedData = true
                    fragment.showManualEntryForm()

                }
                .addOnFailureListener {
                    Toast.makeText(fragment.context, "Error adding master product.", Toast.LENGTH_SHORT).show()
                }
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
        return if (binding.layoutAddManually.unopenedChip.isChecked) "Unopened" else "Opened"
    }

    companion object {
        private const val TAG = "AddFragmentDataHandler"
    }
}