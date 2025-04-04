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

package com.toandtpro.chatgpt.ui

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.toandtpro.chatgpt.core.designsystem.component.ChatGPTBackground
import com.toandtpro.chatgpt.core.designsystem.component.ChatGPTSmallTopBar
import com.toandtpro.chatgpt.core.designsystem.theme.ChatGPTComposeTheme
import com.toandtpro.chatgpt.core.navigation.AppComposeNavigator
import com.toandtpro.chatgpt.navigation.ChatGPTNavHost



@Composable
fun ChatGPTMain(
  composeNavigator: AppComposeNavigator
) {
  ChatGPTComposeTheme {
    val navHostController = rememberNavController()

    LaunchedEffect(Unit) {
      composeNavigator.handleNavigationCommands(navHostController)
    }

    ChatGPTBackground {
      ChatGPTNavHost(navHostController = navHostController, composeNavigator = composeNavigator)
    }
  }
}


