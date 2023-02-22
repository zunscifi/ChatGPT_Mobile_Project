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

package com.toandtpro.chatgpt.feature.login

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.MutableLiveData
import com.acsbendi.requestinspectorwebview.RequestInspectorWebViewClient
import com.acsbendi.requestinspectorwebview.WebViewRequest
import com.toandtpro.chatgpt.core.designsystem.component.ChatGPTSmallTopBar
import com.toandtpro.chatgpt.core.navigation.AppComposeNavigator
import com.toandtpro.chatgpt.core.navigation.ChatGPTScreens
import com.toandtpro.chatgpt.core.network.AUTHORIZATION
import com.toandtpro.chatgpt.core.network.COOKIE
import com.toandtpro.chatgpt.core.network.USER_AGENT
import com.toandtpro.chatgpt.core.preferences.Preferences

public const val NORMAL = -1
public const val LOGIN_COMPLETED = 0
public const val NOT_AUTHORIZED = 1
public const val LOGIN_ING = 2
var stopLogin = false
/**
 *
 */
fun LoginGPT(webView: WebView, context: Context, stateLiveData: MutableLiveData<Int>) {
  val preferences = Preferences(context)
  var isCompleted = false
  webView.apply {
    webChromeClient = WebChromeClient()
    webViewClient = object : RequestInspectorWebViewClient(this@apply) {
      override fun shouldInterceptRequest(
        view: WebView,
        webViewRequest: WebViewRequest
      ): WebResourceResponse? {
        try {
          if (stateLiveData.value == NORMAL && checkIfAuthorized(webViewRequest.headers)) {
            val authorization = webViewRequest.headers[AUTHORIZATION] ?: return null
            val cookie = webViewRequest.headers[COOKIE] ?: return null
            val userAgent = webViewRequest.headers[USER_AGENT] ?: return null
            stateLiveData.postValue(LOGIN_ING)
            isCompleted = true
            preferences.authorization = authorization
            preferences.cookie = cookie
            preferences.userAgent = userAgent
            Handler(Looper.getMainLooper()).post {
              Toast.makeText(context, R.string.toast_logged_in, Toast.LENGTH_SHORT).show()
            }
          } else if (stateLiveData.value == LOGIN_ING && isCompleted) {
            stateLiveData.postValue(LOGIN_COMPLETED)
          }
        } catch (ignore: Exception) {
        }
        return super.shouldInterceptRequest(view, webViewRequest)
      }
    }.apply {
      loadUrl("https://chat.openai.com/chat")
    }
  }
}



@Composable
fun ChatGPTLogin(
  composeNavigator: AppComposeNavigator,
  viewModel: ChatGPTLoginViewModel = hiltViewModel()
) {
  val context = LocalContext.current
  val webView = remember { WebView(context) }

  BackHandler {
    if (webView.canGoBack()) {
      webView.goBack()
    } else {
      composeNavigator.navigateUp()
    }
  }

  Scaffold(topBar = {
    ChatGPTSmallTopBar(
      title = stringResource(id = R.string.top_bar_login)
    )
  }) { padding ->
    AndroidView(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding),
      factory = {
        webView.apply {
          webChromeClient = WebChromeClient()
          webViewClient = object : RequestInspectorWebViewClient(this@apply) {
            override fun shouldInterceptRequest(
              view: WebView,
              webViewRequest: WebViewRequest
            ): WebResourceResponse? {
              if (checkIfAuthorized(webViewRequest.headers)) {
                val authorization = webViewRequest.headers[AUTHORIZATION] ?: return null
                val cookie = webViewRequest.headers[COOKIE] ?: return null
                val userAgent = webViewRequest.headers[USER_AGENT] ?: return null
                viewModel.persistLoginInfo(
                  authorization = authorization,
                  cookie = cookie,
                  userAgent = userAgent
                )
                composeNavigator.navigateAndClearBackStack(ChatGPTScreens.Channels.name)

                Handler(Looper.getMainLooper()).post {
                  Toast.makeText(context, R.string.toast_logged_in, Toast.LENGTH_SHORT).show()
                }
              }
              return super.shouldInterceptRequest(view, webViewRequest)
            }
          }.apply {
            loadUrl("https://chat.openai.com/chat")
          }
        }
      }
    )
  }
}
fun checkIfAuthorized(header: Map<String, String>): Boolean {
  return header.containsKey(AUTHORIZATION) &&
    header.containsKey(COOKIE) &&
    header.containsKey(USER_AGENT)
}
fun checkIfAuthorized(header: Map<String, String>, callback: (Boolean) -> Unit): Boolean {
  return header.containsKey(AUTHORIZATION) &&
    header.containsKey(COOKIE) &&
    header.containsKey(USER_AGENT)
}
