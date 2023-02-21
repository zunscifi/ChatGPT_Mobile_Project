package com.project.toandt.Control.Adapter

import android.R.attr.label
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.recyclerview.widget.RecyclerView
import com.project.toandt.Control.Database.DatabaseHelper
import com.project.toandt.Model.Message
import com.project.toandt.Model.MessageManager
import com.toandtpro.chatgpt.R
import com.toandtpro.chatgpt.databinding.ActivityChatBinding


class MessageAdapter(private val context: Context, private val messageManager: MessageManager, private val binding: ActivityChatBinding) : RecyclerView.Adapter<MessageAdapter.ViewHolder>() {

  private val messages = messageManager.getMessages()

  inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val tv_response_text : TextView = itemView.findViewById(R.id.tv_response_text)
    private val ll_background_message : LinearLayout = itemView.findViewById(R.id.ll_background_message)
    private val img_sender : ImageView = itemView.findViewById(R.id.img_sender)
    private val imgbtn_copy_message : ImageButton = itemView.findViewById(R.id.imgbtn_copy_message)
    fun bind(message: Message) {
      tv_response_text.text = message.content
      tv_response_text.setTextIsSelectable(true)
      if(message.sender == DatabaseHelper.SENDER_CLIENT){
        img_sender.setImageResource(R.drawable.user)
        ll_background_message.setBackgroundColor(ContextCompat.getColor(context, R.color.colorSecondary))
      }else{
        img_sender.setImageResource(R.drawable.openai)
        ll_background_message.setBackgroundColor(ContextCompat.getColor(context, R.color.colorPrimary))
      }

      imgbtn_copy_message.setOnClickListener(){
        val clipboard = ContextCompat.getSystemService(context, ClipboardManager::class.java)
        val clip = ClipData.newPlainText("label", message.content)
        clipboard!!.setPrimaryClip(clip)
        Toast.makeText(context, "Copy completed", Toast.LENGTH_SHORT).show()
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