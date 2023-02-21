package com.project.toandt.View.Activity

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.example.awesomedialog.AwesomeDialog
import com.example.awesomedialog.body
import com.example.awesomedialog.icon
import com.example.awesomedialog.onNegative
import com.example.awesomedialog.position
import com.example.awesomedialog.title
import com.example.flatdialoglibrary.dialog.FlatDialog
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.material.card.MaterialCardView
import com.mikepenz.materialdrawer.Drawer
import com.mikepenz.materialdrawer.DrawerBuilder
import com.mikepenz.materialdrawer.model.DividerDrawerItem
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem
import com.mikepenz.materialdrawer.model.ProfileDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem
import com.project.toandt.Control.Database.DatabaseHelper
import com.project.toandt.Control.SharedPref.ConversionSharedPreferences
import com.project.toandt.Control.WorkerChat.ChatManager
import com.project.toandt.Control.WorkerChat.ChatResponseHandler
import com.project.toandt.Control.WorkerChat.ChatWorkerBuilder
import com.project.toandt.Model.Conversation
import com.project.toandt.Model.ConversationManager
import com.project.toandt.Model.MessageManager
import com.toandtpro.chatgpt.ChatGPTApp
import com.toandtpro.chatgpt.R
import com.toandtpro.chatgpt.core.network.AUTHORIZATION
import com.toandtpro.chatgpt.core.network.COOKIE
import com.toandtpro.chatgpt.core.network.USER_AGENT
import com.toandtpro.chatgpt.core.preferences.Preferences
import com.toandtpro.chatgpt.databinding.ActivityChatBinding
import com.toandtpro.chatgpt.feature.login.ChatGPTLogin
import com.toandtpro.chatgpt.feature.login.LOGIN_COMPLETED
import com.toandtpro.chatgpt.feature.login.LoginGPT
import dev.shreyaspatil.MaterialDialog.AbstractDialog
import dev.shreyaspatil.MaterialDialog.BottomSheetMaterialDialog
import dev.shreyaspatil.MaterialDialog.interfaces.DialogInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import net.gotev.speech.GoogleVoiceTypingDisabledException
import net.gotev.speech.Speech
import net.gotev.speech.SpeechDelegate
import net.gotev.speech.SpeechRecognitionNotAvailable
import nl.invissvenska.modalbottomsheetdialog.Item
import nl.invissvenska.modalbottomsheetdialog.ModalBottomSheetDialog
import java.io.ByteArrayOutputStream

class ChatActivity : AppCompatActivity(), ModalBottomSheetDialog.Listener {
  private lateinit var databaseHelper: DatabaseHelper
  private lateinit var binding : ActivityChatBinding
  private lateinit var currentIDConversation : String
  private lateinit var messageManager: MessageManager
  private lateinit var conversationManager: ConversationManager
  private lateinit var drawer : Drawer
  private lateinit var chatManager: ChatManager
  private lateinit var chatResponseHandler: ChatResponseHandler
  private lateinit var chatWorkerBuilder: ChatWorkerBuilder
  private lateinit var conversionSharedPreferences: ConversionSharedPreferences
  private val REQUEST_REC_AUDIO_PERMISSION = 1
  private val REQUEST_SHARE_SCREEN_PERMISSION = 2
  private val isRecordingAudio = MutableLiveData<Boolean>()
  private val STATE_WAITING_RESPONSE_STR = "ChatGPT are typing..."
  private val STATE_WAITING_RECODING_STR = "ChatGPT are hearing..."
  private var backPressedOnce = false
  private var interstitialAd: InterstitialAd? = null
  private var adIsLoading: Boolean = false
  private val uiScope = CoroutineScope(Dispatchers.Main)
  override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      binding = ActivityChatBinding.inflate(layoutInflater)
      val view = binding.root
      setContentView(view)
      initInterAds()
      addControls()
      addEvents()
      Speech.init(this, packageName);
    }
  override fun onResume() {
    super.onResume()
  }
  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    when (requestCode) {
      REQUEST_REC_AUDIO_PERMISSION -> {
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          // Permission has been granted, call the hello function
          handleSpeechToText()
        } else  {
          // Permission has been denied, handle it
        }
      }
      REQUEST_SHARE_SCREEN_PERMISSION ->{
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          // Permission has been granted, call the hello function
          handleShareScreen()
        } else  {
          // Permission has been denied, handle it
        }
      }
    }
  }
  override fun onBackPressed() {
    if (backPressedOnce) {
      finishAffinity()
    }

    this.backPressedOnce = true
    Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show()

    Handler().postDelayed({
      backPressedOnce = false
    }, 2000)
  }
  override fun onDestroy() {
    super.onDestroy()
    //Protect app from "If user open app and close app so quickly and Speech not init at time"
    try{
      Speech.getInstance().shutdown();
    }catch (e : Exception){}
  }
  private fun addEvents() {
    binding.imgbtnShareScreen.setOnClickListener(){
      if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        == PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_SHARE_SCREEN_PERMISSION)
      }else{
        handleShareScreen()
      }
    }
    binding.imgbtnSettingBottomDialog.setOnClickListener(){
      handleSettingDialog()
    }
    binding.imgbtnMenuConversation.setOnClickListener(){
      drawer.openDrawer()
    }
    binding.imgbtnSpeechToText.setOnClickListener(){
      if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
        == PackageManager.PERMISSION_GRANTED) {
        // Permission has been granted, call the hello function
        handleSpeechToText()
      } else {
        // Permission has not been granted, request it
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_REC_AUDIO_PERMISSION)
      }
    }
    binding.imgbtnSendRequest.setOnClickListener(){
      if(binding.imgbtnSendRequest.isEnabled){
        if(binding.edtxtRequestText.text.isNotEmpty()){
          chatResponseHandler.handleEventRequestResponse(
            binding.edtxtRequestText.text.toString(),
            currentIDConversation,
            this@ChatActivity,
            this@ChatActivity,
            databaseHelper,
            binding,
            messageManager,
            chatManager,
            conversationManager
          )
        }else{
          Toast.makeText(this, "Please enter the question", Toast.LENGTH_SHORT).show()
        }
      }else{
        Toast.makeText(this, "Only one message at a time", Toast.LENGTH_SHORT).show()
      }
    }
  }
  private fun handleShareScreen() {
    Log.i("ChatActivity", "handleShareScreen started ...")
    val image = getScreenBitmap(this@ChatActivity)
    val uri = getImageUri(this@ChatActivity, image)
    val shareIntent = Intent().apply {
      action = Intent.ACTION_SEND
      putExtra(Intent.EXTRA_STREAM, uri)
      type = "image/png"
    }
    startActivity(Intent.createChooser(shareIntent, "Share via"))
    Log.i("ChatActivity", "handleShareScreen completed")
  }
  private fun getScreenBitmap(activity: Activity): Bitmap {
    val view = activity.window.decorView.rootView
    view.isDrawingCacheEnabled = true
    val bitmap = Bitmap.createBitmap(view.drawingCache)
    view.isDrawingCacheEnabled = false
    return bitmap
  }
  private fun getImageUri(context: Context, image: Bitmap): Uri {
    val bytes = ByteArrayOutputStream()
    image.compress(Bitmap.CompressFormat.PNG, 100, bytes)
    val path = MediaStore.Images.Media.insertImage(context.contentResolver, image, "screenshot", null)
    return Uri.parse(path)
  }
  private fun handleDisplayChatMessages(){
    if(conversationManager.getConversations().size == 1){
      currentIDConversation = conversationManager.getConversations()[0].id.toString()
      conversionSharedPreferences.saveStringToSharedPreferences(currentIDConversation.toString())
    }
    initMessageManager()
    Log.i("ChatActivity", "Display Chat Messages with ID CONVERSATION $currentIDConversation")
    chatManager.handleDisplayChatMessages(currentIDConversation, this@ChatActivity, conversationManager, messageManager)
  }
  private fun handleSpeechToText(){
    if(isRecordingAudio.value == false){
      try {
        isRecordingAudio.value = true
        // you must have android.permission.RECORD_AUDIO granted at this point
        Speech.getInstance().startListening(object : SpeechDelegate {
          override fun onStartOfSpeech() {
            Log.i("speech", "speech recognition is now active")
          }

          override fun onSpeechRmsChanged(value: Float) {
            Log.d("speech", "rms is now: $value")
          }

          override fun onSpeechPartialResults(results: List<String>) {
            val str = StringBuilder()
            for (res in results) {
              str.append(res).append(" ")
            }
            binding.edtxtRequestText.setText(str.toString().trim())
            binding.edtxtRequestText.setSelection(str.toString().trim().length)
            Log.i("speech", "partial result: ${str.toString().trim()}")
          }

          override fun onSpeechResult(result: String) {
            Log.i("speech", "result: $result")
          }
        })
      } catch (exc: SpeechRecognitionNotAvailable) {
        Log.e("speech", "Speech recognition is not available on this device!")
        // You can prompt the user if he wants to install Google App to have
        // speech recognition, and then you can simply call:
        //
        // SpeechUtil.redirectUserToGoogleAppOnPlayStore(this);
        //
        // to redirect the user to the Google App page on Play Store
      } catch (exc: GoogleVoiceTypingDisabledException) {
        Log.e("speech", "Google voice typing must be enabled!")
      }
    }else{
      Speech.getInstance().stopListening()
      isRecordingAudio.value = false
    }


  }
  private fun addControls() {
      initSharedPref()
      handleSQLite()
      initMessageManager()
      initConversationManager()
      initWorkerChat()
      handleDrawer()
      handleDisplayChatMessages()
      checkRecordAudio()
      initDefaultValue()
  }
  private fun showDialogToast(tittle : String, subcription : String){
    val awesomeDialog = AwesomeDialog.build(this)
      .title(tittle)
      .body(subcription)
      .icon(R.drawable.baseline_check_circle_24)
      .position(AwesomeDialog.POSITIONS.CENTER)
      .onNegative("Goto ChatGPT"){}
    awesomeDialog.setCancelable(true)
    awesomeDialog.setCanceledOnTouchOutside(true)
  }
  private fun showDialogToast(tittle : String, subcription : String, icon : Int){
    val awesomeDialog = AwesomeDialog.build(this)
      .title(tittle)
      .body(subcription)
      .icon(icon)
      .position(AwesomeDialog.POSITIONS.CENTER)
      .onNegative("Goto ChatGPT"){}
    awesomeDialog.setCancelable(true)
    awesomeDialog.setCanceledOnTouchOutside(true)
  }
  private fun handleSettingDialog() {
    ModalBottomSheetDialog.Builder()
      .setHeader("Extended Function")
      .add(R.menu.setting)
      .setRoundedModal(true)
      .setHeaderLayout(R.layout.setting_dialog_header)
      .setItemLayout(R.layout.setting_dialog_item)
      .show(supportFragmentManager, "Haaa")
  }
  private fun initSharedPref() {
    conversionSharedPreferences = ConversionSharedPreferences(this@ChatActivity)
  }
  private fun initWorkerChat() {
    chatManager = ChatManager(binding)
    chatResponseHandler = ChatResponseHandler(binding)
    chatWorkerBuilder = ChatWorkerBuilder(currentIDConversation)
  }
  private fun handleDrawer(){
    MobileAds.initialize(this) {}
    val viewGroup = LayoutInflater.from(this).inflate(R.layout.item_top_drawer, null) as ViewGroup
    val mAdView : AdView = viewGroup.findViewById(R.id.adView)
    val mcv_new_chat : MaterialCardView = viewGroup.findViewById(R.id.mcv_new_chat)
    val mcv_delete_all_conversation : MaterialCardView = viewGroup.findViewById(R.id.mcv_delete_all_conversation)
    mcv_new_chat.setOnClickListener() {
      val isConversation = databaseHelper.addConversation("New Conversation")
      if (isConversation != -1) {
        currentIDConversation = isConversation.toString()
        databaseHelper.addMessage(currentIDConversation.toInt(),
          DatabaseHelper.SENDER_SEVER,
          getString(R.string.chatgpt_welcome_message),
        System.currentTimeMillis().toString())
        drawer.closeDrawer()
        initConversationManager()
        handleDrawer()
        initMessageManager()
        handleDisplayChatMessages()
        if(interstitialAd != null){
          showInterstitial()
        }
      }
    }
    mcv_delete_all_conversation.setOnClickListener(){
      val mBottomSheetDialog = BottomSheetMaterialDialog.Builder(this)
        .setTitle("Remove?")
        .setMessage("Are you sure want to remove all conversations?")
        .setCancelable(false)
        .setPositiveButton("Remove", R.drawable.baseline_delete_outline_24
        ) { dialogInterface, which ->
          for(conversation in conversationManager.getConversations()){
            if(conversationManager.getConversations().size > 1){
              val isDeletedConversation = databaseHelper.deleteConversation(conversation.id.toInt())
              if(isDeletedConversation){
                initConversationManager()
                val isDeletedMessages = databaseHelper.deleteAllMessagesWithConversationId(conversation.id.toInt())
              }
            }
          }
          initConversationManager()
          currentIDConversation = conversationManager.getConversations()[0].id.toString()
          drawer.closeDrawer()
          handleDrawer()
          initMessageManager()
          handleDisplayChatMessages()
          showDialogToast("Completed", "Remove all conversation completed")
          dialogInterface.dismiss()
          if(interstitialAd != null){
            showInterstitial()
          }
        }
        .setNegativeButton("Cancel", R.drawable.baseline_close_24
        ) { dialogInterface, which ->
          dialogInterface.dismiss()
        }
        .build()

// Show Dialog
      mBottomSheetDialog.show()

    }
    val adRequest = AdRequest.Builder().build()
    mAdView.loadAd(adRequest)
    mAdView.adListener = object: AdListener() {
      override fun onAdClicked() {
        // Code to be executed when the user clicks on an ad.
      }

      override fun onAdClosed() {
        // Code to be executed when the user is about to return
        // to the app after tapping on an ad.
      }

      override fun onAdFailedToLoad(adError : LoadAdError) {
        // Code to be executed when an ad request fails.
        mAdView.visibility = View.GONE
      }

      override fun onAdImpression() {
        // Code to be executed when an impression is recorded
        // for an ad.
      }

      override fun onAdLoaded() {
        // Code to be executed when an ad finishes loading.
        mAdView.visibility = View.VISIBLE
      }

      override fun onAdOpened() {
        // Code to be executed when an ad opens an overlay that
        // covers the screen.
      }
    }
    drawer = DrawerBuilder()
      .withActivity(this)
      .withCloseOnClick(true)
      .withSliderBackgroundColorRes(R.color.colorPrimary)
      .withTranslucentStatusBar(true)
      .withStickyFooter(viewGroup)
      .withOnDrawerItemClickListener(object : Drawer.OnDrawerItemClickListener {
        override fun onItemClick(view: View?, position: Int, drawerItem: IDrawerItem<*>): Boolean {
          if(drawerItem is ProfileDrawerItem){
            val s = drawerItem.contentDescription.toString()
            currentIDConversation = s
            initMessageManager()
            handleDisplayChatMessages()
            drawer.closeDrawer()
          }
          return true
        }
      })
      .withOnDrawerItemLongClickListener(object : Drawer.OnDrawerItemLongClickListener{
        override fun onItemLongClick(view: View, position: Int, drawerItem: IDrawerItem<*>): Boolean {
          if(drawerItem is ProfileDrawerItem){
            if(!isFinishing){
              val flatDialog = FlatDialog(this@ChatActivity)
              flatDialog.setTitle("Conversation Detail")
                .setBackgroundColor(ContextCompat.getColor(this@ChatActivity, R.color.colorPrimary))
                .setSubtitle("Change anything you want here")
                .setFirstTextFieldHint("This is name of conversation...")
                .setFirstTextField(drawerItem.name.toString())
                .setFirstButtonText("SAVE DETAIL")
                .setFirstButtonColor(ContextCompat.getColor(this@ChatActivity, R.color.colorSecondary))
                .setFirstButtonTextColor(ContextCompat.getColor(this@ChatActivity, R.color.white))
                .setSecondButtonText("DELETE CONVERSATION")
                .setSecondButtonColor(ContextCompat.getColor(this@ChatActivity, R.color.colorSecondary))
                .setSecondButtonTextColor(ContextCompat.getColor(this@ChatActivity, R.color.white))
                .setThirdButtonText("CANCEL")
                .setThirdButtonColor(ContextCompat.getColor(this@ChatActivity, R.color.colorSecondary))
                .setThirdButtonTextColor(ContextCompat.getColor(this@ChatActivity,  R.color.white))
                .withFirstButtonListner {
                  val nameConversation = flatDialog.firstTextField
                  databaseHelper.updateConversation(drawerItem.contentDescription.toString().toInt(), nameConversation)
                  initConversationManager()
                  initMessageManager()
                  handleDisplayChatMessages()
                  drawer.closeDrawer()
                  handleDrawer()
                  flatDialog.dismiss()
                }
                .withSecondButtonListner {
                  initConversationManager()
                  if(conversationManager.getConversations().size > 1){
                    val isDeletedConversation = databaseHelper.deleteConversation(drawerItem.contentDescription.toString().toInt())
                    if(isDeletedConversation){
                      val isDeletedMessages = databaseHelper.deleteAllMessagesWithConversationId(drawerItem.contentDescription.toString().toInt())
                      initConversationManager()
                      currentIDConversation = conversationManager.getConversations()[0].id.toString()
                      drawer.closeDrawer()
                      handleDrawer()
                      initMessageManager()
                      handleDisplayChatMessages()
                      flatDialog.dismiss()
                      Toast.makeText(this@ChatActivity, "Delete conversation completed!", Toast.LENGTH_SHORT).show();
                    }
                  }else{
                    Toast.makeText(this@ChatActivity, "There must be at least one Message that exists!", Toast.LENGTH_SHORT).show();
                  }
                }
                .withThirdButtonListner {
                  flatDialog.dismiss()
                }
                .show()
            }
          }
          return true
        }
      })
      .build()
    val conversationList = conversationManager.getConversations()
    drawer.addItem(PrimaryDrawerItem()
      .withName("Conversation ChatGPT")
      .withTextColorRes(R.color.white))
    for(conversation in conversationList){
      drawer.addItem(
        ProfileDrawerItem()
          .withName(conversation.name)
          .withTextColorRes(R.color.white)
          .withIcon(R.drawable.openai)
          .withNameShown(true)
          .withEmail("ID Conversation: GPT${conversation.id}")
          .withContentDescription("${conversation.id}")
      )
    }
    drawer.addItem(DividerDrawerItem())
  }
  private fun initConversationManager() {
    conversationManager = ConversationManager()
    val cursor = databaseHelper.allConversations
    while(cursor.moveToNext()){
      val conversation = Conversation(cursor.getLong(0), cursor.getString(1))
      conversationManager.addConversation(conversation)
    }
    cursor.close()
  }
  private fun initDefaultValue() {
    isRecordingAudio.value = false
  }
  private fun initMessageManager() {
    messageManager = databaseHelper.getMessageManager(currentIDConversation.toInt())
  }
  private fun handleSQLite() {
    handleFirstInit()
    handleDataCurrentConversation()
  }
  private fun handleDataCurrentConversation() {
    currentIDConversation = conversionSharedPreferences.getStringFromSharedPreferences().toString()
  }
  private fun handleFirstInit() {
    databaseHelper = DatabaseHelper(this)
    val conversationID : String = conversionSharedPreferences.getStringFromSharedPreferences().toString()
    println("dddddddddddddd  $conversationID")
    if(conversationID == "NULL"){
      val conversationId = databaseHelper.addConversation("New Conversation")
      if(conversationId != -1){
        conversionSharedPreferences.saveStringToSharedPreferences(conversationId.toString())
        currentIDConversation = conversationId.toString()
        databaseHelper.addMessage(currentIDConversation.toInt(),
            DatabaseHelper.SENDER_SEVER,
            getString(R.string.chatgpt_welcome_message),
            System.currentTimeMillis().toString())
        initConversationManager()
        initMessageManager()
        initWorkerChat()
        handleDisplayChatMessages()
      }
    }
  }
  private fun checkRecordAudio() {
    isRecordingAudio.observe(this, Observer { RECORDING ->
      if (RECORDING) {
        binding.imgbtnSendRequest.isEnabled = false
        binding.txtStateChatgpt.text = STATE_WAITING_RECODING_STR
        binding.txtStateWhileWaitingResponse.visibility = View.VISIBLE
        binding.imgbtnSpeechToText.setImageResource(R.drawable.icon__pause)
      } else {
        binding.imgbtnSendRequest.isEnabled = true
        binding.txtStateChatgpt.text = STATE_WAITING_RESPONSE_STR
        binding.txtStateWhileWaitingResponse.visibility = View.GONE
        binding.imgbtnSpeechToText.setImageResource(R.drawable.icon_voice)
      }
    })
  }
  /**
   * Setting Dialog Event
   */
  override fun onItemSelected(tag: String, item: Item) {
    if(item.id == R.id.action_clear_memory){
      try{
        val sharedPrefs = getSharedPreferences("CONVERTION_CHATGPT", Context.MODE_PRIVATE)
        sharedPrefs.edit().remove(currentIDConversation).apply()
        showDialogToast("Completed", "Remove ChatGPT Memory Completed")
      }catch (e : Exception){}
    }else if(item.id == R.id.action_logout){
      handleLogout()
    }else if(item.id == R.id.action_lang_spec_boost){
      showDialogToast( "Feature Not Available Now!",
        "This feature help you get faster response from ChatGPT when using specific language like Vietnamese ... It will support up to 58 language",
        R.drawable.round_info_24)
    }
  }
  public fun handleLogout(){
    val preferences = Preferences(this@ChatActivity)
    preferences.authorization = ""
    preferences.cookie = ""
    preferences.userAgent = ""
    val intent = Intent(this, LoginActivity::class.java)
    intent.putExtra("MODE", "SIGN_OUT")
    finish()
    startActivity(intent)
  }
  public fun initInterAds(){
    // Initialize the Mobile Ads SDK.
    MobileAds.initialize(this) {}

    // Set your test devices. Check your logcat output for the hashed device ID to
    // get test ads on a physical device. e.g.
    // "Use RequestConfiguration.Builder().setTestDeviceIds(Arrays.asList("ABCDEF012345"))
    // to get test ads on this device."
    MobileAds.setRequestConfiguration(
      RequestConfiguration.Builder().setTestDeviceIds(listOf("ABCDEF012345")).build()
    )
    if (!adIsLoading && interstitialAd == null) {
      adIsLoading = true
      loadAd()
    }
  }
  private fun loadAd() {
    var adRequest = AdRequest.Builder().build()

    InterstitialAd.load(
      this,
      resources.getString(R.string.inter_ads),
      adRequest,
      object : InterstitialAdLoadCallback() {
        override fun onAdFailedToLoad(adError: LoadAdError) {
          Log.d("ChatActivity", adError.message)
          interstitialAd = null
          adIsLoading = false
          val error =
            "domain: ${adError.domain}, code: ${adError.code}, " + "message: ${adError.message}"
        }

        override fun onAdLoaded(ad: InterstitialAd) {
          Log.d("ChatActivity", "Ad was loaded.")
          interstitialAd = ad
          adIsLoading = false
        }
      }
    )
  }
  // Show the ad if it's ready. Otherwise toast and restart the game.
  private fun showInterstitial() {
    if (interstitialAd != null) {
      interstitialAd?.fullScreenContentCallback =
        object : FullScreenContentCallback() {
          override fun onAdDismissedFullScreenContent() {
            Log.d("ChatActivity", "Ad was dismissed.")
            // Don't forget to set the ad reference to null so you
            // don't show the ad a second time.
            interstitialAd = null
            loadAd()
          }

          override fun onAdFailedToShowFullScreenContent(adError: AdError) {
            Log.d("ChatActivity", "Ad failed to show.")
            // Don't forget to set the ad reference to null so you
            // don't show the ad a second time.
            interstitialAd = null
          }

          override fun onAdShowedFullScreenContent() {
            Log.d("ChatActivity", "Ad showed fullscreen content.")
            // Called when ad is dismissed.
          }
        }
      interstitialAd?.show(this)
    } else {
      Toast.makeText(this, "Ad wasn't loaded.", Toast.LENGTH_SHORT).show()
    }
  }
}