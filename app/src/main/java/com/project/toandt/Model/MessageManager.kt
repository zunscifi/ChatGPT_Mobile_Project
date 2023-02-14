package com.project.toandt.Model

class MessageManager {
  private val messages = mutableListOf<Message>()

  fun addMessage(message: Message) {
    messages.add(message)
    messages.sortBy { it.timestamp }
  }

  fun getMessages(): List<Message> {
    return messages.toList()
  }
}
