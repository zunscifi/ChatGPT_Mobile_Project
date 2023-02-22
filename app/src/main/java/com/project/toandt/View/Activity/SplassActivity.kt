package com.project.toandt.View.Activity

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.toandtpro.chatgpt.ChatGPTApp
import com.toandtpro.chatgpt.R
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.NonCancellable.start
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

//import com.xcode.onboarding.OnBoarder
//import com.xcode.onboarding.OnBoardingPage

private const val COUNTER_TIME = 5L
class SplassActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splass)
      Thread {
        Thread.sleep(4000) // wait for 5 seconds in the background thread
        Handler(Looper.getMainLooper()).post { startMainActivity()} // post a Runnable to the UI thread to call the start method
      }.start()
    }

  /** Start the MainActivity. */
  private fun startMainActivity() {
    val intent = Intent(this, LoginActivity::class.java)
    finish()
    startActivity(intent)
  }
}
