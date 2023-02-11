package com.project.toandt.Model

data class Message(
  val id: Long,
  val conversationId: Long,
  val sender: String,
  val content: String,
  val timestamp: Long
)
