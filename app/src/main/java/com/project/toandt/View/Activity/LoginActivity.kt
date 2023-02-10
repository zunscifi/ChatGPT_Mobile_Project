package com.project.toandt.View.Activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import com.skydoves.chatgpt.core.preferences.Preferences
import com.skydoves.chatgpt.databinding.ActivityLoginBinding
import com.skydoves.chatgpt.feature.login.LOGIN_COMPLETED
import com.skydoves.chatgpt.feature.login.LOGIN_ING
import com.skydoves.chatgpt.feature.login.LoginGPT
import com.skydoves.chatgpt.feature.login.NORMAL
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginActivity : ComponentActivity() {
  private val TAG : String = "LoginActivity"
  //ViewBinding
  private lateinit var binding : ActivityLoginBinding
    override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      binding = ActivityLoginBinding.inflate(layoutInflater)
      val view = binding.root
      setContentView(view)
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
    handleLoginTask()
  }

  private fun handleLoginTask() {
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