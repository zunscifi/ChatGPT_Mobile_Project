package com.project.toandt.View.Activity

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.project.toandt.Control.Database.DatabaseHelper
import com.project.toandt.Model.Conversation
import com.project.toandt.Model.Message
import com.skydoves.chatgpt.R
import com.skydoves.chatgpt.databinding.ActivityChatBinding
import com.skydoves.chatgpt.databinding.ActivityLoginBinding
import com.skydoves.chatgpt.feature.chat.messages.ChatGPTMessagesViewModel
import com.skydoves.chatgpt.feature.chat.worker.ChatGPTMessageWorker
import io.getstream.log.streamLog

class ChatActivity : AppCompatActivity() {
  private lateinit var databaseHelper: DatabaseHelper
  private lateinit var binding : ActivityChatBinding
  private lateinit var currentIDConversation : String

    override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      binding = ActivityChatBinding.inflate(layoutInflater)
      val view = binding.root
      setContentView(view)
      addControls()
      addEvents()
    }

  private fun addEvents() {
    binding.imgbtnSendRequest.setOnClickListener(){
      if(binding.edtxtRequestText.text.isNotEmpty()){
        handleEventRequestResponse(binding.edtxtRequestText.text.toString())
      }else{
        //SHOW ERROR
      }
    }
  }

  private fun handleEventRequestResponse(input: String) {
    val resultData = handleRequestResponseChatGPT(input)
    resultData.observe(this, Observer { result ->
      if (result.containsKey("Success")) {
        val data = result["Success"]
        val clientMessID = databaseHelper.addMessage(currentIDConversation.toInt(), DatabaseHelper.SENDER_CLIENT, input, System.currentTimeMillis().toString())
        val severMessID = databaseHelper.addMessage(currentIDConversation.toInt(), DatabaseHelper.SENDER_SEVER, data, System.currentTimeMillis().toString())
        if(clientMessID != -1 && severMessID != -1){
          var clientMess : Message = databaseHelper.getMessage(clientMessID, currentIDConversation.toInt())
          var severMess : Message = databaseHelper.getMessage(severMessID, currentIDConversation.toInt())
        }
      // do something with the data
      } else if (result.containsKey("Failure")) {
        val error = result["Failure"]
        // handle the error
      }
    })
  }

  private fun handleRequestResponseChatGPT(inputStr: String): LiveData<HashMap<String, String>> {
    val resultData = MutableLiveData<HashMap<String, String>>()
    val workRequest = buildGPTMessageWorkerRequest(inputStr)
    WorkManager.getInstance(this).enqueue(workRequest)
    val workInfo = WorkManager.getInstance(this).getWorkInfoByIdLiveData(workRequest.id)
    workInfo.observe(this, Observer { workInfo ->
      if (workInfo != null) {
        if (workInfo.state == WorkInfo.State.SUCCEEDED) {
          val data = workInfo.outputData.getString(ChatGPTMessageWorker.DATA_SUCCESS)
          streamLog { "gpt message worker success: $data" }
          val result = HashMap<String, String>()
          result["Success"] = data.toString()
          resultData.postValue(result)
        } else if (workInfo.state == WorkInfo.State.FAILED) {
          val error = workInfo.outputData.getString(ChatGPTMessageWorker.DATA_FAILURE) ?: ""
          streamLog { "gpt message worker failed: $error" }
          val result = HashMap<String, String>()
          result["Failure"] = error.toString()
          resultData.postValue(result)
        }
      }
    })
    return resultData
  }



    private fun addControls() {
    handleSQLite()
  }

  private fun handleSQLite() {
    handleFirstInit()
    handleDataCurrentConversation()
  }

  private fun handleDataCurrentConversation() {
    currentIDConversation = getStringFromSharedPreferences(this).toString()
  }

  private fun handleFirstInit() {
    databaseHelper = DatabaseHelper(this)
    val conversationID : String = getStringFromSharedPreferences(this).toString()
    if(conversationID == "NULL"){
      val conversationId = databaseHelper.addConversation("Test")
      if(conversationId != -1){
        saveStringToSharedPreferences(this, conversationID.toString())
      }
    }
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

  fun saveStringToSharedPreferences(context: Context, value: String) {
    val sharedPreferences = context.getSharedPreferences("CONVERTION_CHATGPT", Context.MODE_PRIVATE)
    val editor = sharedPreferences.edit()
    editor.putString("CONVERTION_TEXT_ID", value)
    editor.apply()
  }
  fun getStringFromSharedPreferences(context: Context): String? {
    val sharedPreferences = context.getSharedPreferences("CONVERTION_CHATGPT", Context.MODE_PRIVATE)
    return sharedPreferences.getString("CONVERTION_TEXT_ID", "NULL")
  }

}