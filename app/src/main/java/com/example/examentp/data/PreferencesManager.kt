package com.example.examentp.data

import android.content.Context
import android.graphics.Color
import androidx.preference.PreferenceManager

class PreferencesManager(context: Context) {
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)

    val fontSizeSp: Float
        get() {
            val valStr = prefs.getString("taille_police", "16") ?: "16"
            return valStr.toFloatOrNull() ?: 16f
        }

    val textColorInt: Int
        get() {
            val hex = prefs.getString("couleur_preferee", "#000000") ?: "#000000"
            return try {
                Color.parseColor(hex)
            } catch (e: Exception) {
                Color.BLACK
            }
        }

    val isCallFromListEnabled: Boolean
        get() = prefs.getBoolean("appel_depuis_liste", false)

    val sortOrder: String
        get() = prefs.getString("ordre_tri", "nom") ?: "nom"
}