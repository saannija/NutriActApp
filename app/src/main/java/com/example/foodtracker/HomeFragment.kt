package com.example.foodtracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment

class HomeFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        val savedRecipesButton: Button = view.findViewById(R.id.savedRecipesButton)

        savedRecipesButton.setOnClickListener {
            val transaction = requireActivity().supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment_container, SavedRecipesFragment())
            transaction.addToBackStack(null)
            transaction.commit()
        }

        return view
    }
}