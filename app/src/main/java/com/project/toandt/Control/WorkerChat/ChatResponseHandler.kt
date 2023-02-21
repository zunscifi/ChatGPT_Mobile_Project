package com.project.toandt.Control.WorkerChat

import android.content.Context
import android.view.View
import android.widget.Toast
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.project.toandt.Control.ChatGPTMemoryManager.ChatHistory
import com.project.toandt.Control.Database.DatabaseHelper
import com.project.toandt.Model.ConversationManager
import com.project.toandt.Model.Message
import com.project.toandt.Model.MessageManager
import com.project.toandt.View.Activity.ChatActivity
import com.toandtpro.chatgpt.R
import com.toandtpro.chatgpt.databinding.ActivityChatBinding
import com.toandtpro.chatgpt.feature.chat.worker.ChatGPTMessageWorker
import io.getstream.log.streamLog

class ChatResponseHandler(private val binding: ActivityChatBinding) {

  fun handleEventRequestResponse(input: String, currentIDConversation : String,
                                 context : Context, lifecycleOwner: LifecycleOwner,
                                 databaseHelper : DatabaseHelper, binding: ActivityChatBinding,
                                 messageManager : MessageManager, chatManager: ChatManager, conversationManager: ConversationManager ) {
    val chatHistory = ChatHistory(context, currentIDConversation)
    binding.edtxtRequestText.setText("")
    binding.imgbtnSendRequest.isEnabled = false
    val clientMessID = databaseHelper.addMessage(currentIDConversation.toInt(), DatabaseHelper.SENDER_CLIENT, input, System.currentTimeMillis().toString())
    if(clientMessID != -1){
      val clientMess : Message = databaseHelper.getMessage(clientMessID, currentIDConversation.toInt())
      messageManager.addMessage(clientMess)
      chatManager.handleDisplayChatMessages(currentIDConversation, context, conversationManager, messageManager)
      chatHistory.addMessage("User: $input")
    }
    val resultData = handleRequestResponseChatGPT(input, currentIDConversation, context, lifecycleOwner)
    resultData.observe(lifecycleOwner, Observer { result ->
      if (result.containsKey("Success")) {
        binding.imgbtnSendRequest.isEnabled = true
        val data = result["Success"]
        val severMessID = databaseHelper.addMessage(currentIDConversation.toInt(), DatabaseHelper.SENDER_SEVER, data, System.currentTimeMillis().toString())
        if(severMessID != -1){
          val severMess : Message = databaseHelper.getMessage(severMessID, currentIDConversation.toInt())
          messageManager.addMessage(severMess)
          chatManager.handleDisplayChatMessages(currentIDConversation, context, conversationManager, messageManager )
          chatHistory.addMessage("ChatGPT: $data")
        }else{
          val severMessError = Message( -1 * System.currentTimeMillis(),
            currentIDConversation.toLong(), DatabaseHelper.SENDER_SEVER,
          context.getString(R.string.EROR_MESSAGE), System.currentTimeMillis())
          messageManager.addMessage(severMessError)
          chatManager.handleDisplayChatMessages(currentIDConversation, context, conversationManager, messageManager)
        }
        // do something with the data
      } else if (result.containsKey("Failure")) {
        binding.imgbtnSendRequest.isEnabled = true
        val error = result["Failure"]
        val severMessError = Message( -1 * System.currentTimeMillis(),
          currentIDConversation.toLong(), DatabaseHelper.SENDER_SEVER,
          "Your session was expired. Please Login again to keep using.", System.currentTimeMillis())
        messageManager.addMessage(severMessError)
        if(context is ChatActivity){
          context.handleLogout()
        }
      }
    })
  }

  private fun handleRequestResponseChatGPT(inputStr: String, currentIDConversation : String, context : Context, lifecycleOwner: LifecycleOwner): LiveData<HashMap<String, String>> {
    val chatWorkerBuilder = ChatWorkerBuilder(currentIDConversation)
    binding.txtStateWhileWaitingResponse.visibility = View.VISIBLE
    val resultData = MutableLiveData<HashMap<String, String>>()
    val workRequest = chatWorkerBuilder.buildGPTMessageWorkerRequest(inputStr)
    WorkManager.getInstance(context).enqueue(workRequest)
    val workInfo = WorkManager.getInstance(context).getWorkInfoByIdLiveData(workRequest.id)
    workInfo.observe(lifecycleOwner, Observer { workInfo ->
      if (workInfo != null) {
        if (workInfo.state == WorkInfo.State.SUCCEEDED) {
          val data = workInfo.outputData.getString(ChatGPTMessageWorker.DATA_SUCCESS)
          streamLog { "gpt message worker success: $data" }
          val result = HashMap<String, String>()
          result["Success"] = data.toString()
          resultData.postValue(result)
          binding.txtStateWhileWaitingResponse.visibility = View.GONE
        } else if (workInfo.state == WorkInfo.State.FAILED) {
          val error = workInfo.outputData.getString(ChatGPTMessageWorker.DATA_FAILURE) ?: ""
          streamLog { "gpt message worker failed: $error" }
          val result = HashMap<String, String>()
          result["Failure"] = error.toString()
          resultData.postValue(result)
          binding.txtStateWhileWaitingResponse.visibility = View.GONE
        }
      }
    })
    return resultData
  }
}