package com.project.toandt.Model
class ConversationManager {
  private val conversations = mutableListOf<Conversation>()

  fun addConversation(conversation: Conversation) {
    conversations.add(conversation)
    conversations.sortBy { it.id }
  }

  fun getConversations(): List<Conversation> {
    return conversations.toList()
  }
}