package com.project.toandt.Control.ChatGPTMemoryManager

import android.content.Context
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentLinkedDeque

class ChatHistory {

  private val chatHistory: ConcurrentLinkedDeque<String> = ConcurrentLinkedDeque()

  private val timestampFormatter: DateTimeFormatter = DateTimeFormatter.ISO_INSTANT
  private val newestMarker: String = "*"
  private lateinit var  conversation_id: String
  private lateinit var context: Context
  constructor(context: Context, conversation_id: String){
    this.conversation_id = conversation_id
    this.context = context
    setChatHistory(getStringFromSharedPreferences(context, conversation_id).toString())
  }
  fun saveStringToSharedPreferences(context: Context, value: String, conversation_id: String) {
    val sharedPreferences = context.getSharedPreferences("CONVERTION_CHATGPT", Context.MODE_PRIVATE)
    val s : String = sharedPreferences.getString(conversation_id, "NULL").toString()
    val editor = sharedPreferences.edit()
    editor.putString(conversation_id, value)
    editor.apply()
  }
  fun getStringFromSharedPreferences(context: Context, conversation_id: String): String? {
    val sharedPreferences = context.getSharedPreferences("CONVERTION_CHATGPT", Context.MODE_PRIVATE)
    return sharedPreferences.getString(conversation_id, "NULL")
  }
  fun addMessage(message: String) {
    val timestamp = timestampFormatter.format(Instant.now())
    val formattedMessage = "$timestamp|$message"
    chatHistory.add(formattedMessage)
    checkSizeMemory()
    saveStringToSharedPreferences(context, getChatHistory(), conversation_id)
  }

  fun getChatHistory(): String {
    val stringBuilder = StringBuilder()

    // Add each message to the string builder
    for (message in chatHistory) {
      // Add a marker to the newest message
      val marker = if (message == chatHistory.last) newestMarker else ""
      stringBuilder.append("$marker$message\n")
    }

    return stringBuilder.toString()
  }

  fun setChatHistory(chatHistoryString: String) {
    // Clear the existing chat history
    chatHistory.clear()

    // Parse each line of the chat history string and add it to the chat history
    val lines = chatHistoryString.split("\n")
    for (line in lines) {
      if (line.isNotBlank()) {
        chatHistory.add(line)
      }
    }
  }
  private fun checkSizeMemory(){
    var memoryStr = getChatHistory()
    while (memoryStr.length > 3000){
      chatHistory.removeLast()
      memoryStr = getChatHistory()
    }
  }

}
