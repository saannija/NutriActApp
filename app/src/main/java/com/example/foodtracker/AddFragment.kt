package com.example.foodtracker

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

class AddFragment : Fragment() {

    private lateinit var btnScanBarcode: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_add, container, false)

        // Find the buttons in the layout
        btnScanBarcode = view.findViewById(R.id.btnScanBarcode)
        val btnAddManually = view.findViewById<Button>(R.id.btnAddManually)

        // Set the icon programmatically
        val icon = ContextCompat.getDrawable(requireContext(), R.drawable.icon_barcode)
        btnScanBarcode.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null)
        btnScanBarcode.compoundDrawablePadding = resources.getDimensionPixelSize(R.dimen.drawable_padding)

        // Set click listeners for the buttons
        btnScanBarcode.setOnClickListener {
            // Handle scan barcode button click
            btnScanBarcode.visibility = View.GONE // Hide the button
            val intent = Intent(context, BarcodeScannerActivity::class.java)
            startActivity(intent)
        }

        btnAddManually.setOnClickListener {
            // Handle add manually button click
            Toast.makeText(context, "Add Manually clicked", Toast.LENGTH_SHORT).show()
            // You would start the manual add activity here
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        btnScanBarcode.visibility = View.VISIBLE // Make the button visible again
    }
}