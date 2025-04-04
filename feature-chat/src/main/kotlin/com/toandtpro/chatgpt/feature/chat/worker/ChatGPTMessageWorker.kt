/*
 * Designed and developed by 2022 skydoves (Jaewoong Eum)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.toandtpro.chatgpt.feature.chat.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.toandtpro.chatgpt.core.data.repository.GPTMessageRepository
import com.toandtpro.chatgpt.core.model.GPTChatRequest
import com.toandtpro.chatgpt.core.model.GPTContent
import com.toandtpro.chatgpt.core.model.GPTMessage
import com.toandtpro.chatgpt.feature.chat.di.ChatEntryPoint
import com.skydoves.sandwich.getOrThrow
import com.skydoves.sandwich.isSuccess
import com.skydoves.sandwich.messageOrNull
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.client.models.Message
import io.getstream.log.streamLog
import java.util.UUID
import javax.inject.Inject

@HiltWorker
class ChatGPTMessageWorker @AssistedInject constructor(
  @Assisted private val context: Context,
  @Assisted private val workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

  @set:Inject
  internal lateinit var repository: GPTMessageRepository

  @set:Inject
  internal lateinit var chatClient: ChatClient

  override suspend fun doWork(): Result {
    ChatEntryPoint.resolve(context).inject(this)
    val text = workerParams.inputData.getString(DATA_TEXT) ?: return Result.failure()
    val channelId = workerParams.inputData.getString(DATA_CHANNEL_ID) ?: return Result.failure()
    val conversation_id = workerParams.inputData.getString(CONVERSATION_ID) ?: return Result.failure()
    val previous_text = getStringFromSharedPreferences(context, conversation_id)
    val idMess = UUID.randomUUID().toString()
    val messageId : String
    val sTemp : String
    val requestText : String
    if(previous_text != "NULL") {
     sTemp = "[Contents of the previous conversation : $previous_text]";
    }else{
      sTemp = "NULL"
    }
    messageId = UUID.randomUUID().toString()
    if(sTemp != "NULL"){
      requestText = "[My Question: $text], $sTemp"
    }else{
      requestText = "[My Question: $text]"
    }
    val request = GPTChatRequest(
      messages = listOf(
        GPTMessage(
          id = idMess,
          content = GPTContent(parts = listOf(requestText))
        )
      ),

      parent_message_id = messageId
    )
    val response = repository.sendMessage(request)
    return if (response.isSuccess) {
      streamLog { "worker success!" }
      Result.success(Data.Builder().putString(DATA_SUCCESS, response.getOrThrow()).build())
    } else {
      streamLog { "worker failure!" }
      Result.failure(Data.Builder().putString(DATA_FAILURE, response.messageOrNull ?: "").build())
    }
  }
  fun getStringFromSharedPreferences(context: Context, conversation_id: String): String? {
    val sharedPreferences = context.getSharedPreferences("CONVERTION_CHATGPT", Context.MODE_PRIVATE)
    return sharedPreferences.getString(conversation_id, "NULL")
  }
  private suspend fun sendStreamMessage(text: String, channelId: String) {
    val channelClient = chatClient.channel(channelId)
    channelClient.sendMessage(
      message = Message(
        id = UUID.randomUUID().toString(),
        cid = channelClient.cid,
        text = text,
        extraData = mutableMapOf("ChatGPT" to true)
      )
    ).await()
  }

  companion object {
    const val DATA_TEXT = "DATA_TEXT"
    const val DATA_CHANNEL_ID = "DATA_CHANNEL_ID"
    const val DATA_SUCCESS = "DATA_SUCCESS"
    const val DATA_FAILURE = "DATA_FAILURE"
    const val CONVERSATION_ID = "-1"
  }
}
