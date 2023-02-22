package com.project.toandt.View.Activity

import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.CookieManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.MutableLiveData
import com.google.firebase.messaging.reporting.MessagingClientEvent.SDKPlatform
import com.project.toandt.Control.Database.DatabaseHelper
import com.toandtpro.chatgpt.ChatGPTApp
import com.toandtpro.chatgpt.databinding.ActivityLoginBinding
import com.toandtpro.chatgpt.feature.login.LOGIN_COMPLETED
import com.toandtpro.chatgpt.feature.login.LOGIN_ING
import com.toandtpro.chatgpt.feature.login.LoginGPT
import com.toandtpro.chatgpt.feature.login.NORMAL
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
      startMethod()
    }

  @Deprecated("Deprecated in Java")
  override fun onBackPressed() {
    if(binding.wvChatgptLogin.canGoBack()){
      binding.wvChatgptLogin.goBack()
    }
  }
  private fun startMethod() {
    val application = application as? ChatGPTApp
    if (application == null) {
      addControls()
      addEvents()
      return
    }

    // Show the app open ad.
    application.showAdIfAvailable(
      this@LoginActivity,
      object : ChatGPTApp.OnShowAdCompleteListener {
        override fun onShowAdComplete() {
          addControls()
          addEvents()
        }
      })
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
    val stateLiveData = MutableLiveData<Int>(NORMAL)
    LoginGPT(binding.wvChatgptLogin, this@LoginActivity, stateLiveData)
    stateLiveData.observe(this) { state ->
      when (state) {
        NORMAL -> {
          // Do something when in NORMAL state
          Log.i(TAG, "NORMAL")
          if (binding.wvChatgptLogin.visibility == View.INVISIBLE) {
            binding.wvChatgptLogin.visibility = View.VISIBLE
          }
        }

        LOGIN_ING -> {
          Log.i(TAG, "LOGIN_ING")
          // Do something when in LOGIN_ING state
          binding.wvChatgptLogin.visibility = View.INVISIBLE
        }

        LOGIN_COMPLETED -> {
          // Do something when in LOGIN_COMPLETED state
          Log.i(TAG, "LOGIN_COMPLETED")
          val intent = Intent(this, ChatActivity::class.java)
          finish()
          startActivity(intent)
        }
      }
    }
  }

}