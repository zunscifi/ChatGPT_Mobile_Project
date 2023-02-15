package com.project.toandt.View.Activity

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.flatdialoglibrary.dialog.FlatDialog
import com.google.android.material.card.MaterialCardView
import com.mikepenz.materialdrawer.Drawer
import com.mikepenz.materialdrawer.DrawerBuilder
import com.mikepenz.materialdrawer.model.DividerDrawerItem
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem
import com.mikepenz.materialdrawer.model.ProfileDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem
import com.project.toandt.Control.Adapter.MessageAdapter
import com.project.toandt.Control.Database.DatabaseHelper
import com.project.toandt.Model.Conversation
import com.project.toandt.Model.ConversationManager
import com.project.toandt.Model.Message
import com.project.toandt.Model.MessageManager
import com.project.toandt.View.Dialog.DialogToolConversation
import com.skydoves.chatgpt.R
import com.skydoves.chatgpt.databinding.ActivityChatBinding
import com.skydoves.chatgpt.feature.chat.worker.ChatGPTMessageWorker
import io.getstream.log.streamLog
import net.gotev.speech.GoogleVoiceTypingDisabledException
import net.gotev.speech.Speech
import net.gotev.speech.SpeechDelegate
import net.gotev.speech.SpeechRecognitionNotAvailable
import okhttp3.internal.notifyAll

class ChatActivity : AppCompatActivity() {
  private lateinit var databaseHelper: DatabaseHelper
  private lateinit var binding : ActivityChatBinding
  private lateinit var currentIDConversation : String
  private lateinit var messageManager: MessageManager
  private lateinit var conversationManager: ConversationManager
  private lateinit var drawer : Drawer
  private val REQUEST_REC_AUDIO_PERMISSION = 1
  private val isEnable = MutableLiveData<Boolean>()
  private val STATE_WAITING_RESPONSE_STR = "ChatGPT are typing..."
  private val STATE_WAITING_RECODING_STR = "ChatGPT are hearing..."
    override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      binding = ActivityChatBinding.inflate(layoutInflater)
      val view = binding.root
      setContentView(view)
      addControls()
      addEvents()
      Speech.init(this, packageName);
    }

  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    when (requestCode) {
      REQUEST_REC_AUDIO_PERMISSION -> {
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          // Permission has been granted, call the hello function
          handleSpeechToText()
        } else {
          // Permission has been denied, handle it
        }
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    Speech.getInstance().shutdown();
  }
  private fun addEvents() {
    binding.imgbtnClearMemory.setOnClickListener(){
      try{
        val sharedPrefs = getSharedPreferences("CONVERTION_CHATGPT", Context.MODE_PRIVATE)
        sharedPrefs.edit().remove(currentIDConversation).apply()
        Toast.makeText(this, "Remove ChatGPT Memory Completed!", Toast.LENGTH_SHORT).show()
      }catch (e : Exception){}
    }
    binding.imgbtnMenuConversation.setOnClickListener(){
      drawer.openDrawer()
    }
    binding.imgbtnSpeechToText.setOnClickListener(){
      if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
        == PackageManager.PERMISSION_GRANTED) {
        // Permission has been granted, call the hello function
        handleSpeechToText()
      } else {
        // Permission has not been granted, request it
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_REC_AUDIO_PERMISSION)
      }
    }

    binding.imgbtnSendRequest.setOnClickListener(){
      if(binding.imgbtnSendRequest.isEnabled){
        if(binding.edtxtRequestText.text.isNotEmpty()){
          handleEventRequestResponse(binding.edtxtRequestText.text.toString())
        }else{
          Toast.makeText(this, "Please enter the question", Toast.LENGTH_SHORT).show()
        }
      }else{
        Toast.makeText(this, "Only one message at a time", Toast.LENGTH_SHORT).show()
      }
    }
  }

  private fun handleDisplayChatMessages(){
    for(conver in conversationManager.getConversations()){
      if(conver.id.toString() == currentIDConversation){
        binding.txtConversationName.text = conver.name
      }
    }
    val recyclerView = binding.rcvChatConversation
    recyclerView.layoutManager = LinearLayoutManager(this)
    recyclerView.adapter = MessageAdapter(this, messageManager, binding)
    recyclerView.scrollToPosition(messageManager.getMessages().size - 1)
  }

  private fun handleEventRequestResponse(input: String) {
    binding.edtxtRequestText.setText("")
    binding.imgbtnSendRequest.isEnabled = false
    val resultData = handleRequestResponseChatGPT(input)
    resultData.observe(this, Observer { result ->
      if (result.containsKey("Success")) {
        binding.imgbtnSendRequest.isEnabled = true
        val data = result["Success"]
        val clientMessID = databaseHelper.addMessage(currentIDConversation.toInt(), DatabaseHelper.SENDER_CLIENT, input, System.currentTimeMillis().toString())
        val severMessID = databaseHelper.addMessage(currentIDConversation.toInt(), DatabaseHelper.SENDER_SEVER, data, System.currentTimeMillis().toString())
        if(clientMessID != -1 && severMessID != -1){
          var clientMess : Message = databaseHelper.getMessage(clientMessID, currentIDConversation.toInt())
          var severMess : Message = databaseHelper.getMessage(severMessID, currentIDConversation.toInt())
          messageManager.addMessage(clientMess)
          messageManager.addMessage(severMess)
          handleDisplayChatMessages()
        }
      // do something with the data
      } else if (result.containsKey("Failure")) {
        binding.imgbtnSendRequest.isEnabled = true
        val error = result["Failure"]
        Toast.makeText(this, error.toString(), Toast.LENGTH_LONG).show()
      }
    })
  }

  private fun handleSpeechToText(){
    if(isEnable.value == false){
      try {
        isEnable.value = true
        // you must have android.permission.RECORD_AUDIO granted at this point
        Speech.getInstance().startListening(object : SpeechDelegate {
          override fun onStartOfSpeech() {
            Log.i("speech", "speech recognition is now active")
          }

          override fun onSpeechRmsChanged(value: Float) {
            Log.d("speech", "rms is now: $value")
          }

          override fun onSpeechPartialResults(results: List<String>) {
            val str = StringBuilder()
            for (res in results) {
              str.append(res).append(" ")
            }
            binding.edtxtRequestText.setText(str.toString().trim())
            binding.edtxtRequestText.setSelection(str.toString().trim().length)
            Log.i("speech", "partial result: ${str.toString().trim()}")
          }

          override fun onSpeechResult(result: String) {
            Log.i("speech", "result: $result")
          }
        })
      } catch (exc: SpeechRecognitionNotAvailable) {
        Log.e("speech", "Speech recognition is not available on this device!")
        // You can prompt the user if he wants to install Google App to have
        // speech recognition, and then you can simply call:
        //
        // SpeechUtil.redirectUserToGoogleAppOnPlayStore(this);
        //
        // to redirect the user to the Google App page on Play Store
      } catch (exc: GoogleVoiceTypingDisabledException) {
        Log.e("speech", "Google voice typing must be enabled!")
      }
    }else{
      Speech.getInstance().stopListening()
      isEnable.value = false
    }


  }


  private fun handleRequestResponseChatGPT(inputStr: String): LiveData<HashMap<String, String>> {
    binding.txtStateWhileWaitingResponse.visibility = View.VISIBLE
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



    private fun addControls() {
      handleSQLite()
      initMessageManager()
      initConversationManager()
      handleDrawer()
      handleDisplayChatMessages()
      checkRecordAudio()
      initDefaultValue()
  }

  private fun handleDrawer(){
    val viewGroup = LayoutInflater.from(this).inflate(R.layout.item_top_drawer, null) as ViewGroup
    val mcv_new_chat : MaterialCardView = viewGroup.findViewById(R.id.mcv_new_chat)
    val mcv_delete_all_conversation : MaterialCardView = viewGroup.findViewById(R.id.mcv_delete_all_conversation)
    mcv_new_chat.setOnClickListener() {
      val isConversation = databaseHelper.addConversation("New Conversation")
      if (isConversation != -1) {
        currentIDConversation = isConversation.toString()
        databaseHelper.addMessage(currentIDConversation.toInt(),
          DatabaseHelper.SENDER_SEVER,
          getString(R.string.chatgpt_welcome_message),
        System.currentTimeMillis().toString())
        drawer.closeDrawer()
        initConversationManager()
        handleDrawer()
        initMessageManager()
        handleDisplayChatMessages()
      }
    }
    mcv_delete_all_conversation.setOnClickListener(){
      for(conversation in conversationManager.getConversations()){
        if(conversationManager.getConversations().size > 1){
          val isDeletedConversation = databaseHelper.deleteConversation(conversation.id.toInt())
          if(isDeletedConversation){
            initConversationManager()
            val isDeletedMessages = databaseHelper.deleteAllMessagesWithConversationId(conversation.id.toInt())
          }
        }
      }
      Toast.makeText(this@ChatActivity, "Delete all conversation completed!", Toast.LENGTH_SHORT).show();
      initConversationManager()
      currentIDConversation = conversationManager.getConversations()[0].id.toString()
      drawer.closeDrawer()
      handleDrawer()
      initMessageManager()
      handleDisplayChatMessages()
    }
    drawer = DrawerBuilder()
      .withActivity(this)
      .withCloseOnClick(true)
      .withSliderBackgroundColorRes(R.color.colorPrimary)
      .withTranslucentStatusBar(true)
      .withStickyFooter(viewGroup)
      .withOnDrawerItemClickListener(object : Drawer.OnDrawerItemClickListener {
        override fun onItemClick(view: View?, position: Int, drawerItem: IDrawerItem<*>): Boolean {
          if(drawerItem is ProfileDrawerItem){
            val s = drawerItem.contentDescription.toString()
            currentIDConversation = s
            initMessageManager()
            handleDisplayChatMessages()
            drawer.closeDrawer()
          }
          return true
        }
      })
      .withOnDrawerItemLongClickListener(object : Drawer.OnDrawerItemLongClickListener{
        override fun onItemLongClick(view: View, position: Int, drawerItem: IDrawerItem<*>): Boolean {
          if(drawerItem is ProfileDrawerItem){
            if(!isFinishing){
              val flatDialog = FlatDialog(this@ChatActivity)
              flatDialog.setTitle("Conversation Detail")
                .setBackgroundColor(ContextCompat.getColor(this@ChatActivity, R.color.colorPrimary))
                .setSubtitle("Change anything you want here")
                .setFirstTextFieldHint("This is name of conversation...")
                .setFirstTextField(drawerItem.name.toString())
                .setFirstButtonText("SAVE DETAIL")
                .setFirstButtonColor(ContextCompat.getColor(this@ChatActivity, R.color.colorSecondary))
                .setFirstButtonTextColor(ContextCompat.getColor(this@ChatActivity, R.color.white))
                .setSecondButtonText("DELETE CONVERSATION")
                .setSecondButtonColor(ContextCompat.getColor(this@ChatActivity, R.color.colorSecondary))
                .setSecondButtonTextColor(ContextCompat.getColor(this@ChatActivity, R.color.white))
                .setThirdButtonText("CANCEL")
                .setThirdButtonColor(ContextCompat.getColor(this@ChatActivity, R.color.colorSecondary))
                .setThirdButtonTextColor(ContextCompat.getColor(this@ChatActivity,  R.color.white))
                .withFirstButtonListner {
                  val nameConversation = flatDialog.firstTextField
                  databaseHelper.updateConversation(drawerItem.contentDescription.toString().toInt(), nameConversation)
                  initConversationManager()
                  drawer.closeDrawer()
                  handleDrawer()
                  flatDialog.dismiss()
                }
                .withSecondButtonListner {
                  initConversationManager()
                  if(conversationManager.getConversations().size > 1){
                    val isDeletedConversation = databaseHelper.deleteConversation(drawerItem.contentDescription.toString().toInt())
                    if(isDeletedConversation){
                      val isDeletedMessages = databaseHelper.deleteAllMessagesWithConversationId(drawerItem.contentDescription.toString().toInt())
                      initConversationManager()
                      currentIDConversation = conversationManager.getConversations()[0].id.toString()
                      drawer.closeDrawer()
                      handleDrawer()
                      initMessageManager()
                      handleDisplayChatMessages()
                      flatDialog.dismiss()
                      Toast.makeText(this@ChatActivity, "Delete conversation completed!", Toast.LENGTH_SHORT).show();
                    }
                  }else{
                    Toast.makeText(this@ChatActivity, "There must be at least one Message that exists!", Toast.LENGTH_SHORT).show();
                  }
                }
                .withThirdButtonListner {
                  flatDialog.dismiss()
                }
                .show()
//              val dialog = object : DialogToolConversation(this@ChatActivity) {
//
//                override fun onNameConversationChange(s: String) {
//                  super.onNameConversationChange(s)
//                  databaseHelper.updateConversation(drawerItem.contentDescription.toString().toInt(), s)
//                  initConversationManager()
//                  handleDrawer()
//                }
//
//                override fun onRemoveClicked() {
//                  initConversationManager()
//                  if(conversationManager.getConversations().size > 1){
//                    val isDeletedConversation = databaseHelper.deleteConversation(currentIDConversation.toInt())
//                    if(isDeletedConversation){
//                      val isDeletedMessages = databaseHelper.deleteAllMessagesWithConversationId(currentIDConversation.toInt())
//                      if(isDeletedMessages){
//                        initConversationManager()
//                        currentIDConversation = conversationManager.getConversations()[0].id.toString()
//                        handleDrawer()
//                        initMessageManager()
//                        handleDisplayChatMessages()
//                      }
//                    }
//                  }else{
//                    Toast.makeText(context, "There must be at least one Message that exists!", Toast.LENGTH_SHORT).show();
//                  }
//                }
//              }
//              dialog.setTitle("My Dialog")
//              dialog.show()
            }
          }
          return true
        }
      })
      .build()
    val conversationList = conversationManager.getConversations()
    drawer.addItem(PrimaryDrawerItem()
      .withName("Conversation ChatGPT")
      .withTextColorRes(R.color.white))
    for(conversation in conversationList){
      drawer.addItem(
        ProfileDrawerItem()
          .withName(conversation.name)
          .withTextColorRes(R.color.white)
          .withIcon(R.drawable.openai)
          .withNameShown(true)
          .withEmail("ID Conversation: GPT${conversation.id}")
          .withContentDescription("${conversation.id}")
      )
    }
    drawer.addItem(DividerDrawerItem())
  }

  private fun initConversationManager() {
    conversationManager = ConversationManager()
    val cursor = databaseHelper.allConversations
    while(cursor.moveToNext()){
      val conversation = Conversation(cursor.getLong(0), cursor.getString(1))
      conversationManager.addConversation(conversation)
    }
    cursor.close()
  }

  private fun initDefaultValue() {
    isEnable.value = false
  }

  private fun initMessageManager() {
    messageManager = databaseHelper.getMessageManager(currentIDConversation.toInt())
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
        saveStringToSharedPreferences(this, conversationId.toString())
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
      .putString(ChatGPTMessageWorker.CONVERSATION_ID, currentIDConversation)
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

  private fun checkRecordAudio() {
    isEnable.observe(this, Observer { isLoaded ->
      if (isLoaded) {
        binding.imgbtnSendRequest.isEnabled = false
        binding.txtStateChatgpt.text = STATE_WAITING_RECODING_STR
        binding.txtStateWhileWaitingResponse.visibility = View.VISIBLE
        binding.imgbtnSpeechToText.setImageResource(R.drawable.icon__pause)
      } else {
        binding.imgbtnSendRequest.isEnabled = true
        binding.txtStateChatgpt.text = STATE_WAITING_RESPONSE_STR
        binding.txtStateWhileWaitingResponse.visibility = View.GONE
        binding.imgbtnSpeechToText.setImageResource(R.drawable.icon_voice)
      }
    })
  }

}