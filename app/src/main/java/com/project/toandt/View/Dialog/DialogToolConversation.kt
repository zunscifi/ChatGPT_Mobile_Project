package com.project.toandt.View.Dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.google.android.material.card.MaterialCardView
import com.skydoves.chatgpt.R

open class DialogToolConversation(context: Context) : Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen) {

  private lateinit var edtxt_conversation_name : EditText
  private lateinit var mcv_remove : MaterialCardView

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    requestWindowFeature(Window.FEATURE_NO_TITLE)
    setContentView(R.layout.dialog_tool_conversation)
    window?.setBackgroundDrawableResource(android.R.color.transparent);
    addControls()
    addEvents()
  }

  private fun addEvents() {
    mcv_remove.setOnClickListener {
      onRemoveClicked()
    }

    edtxt_conversation_name.addTextChangedListener(object : TextWatcher {
      override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
        // Do nothing
      }

      override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
         onNameConversationChange(s.toString())
      }

      override fun afterTextChanged(s: Editable) {
        // Move the cursor to the end of the text

      }
    })
  }
  /**
   * Called when edtxt_conversation_name (Name Conversation) change
   * @param s Name Of Conversation
   */
  open fun onNameConversationChange(s: String) {

  }

  /**
   * Called when mcv_remove (Remover Conversation Button) Clicked
   */
  open fun onRemoveClicked() {}

  private fun addControls() {
    edtxt_conversation_name = findViewById(R.id.edtxt_conversation_name)
    mcv_remove = findViewById(R.id.mcv_remove)
  }


}
