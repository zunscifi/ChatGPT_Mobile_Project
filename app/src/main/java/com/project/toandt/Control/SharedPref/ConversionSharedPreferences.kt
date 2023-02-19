package com.project.toandt.Control.SharedPref

import android.content.Context

class ConversionSharedPreferences(private val context: Context) {

  private val sharedPreferences = context.getSharedPreferences("CONVERSION_CHATGPT", Context.MODE_PRIVATE)

  fun saveStringToSharedPreferences(value: String) {
    val editor = sharedPreferences.edit()
    editor.putString("CONVERSION_TEXT_ID", value)
    editor.apply()
  }

  fun getStringFromSharedPreferences(): String? {
    return sharedPreferences.getString("CONVERSION_TEXT_ID", "NULL")
  }
}