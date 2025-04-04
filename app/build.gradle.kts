/*
 * Designed and developed by 2022 skydoves (Jaewoong Eum)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
plugins {
  id("skydoves.android.application")
  id("skydoves.android.application.compose")
  id("skydoves.android.hilt")
  id("skydoves.spotless")
  id("kotlin-parcelize")
  id("dagger.hilt.android.plugin")
    id("org.jetbrains.kotlin.android")
}

android {
  namespace = "com.toandtpro.chatgpt"
  compileSdk = Configurations.compileSdk

  defaultConfig {
    applicationId = "com.toandtpro.chatgpt"
    minSdk = 21
    targetSdk = Configurations.targetSdk
    versionCode = Configurations.versionCode
    versionName = Configurations.versionName
  }

  packagingOptions {
    resources {
      excludes.add("/META-INF/{AL2.0,LGPL2.1}")
    }
  }
  buildTypes {
    create("benchmark") {
      signingConfig = signingConfigs.getByName("debug")
      matchingFallbacks += listOf("release")
      isDebuggable = false
    }
    getByName("release") {
      // Enables code shrinking, obfuscation, and optimization for only
      // your project's release build type.
      isMinifyEnabled = true

      // Enables resource shrinking, which is performed by the
      // Android Gradle plugin.
      isShrinkResources = true

      // Includes the default ProGuard rules files that are packaged with
      // the Android Gradle plugin. To learn more, go to the section about
      // R8 configuration files.
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
      )
    }
  }
  buildFeatures {
    viewBinding = true
  }

}

dependencies {
  implementation ("androidx.multidex:multidex:2.0.1")
  implementation ("com.google.android.gms:play-services-ads:21.5.0")
  implementation ("androidx.lifecycle:lifecycle-runtime-ktx:2.3.1")
  implementation ("androidx.lifecycle:lifecycle-process:2.3.1")
  kapt ("androidx.lifecycle:lifecycle-compiler:2.3.1")
  implementation ("com.github.chnouman:AwesomeDialog:1.0.5")
  implementation ("dev.shreyaspatil.MaterialDialog:MaterialDialog:2.2.3")
  implementation ("com.github.invissvenska:ModalBottomSheetDialog:1.0.5")
  implementation ("com.github.mejdi14:Flat-Dialog-Android:1.0.5")
  implementation ("co.zsmb:materialdrawer-kt:3.0.0")
  implementation ("net.gotev:speech:1.6.2")
  // lottie
  implementation("com.airbnb.android:lottie:5.2.0")

  // core modules
  implementation(project(":core-designsystem"))
  implementation(project(":core-navigation"))
  implementation(project(":core-data"))

  // feature modules
  implementation(project(":feature-chat"))
  implementation(project(":feature-login"))

  // material
  implementation(libs.androidx.appcompat)

  // compose
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.runtime)
  implementation(libs.androidx.compose.ui.tooling)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.constraintlayout)

  // jetpack
  implementation(libs.androidx.startup)
  implementation(libs.hilt.android)
  implementation(libs.androidx.hilt.navigation.compose)
    implementation("com.google.android.material:material:1.4.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
  implementation("androidx.work:work-runtime-ktx:2.7.1")
  kapt(libs.hilt.compiler)

  // image loading
  implementation(libs.landscapist.glide)

  // logger
  implementation(libs.stream.log)

  // firebase
  implementation(platform(libs.firebase.bom))
  implementation(libs.firebase.analytics)
  implementation(libs.firebase.messaging)
  implementation(libs.firebase.crashlytics)
}

if (file("google-services.json").exists()) {
  apply(plugin = libs.plugins.gms.googleServices.get().pluginId)
  apply(plugin = libs.plugins.firebase.crashlytics.get().pluginId)
}