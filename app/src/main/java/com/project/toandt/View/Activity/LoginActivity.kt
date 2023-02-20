package com.project.toandt.View.Activity

import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.webkit.CookieManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.google.firebase.messaging.reporting.MessagingClientEvent.SDKPlatform
import com.project.toandt.Control.Database.DatabaseHelper
import com.skydoves.chatgpt.databinding.ActivityLoginBinding
import com.skydoves.chatgpt.feature.login.LOGIN_COMPLETED
import com.skydoves.chatgpt.feature.login.LoginGPT
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginActivity : ComponentActivity() {
  private lateinit var databaseHelper: DatabaseHelper
  private val TAG : String = "LoginActivity"
  //ViewBinding
  private lateinit var binding : ActivityLoginBinding
    override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      binding = ActivityLoginBinding.inflate(layoutInflater)
      val view = binding.root
      setContentView(view)
//      if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
//        val pref: SharedPreferences = getSharedPreferences("pref", MODE_PRIVATE)
//        if (!pref.getBoolean("welcomed", false)) {
//          startActivity(Intent(this, SplassActivity::class.java))
//          pref.edit().putBoolean("welcomed", true).apply()
//        }
//      }
      addControls()
      addEvents()
    }

  @Deprecated("Deprecated in Java")
  override fun onBackPressed() {
    if(binding.wvChatgptLogin.canGoBack()){
      binding.wvChatgptLogin.goBack()
    }
  }

  private fun addEvents() {

  }

  private fun addControls() {
    handleSQLite()
    handleLoginTask()
  }

  private fun handleSQLite() {
    databaseHelper = DatabaseHelper(this)
    val db = databaseHelper.writableDatabase
  }

  private fun handleLoginTask() {
    try{
      val intent = intent
      val MODE  = intent.getStringExtra("MODE")
      if(MODE == "SIGN_OUT"){
        binding.wvChatgptLogin.clearFormData()
        binding.wvChatgptLogin.clearHistory()
        binding.wvChatgptLogin.clearCache(true)
        CookieManager.getInstance().removeAllCookies(null)
        Toast.makeText(this@LoginActivity, "SignOut Completed", Toast.LENGTH_SHORT).show()
      }else if (MODE == "NOT_PRESENT"){
        binding.wvChatgptLogin.clearFormData()
        binding.wvChatgptLogin.clearHistory()
        binding.wvChatgptLogin.clearCache(true)
        CookieManager.getInstance().removeAllCookies(null)
        Toast.makeText(this@LoginActivity, "Your session not present anymore, please login again", Toast.LENGTH_SHORT).show()
      }
    }catch (e : Exception){}
      LoginGPT(binding.wvChatgptLogin, this) {it ->
        when(it){
          LOGIN_COMPLETED -> {
            Log.i(TAG, "LOGIN_COMPLETED")
            val intent = Intent(this, ChatActivity::class.java)
            finish()
            startActivity(intent)
          }
        }
      }
  }

}