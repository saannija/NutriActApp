package com.example.foodtracker

import android.view.View
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import com.example.foodtracker.databinding.FragmentAddBinding
import java.util.Calendar

class AddFragmentUiSetup(
    private val fragment: AddFragment,
    private val binding: FragmentAddBinding
) {

    fun setupUI() {
        // Set the icon for scan barcode button
        val icon = ContextCompat.getDrawable(fragment.requireContext(), R.drawable.icon_barcode)
        binding.btnScanBarcode.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null)
        binding.btnScanBarcode.compoundDrawablePadding =
            fragment.resources.getDimensionPixelSize(R.dimen.drawable_padding)

        // Set up the AutoCompleteTextViews
        setupCategoryAutoCompleteTextView()
        setupTypeAutoCompleteTextView()
        setupUnitAutoCompleteTextView()
        setupMasterCategoryAutoCompleteTextView()
        setupMasterTypeAutoCompleteTextView()

        // Set up the DatePicker
        setupDatePicker()

        // Set up the quantity stepper
        setupQuantityStepper()

        // Set up the total amount stepper
        setupTotalAmountStepper()
    }

    private fun setupCategoryAutoCompleteTextView() {
        val categories = fragment.resources.getStringArray(R.array.product_categories)
        val adapter = ArrayAdapter(
            fragment.requireContext(),
            R.layout.dropdown_item,
            categories
        )
        binding.layoutAddManually.categoryAutoCompleteTextView.setAdapter(adapter)

        // Add a listener to handle the "Other" option
        binding.layoutAddManually.categoryAutoCompleteTextView.setOnItemClickListener { parent, view, position, id ->
            val selectedCategory = parent.getItemAtPosition(position).toString()
            if (selectedCategory == "Other") {
                binding.layoutAddManually.otherCategoryInputLayout.visibility = View.VISIBLE
            } else {
                binding.layoutAddManually.otherCategoryInputLayout.visibility = View.GONE
                binding.layoutAddManually.otherCategoryEditText.text?.clear()
            }
        }
    }

    private fun setupTypeAutoCompleteTextView() {
        val types = fragment.resources.getStringArray(R.array.product_types)
        val adapter = ArrayAdapter(
            fragment.requireContext(),
            R.layout.dropdown_item,
            types
        )
        binding.layoutAddManually.typeAutoCompleteTextView.setAdapter(adapter)

        // Add a listener to handle the "Other" option
        binding.layoutAddManually.typeAutoCompleteTextView.setOnItemClickListener { parent, view, position, id ->
            val selectedType = parent.getItemAtPosition(position).toString()
            if (selectedType == "Other") {
                binding.layoutAddManually.otherTypeInputLayout.visibility = View.VISIBLE
            } else {
                binding.layoutAddManually.otherTypeInputLayout.visibility = View.GONE
                binding.layoutAddManually.otherTypeEditText.text?.clear()
            }
        }
    }

    private fun setupUnitAutoCompleteTextView() {
        val units = fragment.resources.getStringArray(R.array.product_units)
        val adapter = ArrayAdapter(
            fragment.requireContext(),
            R.layout.dropdown_item,
            units
        )
        binding.layoutAddManually.unitAutoCompleteTextView.setAdapter(adapter)

        // Add a listener to handle the "Other" option
        binding.layoutAddManually.unitAutoCompleteTextView.setOnItemClickListener { parent, view, position, id ->
            val selectedUnit = parent.getItemAtPosition(position).toString()
            if (selectedUnit == "Other") {
                binding.layoutAddManually.otherUnitInputLayout.visibility = View.VISIBLE
            } else {
                binding.layoutAddManually.otherUnitInputLayout.visibility = View.GONE
                binding.layoutAddManually.otherUnitEditText.text?.clear()
            }
        }
    }

    private fun setupMasterCategoryAutoCompleteTextView() {
        val categories = fragment.resources.getStringArray(R.array.product_categories)
        val adapter = ArrayAdapter(fragment.requireContext(),
            R.layout.dropdown_item,
            categories
        )
        binding.layoutAddMasterProduct.masterCategoryAutoCompleteTextView.setAdapter(adapter)
    }

    private fun setupMasterTypeAutoCompleteTextView() {
        val types = fragment.resources.getStringArray(R.array.product_types)
        val adapter = ArrayAdapter(
            fragment.requireContext(),
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
        binding.layoutAddManually.btnDecreaseQuantity.setOnClickListener {
            val currentQuantity = binding.layoutAddManually.quantityEditText.text.toString().toIntOrNull() ?: 1
            if (currentQuantity > 1) {
                binding.layoutAddManually.quantityEditText.setText((currentQuantity - 1).toString())
            }
        }

        binding.layoutAddManually.btnIncreaseQuantity.setOnClickListener {
            val currentQuantity = binding.layoutAddManually.quantityEditText.text.toString().toIntOrNull() ?: 1
            binding.layoutAddManually.quantityEditText.setText((currentQuantity + 1).toString())
        }
    }

    // New function to set up the total amount stepper
    private fun setupTotalAmountStepper() {
        binding.layoutAddManually.btnDecreaseTotalAmount.setOnClickListener {
            val currentTotalAmount = binding.layoutAddManually.totalAmountEditText.text.toString().toIntOrNull() ?: 0
            if (currentTotalAmount > 0) {
                binding.layoutAddManually.totalAmountEditText.setText((currentTotalAmount - 1).toString())
            }
        }

        binding.layoutAddManually.btnIncreaseTotalAmount.setOnClickListener {
            val currentTotalAmount = binding.layoutAddManually.totalAmountEditText.text.toString().toIntOrNull() ?: 0
            binding.layoutAddManually.totalAmountEditText.setText((currentTotalAmount + 1).toString())
        }
    }
}