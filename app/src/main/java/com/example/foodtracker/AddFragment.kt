package com.example.foodtracker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.foodtracker.databinding.FragmentAddBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class AddFragment : Fragment() {

    private var _binding: FragmentAddBinding? = null
    val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    lateinit var barcodeResultLauncher: ActivityResultLauncher<Intent>
    var hasScannedData: Boolean = false
    var documentId: String? = null
    var scannedBarcode: String? = null
    var masterProductId: String? = null
    var isAddingNewMasterProduct: Boolean = false

    // Delegates for different responsibilities
    private lateinit var uiSetup: AddFragmentUiSetup
    private lateinit var listenersSetup: AddFragmentListeners
    lateinit var dataHandler: AddFragmentDataHandler

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAddBinding.inflate(inflater, container, false)
        val view = binding.root

        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialize delegates
        uiSetup = AddFragmentUiSetup(this, binding)
        listenersSetup = AddFragmentListeners(this, binding)
        dataHandler = AddFragmentDataHandler(this, binding, auth, db)

        // Restore the state
        if (savedInstanceState != null) {
            hasScannedData = savedInstanceState.getBoolean(KEY_HAS_SCANNED_DATA, false)
            documentId = savedInstanceState.getString(KEY_DOCUMENT_ID)
            isAddingNewMasterProduct = savedInstanceState.getBoolean(KEY_IS_ADDING_MASTER_PRODUCT, false)
        }

        uiSetup.setupUI()
        listenersSetup.setupListeners()
        setupActivityResultLauncher()

        // Check if we're editing an existing product and pre-fill the form
        arguments?.let {
            documentId = it.getString("documentId")
            if (documentId != null) {
                // We are in edit mode, pre-fill the form
                binding.layoutAddManually.productNameEditText.setText(it.getString("productName"))
                binding.layoutAddManually.categoryAutoCompleteTextView.setText(it.getString("category")) // Corrected
                binding.layoutAddManually.typeAutoCompleteTextView.setText(it.getString("type")) // Corrected

                // Set quantity and unit
                binding.layoutAddManually.quantityEditText.setText(it.getInt("quantity").toString())
                val unit = it.getString("unit")
                binding.layoutAddManually.unitAutoCompleteTextView.setText(unit) // Corrected
                // Check if the unit is in the predefined list and show "Other" if not
                val predefinedUnits = resources.getStringArray(R.array.product_units).toList() // Assuming you have this array
                if (unit != null && !predefinedUnits.contains(unit)) {
                    binding.layoutAddManually.otherUnitInputLayout.visibility = View.VISIBLE
                    binding.layoutAddManually.otherUnitEditText.setText(unit)
                } else {
                    binding.layoutAddManually.otherUnitInputLayout.visibility = View.GONE
                    binding.layoutAddManually.otherUnitEditText.text?.clear()
                }

                binding.layoutAddManually.totalAmountEditText.setText(it.getInt("totalAmount").toString())
                binding.layoutAddManually.notesEditText.setText(it.getString("notes"))
                binding.layoutAddManually.allergenAlertCheckBox.isChecked = it.getBoolean("allergenAlert")

                val storageStatus = it.getString("storageStatus")
                if (storageStatus == "Opened") {
                    binding.layoutAddManually.openedChip.isChecked = true
                } else {
                    binding.layoutAddManually.unopenedChip.isChecked = true
                }

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

                // Change button text to "Update Product"
                binding.layoutAddManually.btnAddProduct.text = "Update Product"
                hasScannedData = true // Ensure manual entry form is shown
                isAddingNewMasterProduct = false // Ensure we are not in master product mode
            }
        }

        // Show the correct layout based on the initial state (editing or adding)
        if (documentId != null || hasScannedData) {
            showManualEntryForm()
        } else {
            showAddOptions()
        }

        // Initially hide the manual and master product layouts (will be shown by showManualEntryForm or showAddOptions)
        binding.layoutAddManually.root.visibility = View.GONE
        binding.layoutAddMasterProduct.root.visibility = View.GONE


        return view
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save the state
        outState.putBoolean(KEY_HAS_SCANNED_DATA, hasScannedData)
        outState.putString(KEY_DOCUMENT_ID, documentId)
        outState.putBoolean(KEY_IS_ADDING_MASTER_PRODUCT, isAddingNewMasterProduct)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        // Restore state
        if (savedInstanceState != null) {
            documentId = savedInstanceState.getString(KEY_DOCUMENT_ID)
            hasScannedData = savedInstanceState.getBoolean(KEY_HAS_SCANNED_DATA, false)
            isAddingNewMasterProduct = savedInstanceState.getBoolean(KEY_IS_ADDING_MASTER_PRODUCT, false)
        }
        // After restoring state, ensure the correct layout is shown
        if (documentId != null || hasScannedData) {
            showManualEntryForm()
        } else {
            showAddOptions()
        }
    }

    private fun setupActivityResultLauncher() {
        barcodeResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val productNotFound = data?.getBooleanExtra("productNotFound", false) ?:false
                scannedBarcode = data?.getStringExtra("barcode") ?: ""
                masterProductId = data?.getStringExtra("masterProductId") ?: ""

                if (productNotFound) {
                    isAddingNewMasterProduct = true
                    showProductNotFoundDialog()
                } else {
                    val productName = data?.getStringExtra("productName") ?: ""
                    val brand = data?.getStringExtra("brand") ?: ""
                    val category = data?.getStringExtra("category") ?: ""
                    val quantityFromScan = data?.getStringExtra("quantity") ?: "1"
                    val unitFromScan = data?.getStringExtra("unit") ?: ""

                    // Pre-fill form
                    binding.layoutAddManually.productNameEditText.setText(productName)
                    binding.layoutAddManually.categoryAutoCompleteTextView.setText(category)
                    // Set quantity from scan result
                    binding.layoutAddManually.quantityEditText.setText(quantityFromScan)
                    // Set unit from scan result
                    binding.layoutAddManually.unitAutoCompleteTextView.setText(unitFromScan)
                    // Check if the scanned unit is in the predefined list and show "Other" if not
                    val predefinedUnits = resources.getStringArray(R.array.product_units).toList()
                    if (unitFromScan.isNotEmpty() && !predefinedUnits.contains(unitFromScan)) {
                        binding.layoutAddManually.otherUnitInputLayout.visibility = View.VISIBLE
                        binding.layoutAddManually.otherUnitEditText.setText(unitFromScan)
                    } else {
                        binding.layoutAddManually.otherUnitInputLayout.visibility = View.GONE
                        binding.layoutAddManually.otherUnitEditText.text?.clear()
                    }

                    hasScannedData = true
                    isAddingNewMasterProduct = false // Ensure we are not in master product mode
                    showManualEntryForm()
                }
            } else {
                hasScannedData = false
                isAddingNewMasterProduct = false // Reset this if scan is cancelled
                showAddOptions()
            }
        }
    }

    fun showManualEntryForm() {
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

    private fun showProductNotFoundDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Product Not Found")
            .setMessage("Would you like to add this product as a new master product?")
            .setPositiveButton("Yes") { dialog, which ->
                // Show the master product entry form
                showMasterProductEntryForm()
            }
            .setNegativeButton("No") { dialog, which ->
                // If the user says no, show the manual entry form
                isAddingNewMasterProduct = false
                hasScannedData = true // Indicate that we have scanned data (even if not found in master)
                showManualEntryForm()

                //binding.layoutAddManually.barcodeEditText.setText(scannedBarcode)
            }
            .show()
    }

    private fun showMasterProductEntryForm() {
        binding.layoutAddOptions.visibility = View.GONE
        binding.layoutAddManually.root.visibility = View.GONE
        binding.layoutAddMasterProduct.root.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun isEditing(): Boolean {return documentId != null
    }

    fun clearFields() {
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
        binding.layoutAddManually.unopenedChip.isChecked = true
        binding.layoutAddManually.quantityEditText.setText("1")
        binding.layoutAddManually.unitAutoCompleteTextView.text.clear()
        binding.layoutAddManually.otherUnitEditText.text?.clear()
        binding.layoutAddManually.otherUnitInputLayout.visibility = View.GONE
        binding.layoutAddManually.totalAmountEditText.text?.clear()
        binding.layoutAddManually.notesEditText.text?.clear()
        binding.layoutAddManually.allergenAlertCheckBox.isChecked = false
        documentId = null
        scannedBarcode = null
        masterProductId = null
        binding.layoutAddManually.btnAddProduct.text = "Add Product"
    }

    fun clearMasterProductFields() {
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

    companion object {
        private const val KEY_HAS_SCANNED_DATA = "has_scanned_data"
        private const val KEY_DOCUMENT_ID = "document_id"
        private const val KEY_IS_ADDING_MASTER_PRODUCT = "is_adding_master_product"
    }
}