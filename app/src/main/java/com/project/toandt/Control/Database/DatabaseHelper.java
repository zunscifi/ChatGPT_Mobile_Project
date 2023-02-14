package com.project.toandt.Control.Database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.project.toandt.Model.Message;
import com.project.toandt.Model.MessageManager;

public class DatabaseHelper extends SQLiteOpenHelper {
  public static final String SENDER_SEVER = "ChatGPT";

  public static final String SENDER_CLIENT = "User";
  private static final int DATABASE_VERSION = 1;
  private static final String DATABASE_NAME = "ChatGPTMobileSF.db";
  private static final String TABLE_CONVERSATIONS = "conversations";
  private static final String COLUMN_CONVERSATION_ID = "id";
  private static final String COLUMN_CONVERSATION_NAME = "name";

  private static final String TABLE_MESSAGES = "messages";
  private static final String COLUMN_MESSAGE_ID = "id";
  private static final String COLUMN_MESSAGE_CONVERSATION_ID = "conversation_id";
  private static final String COLUMN_MESSAGE_SENDER = "sender";
  private static final String COLUMN_MESSAGE_MESSAGE = "message";
  private static final String COLUMN_MESSAGE_TIMESTAMP = "timestamp";

  public DatabaseHelper(Context context) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    String createTableConversations = "CREATE TABLE " + TABLE_CONVERSATIONS + "("
      + COLUMN_CONVERSATION_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
      + COLUMN_CONVERSATION_NAME + " TEXT NOT NULL" + ")";
    db.execSQL(createTableConversations);

    String createTableMessages = "CREATE TABLE " + TABLE_MESSAGES + "("
      + COLUMN_MESSAGE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
      + COLUMN_MESSAGE_CONVERSATION_ID + " INTEGER NOT NULL, "
      + COLUMN_MESSAGE_SENDER + " TEXT NOT NULL, "
      + COLUMN_MESSAGE_MESSAGE + " TEXT NOT NULL, "
      + COLUMN_MESSAGE_TIMESTAMP + " TEXT NOT NULL, "
      + "FOREIGN KEY (" + COLUMN_MESSAGE_CONVERSATION_ID + ") REFERENCES "
      + TABLE_CONVERSATIONS + "(" + COLUMN_CONVERSATION_ID + ")" + ")";
    db.execSQL(createTableMessages);
    Log.i(DATABASE_NAME, "OnCreate Completed!");
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    db.execSQL("DROP TABLE IF EXISTS " + TABLE_CONVERSATIONS);
    db.execSQL("DROP TABLE IF EXISTS " + TABLE_MESSAGES);
    onCreate(db);
  }

  public int addConversation(String name) {
    SQLiteDatabase db = this.getWritableDatabase();
    ContentValues values = new ContentValues();
    values.put(COLUMN_CONVERSATION_NAME, name);
    long result = db.insert(TABLE_CONVERSATIONS, null, values);
    db.close();
    return (int) result;
  }

  public int addMessage(int conversationId, String sender, String message, String timestamp) {
    SQLiteDatabase db = this.getWritableDatabase();
    ContentValues values = new ContentValues();
    values.put(COLUMN_MESSAGE_CONVERSATION_ID, conversationId);
    values.put(COLUMN_MESSAGE_SENDER, sender);
    values.put(COLUMN_MESSAGE_MESSAGE, message);
    values.put(COLUMN_MESSAGE_TIMESTAMP, timestamp);
    long result = db.insert(TABLE_MESSAGES, null, values);
    db.close();
    return (int) result;
  }

  public Cursor getAllConversations() {
    SQLiteDatabase db = this.getReadableDatabase();
    Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_CONVERSATIONS, null);
    return cursor;
  }

  public Cursor getAllMessages(int conversationId) {
    SQLiteDatabase db = this.getReadableDatabase();
    Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_MESSAGES
      + " WHERE " + COLUMN_MESSAGE_CONVERSATION_ID + " = " + conversationId, null);
    return cursor;
  }

  public Message getMessage(int messageId, int conversationId){
    Cursor cursor = getAllMessages(conversationId);
    Message message = null;
    while (cursor.moveToNext()){
      if(cursor.getInt(0) == messageId){
        message = new Message(
          cursor.getLong(0),
          cursor.getInt(1),
          cursor.getString(2),
          cursor.getString(3),
          cursor.getLong(4)
          );
      }
    }
    return message;
  }
  public MessageManager getMessageManager(int conversationId){
    MessageManager messageManager = new MessageManager();
    Cursor cursor = getAllMessages(conversationId);
    while (cursor.moveToNext()){
      Message message = new Message(
          cursor.getLong(0),
          cursor.getInt(1),
          cursor.getString(2),
          cursor.getString(3),
          cursor.getLong(4)
        );
      messageManager.addMessage(message);
    }
    return messageManager;
  }

  public boolean updateConversation(int id, String name) {
    SQLiteDatabase db = this.getWritableDatabase();
    ContentValues values = new ContentValues();
    values.put(COLUMN_CONVERSATION_NAME, name);
    int result = db.update(TABLE_CONVERSATIONS, values,
      COLUMN_CONVERSATION_ID + " = " + id, null);
    db.close();
    return result > 0;
  }

  public boolean deleteConversation(int id) {
    SQLiteDatabase db = this.getWritableDatabase();
    int result = db.delete(TABLE_CONVERSATIONS,
      COLUMN_CONVERSATION_ID + " = " + id, null);
    db.close();
    return result > 0;
  }
}
