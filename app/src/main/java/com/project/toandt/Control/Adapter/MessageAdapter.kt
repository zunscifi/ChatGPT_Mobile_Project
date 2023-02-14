package com.project.toandt.Control.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.project.toandt.Control.Database.DatabaseHelper
import com.project.toandt.Model.Message
import com.project.toandt.Model.MessageManager
import com.skydoves.chatgpt.R
import com.skydoves.chatgpt.databinding.ActivityChatBinding

class MessageAdapter(private val context: Context, private val messageManager: MessageManager, private val binding: ActivityChatBinding) : RecyclerView.Adapter<MessageAdapter.ViewHolder>() {

  private val messages = messageManager.getMessages()

  inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val tv_response_text : TextView = itemView.findViewById(R.id.tv_response_text)
    private val ll_background_message : LinearLayout = itemView.findViewById(R.id.ll_background_message)
    fun bind(message: Message) {
      tv_response_text.text = message.content
      if(message.sender == DatabaseHelper.SENDER_CLIENT){
        ll_background_message.setBackgroundColor(ContextCompat.getColor(context, R.color.colorSecondary))
      }else{
        ll_background_message.setBackgroundColor(ContextCompat.getColor(context, R.color.colorPrimary))
      }
    }
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val view = LayoutInflater.from(context).inflate(R.layout.item_rcv_message, parent, false)
    return ViewHolder(view)
  }

  override fun getItemCount() = messages.size

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    holder.bind(messages[position])
  }
}
