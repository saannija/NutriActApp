package com.example.foodtracker

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.CheckBox
import android.widget.DatePicker
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.compose.ui.semantics.text
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.foodtracker.model.Product
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class AddFragment : Fragment() {

    private lateinit var layoutAddOptions: LinearLayout
    private lateinit var layoutAddManually: View
    private lateinit var btnScanBarcode: Button
    private lateinit var btnAddManually: Button
    private lateinit var btnBack: Button
    private lateinit var categoryAutoCompleteTextView: AutoCompleteTextView
    private lateinit var typeAutoCompleteTextView: AutoCompleteTextView
    private lateinit var expirationDatePicker: DatePicker
    private lateinit var quantityTextView: TextView
    private lateinit var btnDecreaseQuantity: Button
    private lateinit var btnIncreaseQuantity: Button
    private lateinit var btnAddProduct: Button
    private lateinit var productNameEditText: TextInputEditText
    private lateinit var unitEditText: TextInputEditText
    private lateinit var totalAmountEditText: TextInputEditText
    private lateinit var notesEditText: TextInputEditText
    private lateinit var storageStatusRadioGroup: RadioGroup
    private lateinit var unopenedRadioButton: RadioButton
    private lateinit var openedRadioButton: RadioButton
    private lateinit var allergenAlertCheckBox: CheckBox
    private var quantity: Int = 1
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_add, container, false)

        // Find the layouts
        layoutAddOptions = view.findViewById(R.id.layoutAddOptions)
        layoutAddManually = view.findViewById(R.id.layoutAddManually)

        // Find the buttons in the layout
        btnScanBarcode = view.findViewById(R.id.btnScanBarcode)
        btnAddManually = view.findViewById(R.id.btnAddManually)
        btnBack = layoutAddManually.findViewById(R.id.btnBack)
        categoryAutoCompleteTextView = layoutAddManually.findViewById(R.id.categoryAutoCompleteTextView)
        typeAutoCompleteTextView = layoutAddManually.findViewById(R.id.typeAutoCompleteTextView)
        expirationDatePicker = layoutAddManually.findViewById(R.id.expirationDatePicker)
        quantityTextView = layoutAddManually.findViewById(R.id.quantityTextView)
        btnDecreaseQuantity = layoutAddManually.findViewById(R.id.btnDecreaseQuantity)
        btnIncreaseQuantity = layoutAddManually.findViewById(R.id.btnIncreaseQuantity)
        btnAddProduct = layoutAddManually.findViewById(R.id.btnAddProduct)
        productNameEditText = layoutAddManually.findViewById(R.id.productNameEditText)
        unitEditText = layoutAddManually.findViewById(R.id.unitEditText)
        totalAmountEditText = layoutAddManually.findViewById(R.id.totalAmountEditText)
        notesEditText = layoutAddManually.findViewById(R.id.notesEditText)
        storageStatusRadioGroup = layoutAddManually.findViewById(R.id.storageStatusRadioGroup)
        unopenedRadioButton = layoutAddManually.findViewById(R.id.unopenedRadioButton)
        openedRadioButton = layoutAddManually.findViewById(R.id.openedRadioButton)
        allergenAlertCheckBox = layoutAddManually.findViewById(R.id.allergenAlertCheckBox)

        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Set the icon programmatically
        val icon = ContextCompat.getDrawable(requireContext(), R.drawable.icon_barcode)
        btnScanBarcode.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null)
        btnScanBarcode.compoundDrawablePadding =
            resources.getDimensionPixelSize(R.dimen.drawable_padding)

        // Set click listeners for the buttons
        btnScanBarcode.setOnClickListener {
            // Handle scan barcode button click
            val intent = Intent(context, BarcodeScannerActivity::class.java)
            startActivity(intent)
        }

        btnAddManually.setOnClickListener {
            // Handle add manually button click
            showManualEntryForm()
        }

        btnBack.setOnClickListener {
            // Handle back button click
            showAddOptions()
        }

        // Set up the AutoCompleteTextViews
        setupCategoryAutoCompleteTextView()
        setupTypeAutoCompleteTextView()

        // Set up the DatePicker
        setupDatePicker()

        // Set up the quantity stepper
        setupQuantityStepper()

        // Set up the add product button
        btnAddProduct.setOnClickListener {
            addProductToFirebase()
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        showAddOptions()
    }

    private fun showManualEntryForm() {
        layoutAddOptions.visibility = View.GONE
        layoutAddManually.visibility = View.VISIBLE
    }

    private fun showAddOptions() {
        layoutAddOptions.visibility = View.VISIBLE
        layoutAddManually.visibility = View.GONE
    }

    private fun setupCategoryAutoCompleteTextView() {
        val categories = resources.getStringArray(R.array.product_categories)
        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.dropdown_item,
            categories
        )
        categoryAutoCompleteTextView.setAdapter(adapter)
    }

    private fun setupTypeAutoCompleteTextView() {
        val types = resources.getStringArray(R.array.product_types)
        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.dropdown_item,
            types
        )
        typeAutoCompleteTextView.setAdapter(adapter)
    }

    private fun setupDatePicker() {
        val calendar = Calendar.getInstance()
        val today = calendar.timeInMillis
        expirationDatePicker.minDate = today
    }

    private fun setupQuantityStepper() {
        btnDecreaseQuantity.setOnClickListener {
            if (quantity > 1) {
                quantity--
                quantityTextView.text = quantity.toString()
            }
        }

        btnIncreaseQuantity.setOnClickListener {
            quantity++
            quantityTextView.text = quantity.toString()
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
        val productName = productNameEditText.text.toString()
        val category = categoryAutoCompleteTextView.text.toString()
        val type = typeAutoCompleteTextView.text.toString()
        val expirationDate = getExpirationDate()
        val storageStatus = getStorageStatus()
        val unit = unitEditText.text.toString()
        val totalAmount = totalAmountEditText.text.toString().toIntOrNull() ?: 0
        val notes = notesEditText.text.toString()
        val allergenAlert = allergenAlertCheckBox.isChecked

        // Create a Product object
        val product = Product(
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
            allergenAlert = allergenAlert
        )

        // Add the product to Firestore
        db.collection("products")
            .add(product)
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "DocumentSnapshot added with ID: ${documentReference.id}")
                Toast.makeText(context, "Product added successfully.", Toast.LENGTH_SHORT).show()
                clearFields()
                showAddOptions()
            }
            .addOnFailureListener { e ->Log.w(TAG, "Error adding document", e)
                Toast.makeText(context, "Error adding product.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun getExpirationDate(): Timestamp {
        val day = expirationDatePicker.dayOfMonth
        val month = expirationDatePicker.month
        val year = expirationDatePicker.year

        val calendar = Calendar.getInstance()
        calendar.set(year, month, day)
        val date = calendar.time
        return Timestamp(date)
    }

    private fun getStorageStatus(): String {
        return when (storageStatusRadioGroup.checkedRadioButtonId) {
            R.id.unopenedRadioButton -> "Unopened"
            R.id.openedRadioButton -> "Opened"
            else -> "Unopened"
        }
    }

    private fun clearFields() {
        productNameEditText.text?.clear()
        categoryAutoCompleteTextView.text?.clear()
        typeAutoCompleteTextView.text?.clear()
        expirationDatePicker.updateDate(
            Calendar.getInstance().get(Calendar.YEAR),
            Calendar.getInstance().get(Calendar.MONTH),
            Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        )
        unopenedRadioButton.isChecked = true
        quantityTextView.text = "1"
        unitEditText.text?.clear()
        totalAmountEditText.text?.clear()
        notesEditText.text?.clear()
        allergenAlertCheckBox.isChecked = false
    }

    companion object {
        private const val TAG = "AddFragment"
    }
}