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

// Klase apstrādā datu saglabāšanu un rediģēšanu pievienošanas fragmentā (AddFragment)
class AddFragmentDataHandler(
    private val fragment: AddFragment, // Fragmenta instance
    private val binding: FragmentAddBinding, // Pieeja visiem UI elementiem
    private val auth: FirebaseAuth, // Firebase autentifikācijas instance
    private val db: FirebaseFirestore // Firebase Firestore datubāzes instance
) {

    // Produkta pievienošana Firestore datubāzē
    fun addProductToFirebase(quantity: Int, scannedBarcode: String?, masterProductId: String?) {
        val userId = auth.currentUser?.uid // Lietotāja ID

        // Ja lietotājs nav ielogojies, rāda kļūdas ziņu (nebūtu jānotiek)
        if (userId == null) {
            showToast("User not logged in.")
            return
        }

        // Saņem datus no ievades laukiem
        val product = collectProductData(userId, scannedBarcode, masterProductId) ?: return

        // Saglabā produktu Firestore "products" datubāzē
        db.collection("products")
            .add(product)
            .addOnSuccessListener {
                showToast("Product added successfully.")
                fragment.clearFields() // Notīra formas laukus
                fragment.showAddOptions() // Atgriežas uz izvēles skatu
            }
            .addOnFailureListener {
                showToast("Error adding product")
            }
    }

    // Esošā produkta rediģēšana (update pēc ID)
    fun updateProductInFirebase(documentId: String, quantity: Int) {

        // Atrod un validē datus
        val updates = collectProductUpdateData() ?: return

        // Atjauno dokumentu Firestore ar jauniem datiem pēc dokumenta ID
        db.collection("products")
            .document(documentId)
            .update(updates)
            .addOnSuccessListener {
                showToast("Product updated successfully.")
                fragment.parentFragmentManager.popBackStack() // Atgriežas atpakaļ UI skatā
            }
            .addOnFailureListener {
                showToast("Error updating product.")
            }
    }

    // Master produkta pievienošana
    fun addNewMasterProductAndContinue(scannedBarcode: String?) {
        // Saņem datus no master produkta formas laukiem
        val masterProductName = binding.layoutAddMasterProduct.masterProductNameEditText.text.toString().trim()
        val masterProductBrand = binding.layoutAddMasterProduct.masterProductBrandEditText.text.toString().trim()
        val masterCategory = binding.layoutAddMasterProduct.masterCategoryAutoCompleteTextView.text.toString().trim()
        val masterType = binding.layoutAddMasterProduct.masterTypeAutoCompleteTextView.text.toString().trim()
        val masterQuantityString = binding.layoutAddMasterProduct.masterQuantityEditText.text.toString().trim()
        val masterQuantity = masterQuantityString.toIntOrNull()
        val masterUnit = if (binding.layoutAddMasterProduct.otherMasterUnitInputLayout.visibility == View.VISIBLE) {
            binding.layoutAddMasterProduct.otherMasterUnitEditText.text.toString().trim()
        } else {
            binding.layoutAddMasterProduct.masterUnitAutoCompleteTextView.text.toString().trim()
        }
        val masterProductDescription = binding.layoutAddMasterProduct.masterProductDescriptionEditText.text.toString().trim()
        val masterProductIngredients = binding.layoutAddMasterProduct.masterProductIngredientsEditText.text.toString().split(",").map { it.trim() }
        val masterProductAllergens = binding.layoutAddMasterProduct.masterProductAllergensEditText.text.toString().split(",").map { it.trim() }

        // Validācija
        if (masterProductName.isEmpty() || masterProductBrand.isEmpty() || masterCategory.isEmpty() || masterType.isEmpty() || masterUnit.isEmpty()) {
            showToast("Please fill in all required fields for the master product.")
            return
        }

        // Izveido masterProduct objektu
        val masterProduct = scannedBarcode?.let {
            MasterProduct(
                productName = masterProductName,
                brand = masterProductBrand,
                category = masterCategory,
                type = masterType,
                quantity = masterQuantity,
                unit = masterUnit,
                description = masterProductDescription,
                ingredients = masterProductIngredients,
                allergens = masterProductAllergens,
                barcode = it
            )
        }

        // Saglabā masterProduct Firestore "masterProducts" datubāzē
        if (masterProduct != null) {
            db.collection("masterProducts")
                .add(masterProduct)
                .addOnSuccessListener { documentReference ->
                    showToast("Master Product added successfully.")
                    // Aizpilda laukus produkta pievienošanai no master product datiem
                    prefillProductFormFromMaster(masterProduct)

                    // Tīra laukus, atjauno UI
                    fragment.masterProductId = documentReference.id
                    fragment.scannedBarcode = scannedBarcode
                    fragment.clearMasterProductFields()
                    fragment.isAddingNewMasterProduct = false
                    fragment.hasScannedData = true
                    fragment.showManualEntryForm()
                }
                .addOnFailureListener { e ->
                    showToast("Error adding master product: ${e.message}")
                }
        }
    }

    // Produkta dzēšana (soft, iestata delete = true)
    fun deleteProduct(documentId: String, callback: (Boolean) -> Unit) {
        db.collection("products").document(documentId)
            .update("deleted", true)
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error soft deleting document $documentId", e)
                callback(false)
            }
    }

    // Iegūst ievades datus
    private fun collectProductData(userId: String, scannedBarcode: String?, masterProductId: String?): Product? {
        // Ievāc visus datus no formas (daļa atkarīga no tā vai ir izvēlēta "Other" opcija)
        val productName = binding.layoutAddManually.productNameEditText.text.toString().trim()
        val category = if (binding.layoutAddManually.otherCategoryInputLayout.visibility == View.VISIBLE) {
            binding.layoutAddManually.otherCategoryEditText.text.toString().trim()
        } else {
            binding.layoutAddManually.categoryAutoCompleteTextView.text.toString().trim()
        }
        val type = if (binding.layoutAddManually.otherTypeInputLayout.visibility == View.VISIBLE) {
            binding.layoutAddManually.otherTypeEditText.text.toString().trim()
        } else {
            binding.layoutAddManually.typeAutoCompleteTextView.text.toString().trim()
        }
        val expirationDate = getExpirationDate()
        val storageStatus = getStorageStatus()
        val productQuantity = binding.layoutAddManually.quantityEditText.text.toString().toIntOrNull() ?: 1
        val unit = if (binding.layoutAddManually.otherUnitInputLayout.visibility == View.VISIBLE) {
            binding.layoutAddManually.otherUnitEditText.text.toString().trim()
        } else {
            binding.layoutAddManually.unitAutoCompleteTextView.text.toString().trim()
        }
        val totalAmount = binding.layoutAddManually.totalAmountEditText.text.toString().toIntOrNull() ?: 0
        val notes = binding.layoutAddManually.notesEditText.text.toString().trim()
        val allergenAlert = binding.layoutAddManually.allergenAlertCheckBox.isChecked

        // Validācija - obligātie lauki
        if (productName.isEmpty() || category.isEmpty() || type.isEmpty() || productQuantity <= 0 || unit.isEmpty()) {
            showToast("Please fill in all required fields.")
            return null
        }

        // Atgriež produkta objektu
        return Product(
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
    }

    // Ievades lauku iegūšana rediģēšanai
    private fun collectProductUpdateData(): HashMap<String, Any>? {
        val product = collectProductData("", null, null) ?: return null

        // Izveido atjaunojamo lauku map
        return hashMapOf(
            "productName" to product.productName,
            "category" to product.category,
            "type" to product.type,
            "expirationDate" to product.expirationDate!!,
            "storageStatus" to product.storageStatus,
            "quantity" to product.quantity,
            "unit" to product.unit,
            "totalAmount" to product.totalAmount,
            "notes" to product.notes,
            "allergenAlert" to product.allergenAlert,
            "updatedDate" to Timestamp.now()
        )
    }

    // Aizpilda produktu formas laukus no master product
    private fun prefillProductFormFromMaster(master: MasterProduct) {
        binding.layoutAddManually.productNameEditText.setText(master.productName)
        binding.layoutAddManually.categoryAutoCompleteTextView.setText(master.category, false)
        binding.layoutAddManually.typeAutoCompleteTextView.setText(master.type, false)
        val quantityFromMaster = master.quantity ?: 1
        binding.layoutAddManually.quantityEditText.setText(quantityFromMaster.toString())
        binding.layoutAddManually.unitAutoCompleteTextView.setText(master.unit, false)
        binding.layoutAddManually.otherUnitEditText.text?.clear()
        binding.layoutAddManually.otherUnitInputLayout.visibility = View.GONE
        binding.layoutAddManually.totalAmountEditText.text?.clear()
        binding.layoutAddManually.notesEditText.text?.clear()
        binding.layoutAddManually.allergenAlertCheckBox.isChecked = false
    }

    // Funkcija, kas atgriež datumu no DatePicker kā Firebase Timestamp
    private fun getExpirationDate(): Timestamp {
        val year = binding.layoutAddManually.expirationDatePicker.year
        val month = binding.layoutAddManually.expirationDatePicker.month
        val day = binding.layoutAddManually.expirationDatePicker.dayOfMonth

        val calendar = Calendar.getInstance()
        calendar.set(year, month, day, 0, 0, 0)
        return Timestamp(calendar.time)
    }

    // Atgriež vai produkts ir atvērts vai nē
    private fun getStorageStatus(): String {
        return if (binding.layoutAddManually.unopenedChip.isChecked) "Unopened" else "Opened"
    }

    // Īss Toast paziņojums
    private fun showToast(message: String) {
        Toast.makeText(fragment.context, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val TAG = "AddFragmentDataHandler"
    }
}
