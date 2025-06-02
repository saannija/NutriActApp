package com.example.foodtracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment

class SettingsFragment : Fragment() {

    private lateinit var languageLatvianLayout: LinearLayout
    private lateinit var languageEnglishLayout: LinearLayout
    private lateinit var selectedLatvianCheck: ImageView
    private lateinit var selectedEnglishCheck: ImageView

    private var currentLanguage = "en"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        languageLatvianLayout = view.findViewById(R.id.languageLatvianLayout)
        languageEnglishLayout = view.findViewById(R.id.languageEnglishLayout)
        selectedLatvianCheck = view.findViewById(R.id.selectedLatvianCheck)
        selectedEnglishCheck = view.findViewById(R.id.selectedEnglishCheck)

        updateCheckVisibility()

        languageLatvianLayout.setOnClickListener {
            selectLanguage("lv")
        }

        languageEnglishLayout.setOnClickListener {
            selectLanguage("en")
        }

        return view
    }

    private fun selectLanguage(languageCode: String) {
        currentLanguage = languageCode
        updateCheckVisibility()
        val languageName = if (languageCode == "lv") "Latvian" else "English"
        Toast.makeText(context, "$languageName selected (visual only)", Toast.LENGTH_SHORT).show()

        // TODO: Implement actual language change logic here
        // 1. Saving the selected locale (e.g., in SharedPreferences).
        // 2. Updating the app's configuration with the new locale.
        // 3. Recreating the current activity (and potentially others) for the change to take effect.

        // val locale = Locale(languageCode)
        // Locale.setDefault(locale)
        // val resources = requireActivity().resources
        // val configuration = resources.configuration
        // configuration.setLocale(locale)
        // resources.updateConfiguration(configuration, resources.displayMetrics)
        // requireActivity().recreate()
    }

    private fun updateCheckVisibility() {
        selectedLatvianCheck.visibility = if (currentLanguage == "lv") View.VISIBLE else View.GONE
        selectedEnglishCheck.visibility = if (currentLanguage == "en") View.VISIBLE else View.GONE
    }
}