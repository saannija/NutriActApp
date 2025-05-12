package com.example.foodtracker

import android.content.Intent
import com.example.foodtracker.databinding.FragmentAddBinding

class AddFragmentListeners(
    private val fragment: AddFragment,
    private val binding: FragmentAddBinding
) {

    fun setupListeners() {
        binding.btnScanBarcode.setOnClickListener {
            val intent = Intent(fragment.context, BarcodeScannerActivity::class.java)
            fragment.barcodeResultLauncher.launch(intent) // Access launcher from fragment
        }

        binding.btnAddManually.setOnClickListener {
            fragment.hasScannedData = true
            fragment.isAddingNewMasterProduct = false
            fragment.showManualEntryForm()
        }

        binding.layoutAddManually.btnAddProduct.setOnClickListener {

            // Get the quantity from the EditText
            val quantity = binding.layoutAddManually.quantityEditText.text.toString().toIntOrNull() ?: 1

            if (fragment.documentId == null) {
                fragment.dataHandler.addProductToFirebase(quantity, fragment.scannedBarcode, fragment.masterProductId)
            } else {
                fragment.dataHandler.updateProductInFirebase(fragment.documentId!!, quantity)
            }
        }

        binding.layoutAddMasterProduct.btnAddMasterProduct.setOnClickListener {
            fragment.dataHandler.addNewMasterProductAndContinue(fragment.scannedBarcode)
        }
    }
}