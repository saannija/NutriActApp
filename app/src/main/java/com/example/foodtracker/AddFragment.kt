package com.example.foodtracker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
        setupDeleteButtonListener()
        setupActivityResultLauncher()

        // Check if we're editing an existing product and pre-fill the form
        arguments?.let {
            documentId = it.getString("documentId")
            if (documentId != null) {
                // We are in edit mode, pre-fill the form
                binding.layoutAddManually.productNameEditText.setText(it.getString("productName"))
                binding.layoutAddManually.categoryAutoCompleteTextView.setText(it.getString("category"))
                binding.layoutAddManually.typeAutoCompleteTextView.setText(it.getString("type"))

                // Set quantity and unit
                binding.layoutAddManually.quantityEditText.setText(it.getInt("quantity").toString())
                val unit = it.getString("unit")
                binding.layoutAddManually.unitAutoCompleteTextView.setText(unit)
                // Check if the unit is in the predefined list and show "Other" if not
                val predefinedUnits = resources.getStringArray(R.array.product_units).toList()
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
                // Make delete button visible only in edit mode
                binding.layoutAddManually.btnDeleteProduct.visibility = View.VISIBLE
                hasScannedData = true // Ensure manual entry form is shown
                isAddingNewMasterProduct = false // Ensure we are not in master product mode
            } else {
                // Check for navigation arguments from HomeFragment
                val shouldStartScanning = it.getBoolean("startScanning", false)
                val shouldShowManualEntry = it.getBoolean("showManualEntry", false)

                if (shouldStartScanning) {
                    // Trigger barcode scanning immediately
                    view.post {
                        val intent = Intent(requireContext(), BarcodeScannerActivity::class.java)
                        barcodeResultLauncher.launch(intent)
                    }
                } else if (shouldShowManualEntry) {
                    // Show manual entry form immediately
                    hasScannedData = true
                    isAddingNewMasterProduct = false
                }

                // Not editing, ensure delete button is hidden and add button text is correct
                binding.layoutAddManually.btnAddProduct.text = "Add Product"
                binding.layoutAddManually.btnDeleteProduct.visibility = View.GONE
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
            showManualEntryForm() // Handle delete button visibility based on isEditing()
            if (isEditing()) {
                binding.layoutAddManually.btnAddProduct.text = "Update Product"
            } else {
                binding.layoutAddManually.btnAddProduct.text = "Add Product"
            }
        } else {
            showAddOptions()
            binding.layoutAddManually.btnAddProduct.text = "Add Product"
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
                    val category = data?.getStringExtra("category") ?: ""
                    val typeFromScan = data?.getStringExtra("type") ?: ""
                    val quantityFromScan = data?.getStringExtra("quantity") ?: "1"
                    val unitFromScan = data?.getStringExtra("unit") ?: ""

                    // Pre-fill form
                    binding.layoutAddManually.productNameEditText.setText(productName)
                    binding.layoutAddManually.categoryAutoCompleteTextView.setText(category)
                    binding.layoutAddManually.typeAutoCompleteTextView.setText(typeFromScan)
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
            binding.layoutAddManually.btnDeleteProduct.visibility = View.GONE
        } else {
            binding.layoutAddManually.root.visibility = View.VISIBLE
            binding.layoutAddMasterProduct.root.visibility = View.GONE
            // Show delete button only if we are in edit mode (documentId is not null)
            if (isEditing()) {
                binding.layoutAddManually.btnDeleteProduct.visibility = View.VISIBLE
            } else {
                binding.layoutAddManually.btnDeleteProduct.visibility = View.GONE
            }
        }
    }

    fun showAddOptions() {
        binding.layoutAddOptions.visibility = View.VISIBLE
        binding.layoutAddManually.root.visibility = View.GONE
        binding.layoutAddMasterProduct.root.visibility = View.GONE
        binding.layoutAddManually.btnDeleteProduct.visibility = View.GONE
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
            }
            .show()
    }

    private fun showMasterProductEntryForm() {
        binding.layoutAddOptions.visibility = View.GONE
        binding.layoutAddManually.root.visibility = View.GONE
        binding.layoutAddMasterProduct.root.visibility = View.VISIBLE
    }

    private fun setupDeleteButtonListener() {
        binding.layoutAddManually.btnDeleteProduct.setOnClickListener {
            showDeleteConfirmationDialog()
        }
    }

    private fun showDeleteConfirmationDialog() {
        // Ensure we are in edit mode and have a documentId
        if (!isEditing() || documentId == null) {
            // Should not happen if button visibility is managed correctly
            Toast.makeText(context, "Cannot delete: No product selected for editing.", Toast.LENGTH_LONG).show()
            return
        }

        val currentDocumentId = documentId!!

        AlertDialog.Builder(requireContext())
            .setTitle("Delete Product")
            .setMessage("Are you sure you want to delete this product? This action cannot be undone.")
            .setPositiveButton("Delete") { dialog, which ->
                dataHandler.deleteProduct(currentDocumentId) { success ->
                    activity?.runOnUiThread { // Ensure execution on the main thread
                        if (!isAdded || view == null) {
                            android.util.Log.w("AddFragment", "Delete callback received but fragment not in a valid state.")
                            return@runOnUiThread
                        }

                        if (success) {
                            Toast.makeText(requireContext(), "Product deleted successfully", Toast.LENGTH_SHORT).show()
                            try {
                                parentFragmentManager.popBackStack()
                            } catch (e: IllegalStateException) {
                                android.util.Log.e("AddFragment", "Error navigating back using parentFragmentManager after delete: ", e)
                            }
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
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
        binding.layoutAddManually.btnDeleteProduct.visibility = View.GONE
    }

    fun clearMasterProductFields() {
        binding.layoutAddMasterProduct.masterProductNameEditText.text?.clear()
        binding.layoutAddMasterProduct.masterProductBrandEditText.text?.clear()
        binding.layoutAddMasterProduct.masterCategoryAutoCompleteTextView.text.clear()
        binding.layoutAddMasterProduct.masterTypeAutoCompleteTextView.text.clear()

        binding.layoutAddMasterProduct.masterQuantityEditText.setText("1")
        binding.layoutAddMasterProduct.masterUnitAutoCompleteTextView.text.clear()
        binding.layoutAddMasterProduct.otherMasterUnitEditText.text?.clear()
        binding.layoutAddMasterProduct.otherMasterUnitInputLayout.visibility = View.GONE

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