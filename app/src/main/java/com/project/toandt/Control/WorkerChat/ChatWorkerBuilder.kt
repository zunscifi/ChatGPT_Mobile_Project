package com.project.toandt.Control.WorkerChat

import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import com.skydoves.chatgpt.feature.chat.worker.ChatGPTMessageWorker

class ChatWorkerBuilder(private val currentIDConversation: String) {

  fun buildGPTMessageWorkerRequest(text: String): OneTimeWorkRequest {
    val constraints = Constraints.Builder()
      .setRequiredNetworkType(NetworkType.CONNECTED)
      .build()

    val data = Data.Builder()
      .putString(ChatGPTMessageWorker.DATA_TEXT, text)
      .putString(ChatGPTMessageWorker.DATA_CHANNEL_ID, "8e68d7b8-b131-4262-8b78-8ebc1d1935d8")
      .putString(ChatGPTMessageWorker.CONVERSATION_ID, currentIDConversation)
      .build()

    return OneTimeWorkRequest.Builder(ChatGPTMessageWorker::class.java)
      .setConstraints(constraints)
      .setInputData(data)
      .build()
  }
}