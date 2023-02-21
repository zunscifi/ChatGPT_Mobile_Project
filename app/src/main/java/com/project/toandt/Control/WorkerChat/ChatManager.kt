package com.project.toandt.Control.WorkerChat

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import com.project.toandt.Control.Adapter.MessageAdapter
import com.project.toandt.Model.ConversationManager
import com.project.toandt.Model.MessageManager
import com.toandtpro.chatgpt.databinding.ActivityChatBinding

class ChatManager(private val binding: ActivityChatBinding) {

  fun handleDisplayChatMessages(currentIDConversation : String, context : Context, conversationManager: ConversationManager, messageManager: MessageManager) {
    for(conver in conversationManager.getConversations()){
      if(conver.id.toString() == currentIDConversation){
        binding.txtConversationName.text = conver.name
      }
    }
    val recyclerView = binding.rcvChatConversation
    recyclerView.layoutManager = LinearLayoutManager(context)
    recyclerView.adapter = MessageAdapter(context, messageManager, binding)
    recyclerView.scrollToPosition(messageManager.getMessages().size - 1)
  }

  // other conversation-related functions
}