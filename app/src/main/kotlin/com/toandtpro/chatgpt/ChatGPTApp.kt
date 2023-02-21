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

package com.toandtpro.chatgpt

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.multidex.MultiDexApplication
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.appopen.AppOpenAd.AppOpenAdLoadCallback
import java.util.Date

private const val AD_UNIT_ID = "ca-app-pub-3940256099942544/3419835294"
private const val LOG_TAG = "MyApplication"
@HiltAndroidApp
class ChatGPTApp :
  MultiDexApplication(), Application.ActivityLifecycleCallbacks, LifecycleObserver {

  private lateinit var appOpenAdManager: AppOpenAdManager
  private var currentActivity: Activity? = null

  override fun onCreate() {
    super.onCreate()
    registerActivityLifecycleCallbacks(this)

    // Log the Mobile Ads SDK version.
    Log.d(LOG_TAG, "Google Mobile Ads SDK Version: " + MobileAds.getVersion())

    MobileAds.initialize(this) {}
    ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    appOpenAdManager = AppOpenAdManager()
  }

  /** LifecycleObserver method that shows the app open ad when the app moves to foreground. */
  @OnLifecycleEvent(Lifecycle.Event.ON_START)
  fun onMoveToForeground() {
    // Show the ad (if available) when the app moves to foreground.
    currentActivity?.let { appOpenAdManager.showAdIfAvailable(it) }
  }

  /** ActivityLifecycleCallback methods. */
  override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

  override fun onActivityStarted(activity: Activity) {
    // An ad activity is started when an ad is showing, which could be AdActivity class from Google
    // SDK or another activity class implemented by a third party mediation partner. Updating the
    // currentActivity only when an ad is not showing will ensure it is not an ad activity, but the
    // one that shows the ad.
    if (!appOpenAdManager.isShowingAd) {
      currentActivity = activity
    }
  }

  override fun onActivityResumed(activity: Activity) {}

  override fun onActivityPaused(activity: Activity) {}

  override fun onActivityStopped(activity: Activity) {}

  override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

  override fun onActivityDestroyed(activity: Activity) {}

  /**
   * Shows an app open ad.
   *
   * @param activity the activity that shows the app open ad
   * @param onShowAdCompleteListener the listener to be notified when an app open ad is complete
   */
  fun showAdIfAvailable(activity: Activity, onShowAdCompleteListener: OnShowAdCompleteListener) {
    // We wrap the showAdIfAvailable to enforce that other classes only interact with MyApplication
    // class.
    appOpenAdManager.showAdIfAvailable(activity, onShowAdCompleteListener)
  }

  /**
   * Interface definition for a callback to be invoked when an app open ad is complete (i.e.
   * dismissed or fails to show).
   */
  interface OnShowAdCompleteListener {
    fun onShowAdComplete()
  }

  /** Inner class that loads and shows app open ads. */
  private inner class AppOpenAdManager {

    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    var isShowingAd = false

    /** Keep track of the time an app open ad is loaded to ensure you don't show an expired ad. */
    private var loadTime: Long = 0

    /**
     * Load an ad.
     *
     * @param context the context of the activity that loads the ad
     */
    fun loadAd(context: Context) {
      // Do not load ad if there is an unused ad or one is already loading.
      if (isLoadingAd || isAdAvailable()) {
        return
      }

      isLoadingAd = true
      val request = AdRequest.Builder().build()
      AppOpenAd.load(
        context,
        AD_UNIT_ID,
        request,
        AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
        object : AppOpenAdLoadCallback() {
          /**
           * Called when an app open ad has loaded.
           *
           * @param ad the loaded app open ad.
           */
          override fun onAdLoaded(ad: AppOpenAd) {
            appOpenAd = ad
            isLoadingAd = false
            loadTime = Date().time
            Log.d(LOG_TAG, "onAdLoaded.")
          }

          /**
           * Called when an app open ad has failed to load.
           *
           * @param loadAdError the error.
           */
          override fun onAdFailedToLoad(loadAdError: LoadAdError) {
            isLoadingAd = false
            Log.d(LOG_TAG, "onAdFailedToLoad: " + loadAdError.message)
          }
        }
      )
    }

    /** Check if ad was loaded more than n hours ago. */
    private fun wasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {
      val dateDifference: Long = Date().time - loadTime
      val numMilliSecondsPerHour: Long = 3600000
      return dateDifference < numMilliSecondsPerHour * numHours
    }

    /** Check if ad exists and can be shown. */
    private fun isAdAvailable(): Boolean {
      // Ad references in the app open beta will time out after four hours, but this time limit
      // may change in future beta versions. For details, see:
      // https://support.google.com/admob/answer/9341964?hl=en
      return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4)
    }

    /**
     * Show the ad if one isn't already showing.
     *
     * @param activity the activity that shows the app open ad
     */
    fun showAdIfAvailable(activity: Activity) {
      showAdIfAvailable(
        activity,
        object : OnShowAdCompleteListener {
          override fun onShowAdComplete() {
            // Empty because the user will go back to the activity that shows the ad.
          }
        }
      )
    }

    /**
     * Show the ad if one isn't already showing.
     *
     * @param activity the activity that shows the app open ad
     * @param onShowAdCompleteListener the listener to be notified when an app open ad is complete
     */
    fun showAdIfAvailable(activity: Activity, onShowAdCompleteListener: OnShowAdCompleteListener) {
      // If the app open ad is already showing, do not show the ad again.
      if (isShowingAd) {
        Log.d(LOG_TAG, "The app open ad is already showing.")
        return
      }

      // If the app open ad is not available yet, invoke the callback then load the ad.
      if (!isAdAvailable()) {
        Log.d(LOG_TAG, "The app open ad is not ready yet.")
        onShowAdCompleteListener.onShowAdComplete()
        loadAd(activity)
        return
      }

      Log.d(LOG_TAG, "Will show ad.")

      appOpenAd!!.setFullScreenContentCallback(
        object : FullScreenContentCallback() {
          /** Called when full screen content is dismissed. */
          override fun onAdDismissedFullScreenContent() {
            // Set the reference to null so isAdAvailable() returns false.
            appOpenAd = null
            isShowingAd = false
            Log.d(LOG_TAG, "onAdDismissedFullScreenContent.")

            onShowAdCompleteListener.onShowAdComplete()
            loadAd(activity)
          }

          /** Called when fullscreen content failed to show. */
          override fun onAdFailedToShowFullScreenContent(adError: AdError) {
            appOpenAd = null
            isShowingAd = false
            Log.d(LOG_TAG, "onAdFailedToShowFullScreenContent: " + adError.message)

            onShowAdCompleteListener.onShowAdComplete()
            loadAd(activity)
          }

          /** Called when fullscreen content is shown. */
          override fun onAdShowedFullScreenContent() {
            Log.d(LOG_TAG, "onAdShowedFullScreenContent.")
          }
        }
      )
      isShowingAd = true
      appOpenAd!!.show(activity)
    }
  }
}
