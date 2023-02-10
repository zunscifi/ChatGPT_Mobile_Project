package com.project.toandt.View.Activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.skydoves.chatgpt.R
import com.skydoves.chatgpt.databinding.ActivityChatBinding
import com.skydoves.chatgpt.databinding.ActivityLoginBinding
import com.skydoves.chatgpt.feature.chat.messages.ChatGPTMessagesViewModel
import com.skydoves.chatgpt.feature.chat.worker.ChatGPTMessageWorker
import io.getstream.log.streamLog

class ChatActivity : AppCompatActivity() {
  private lateinit var binding : ActivityChatBinding
    override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      binding = ActivityChatBinding.inflate(layoutInflater)
      val view = binding.root
      setContentView(view)
      addControls()
      addEvents()
    }

  private fun addEvents() {
    binding.send.setOnClickListener {
      binding.output.setText("Typing...")
      var inputStr : String = binding.input.text.toString()
      val workRequest = buildGPTMessageWorkerRequest(inputStr)
      WorkManager.getInstance(this).enqueue(workRequest)
      val workInfo = WorkManager.getInstance(this).getWorkInfoByIdLiveData(workRequest.id)
      workInfo.observe(this, Observer { workInfo ->
        if (workInfo != null) {
          if (workInfo.state == WorkInfo.State.SUCCEEDED) {
            val data = workInfo.outputData.getString(ChatGPTMessageWorker.DATA_SUCCESS)
            binding.output.setText(data)
            streamLog { "gpt message worker success: $data" }
          } else if (workInfo.state == WorkInfo.State.FAILED) {
            val error = workInfo.outputData.getString(ChatGPTMessageWorker.DATA_FAILURE) ?: ""
            streamLog { "gpt message worker failed: $error" }
            binding.output.setText("gpt message worker failed: $error")
          }
        }
      })
    }
  }

  private fun addControls() {

  }
  private fun buildGPTMessageWorkerRequest(text: String): OneTimeWorkRequest {
    val constraints = Constraints.Builder()
      .setRequiredNetworkType(NetworkType.CONNECTED)
      .build()

    val data = Data.Builder()
      .putString(ChatGPTMessageWorker.DATA_TEXT, text)
      .putString(ChatGPTMessageWorker.DATA_CHANNEL_ID, "8e68d7b8-b131-4262-8b78-8ebc1d1935d8")
      .build()

    return OneTimeWorkRequest.Builder(ChatGPTMessageWorker::class.java)
      .setConstraints(constraints)
      .setInputData(data)
      .build()
  }

}