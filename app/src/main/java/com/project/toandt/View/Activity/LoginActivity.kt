package com.project.toandt.View.Activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import com.skydoves.chatgpt.databinding.ActivityLoginBinding
import com.skydoves.chatgpt.feature.login.ChatGPTLogin
import com.skydoves.chatgpt.feature.login.LOGIN_COMPLETED
import com.skydoves.chatgpt.feature.login.LOGIN_ING
import com.skydoves.chatgpt.feature.login.LoginGPT
import com.skydoves.chatgpt.feature.login.NORMAL
import com.skydoves.chatgpt.feature.login.NOT_AUTHORIZED
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

  private fun addEvents() {

  }

  private fun addControls() {
    handleLoginTask()
  }

  private fun handleLoginTask() {
      LoginGPT(binding.wvChatgptLogin) {it ->
        when(it){
          LOGIN_COMPLETED -> {
            Log.i(TAG, "LOGIN_COMPLETED")
            val intent = Intent(this, ChatActivity::class.java)
            startActivity(intent)
            finish()
          }
          LOGIN_ING -> {
            val run = Runnable {
              Log.i(TAG, "LOGIN_ING")
                binding.wvChatgptLogin.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                  return true
                }
              }
              binding.wvChatgptLogin.visibility = View.GONE
            }
            runOnUiThread(run)
          }
          NORMAL -> {
            val run = Runnable {
              Log.i(TAG, "NORMAL")
              binding.wvChatgptLogin.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                  return false
                }
              }
              binding.wvChatgptLogin.visibility = View.VISIBLE
            }
            runOnUiThread(run)
          }
        }
      }
  }
}