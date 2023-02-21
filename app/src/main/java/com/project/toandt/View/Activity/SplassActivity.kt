package com.project.toandt.View.Activity

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import androidx.appcompat.app.AppCompatActivity
import com.toandtpro.chatgpt.ChatGPTApp
import com.toandtpro.chatgpt.R
//import com.xcode.onboarding.OnBoarder
//import com.xcode.onboarding.OnBoardingPage

private const val COUNTER_TIME = 5L
class SplassActivity : AppCompatActivity() {
  private var secondsRemaining: Long = 0L
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
      val application = application as? ChatGPTApp
      if (application == null) {
        startMainActivity()
        return
      }

      // Show the app open ad.
      application.showAdIfAvailable(
        this@SplassActivity,
        object : ChatGPTApp.OnShowAdCompleteListener {
          override fun onShowAdComplete() {
            startMainActivity()
          }
        })
    }

  /** Start the MainActivity. */
  fun startMainActivity() {
    val intent = Intent(this, LoginActivity::class.java)
    startActivity(intent)
  }
}
