package com.morocco.hamssa.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

import com.morocco.hamssa.MessagesFragment;
import com.morocco.hamssa.TopicsFragment;
import com.morocco.hamssa.entities.Message;
import com.morocco.hamssa.entities.Topic;
import com.morocco.hamssa.utils.Constants;
import com.morocco.hamssa.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Created by hmontaner on 14/03/15.
 */
public class Database {
    final static String TAG = Database.class.getSimpleName();

    private static final String SQL_CREATE_TABLE_TOPICS = "CREATE TABLE topics (" +
                    BaseColumns._ID + " TEXT PRIMARY KEY," +
                    "title TEXT, description TEXT, url TEXT, linkUrl TEXT, time INTEGER, numMessages INTEGER, removed INTEGER, userId TEXT, userName TEXT, userImageUrl TEXT)";

    private static final String SQL_CREATE_TABLE_MESSAGES = "CREATE TABLE messages (" +
            BaseColumns._ID + " TEXT PRIMARY KEY," +
            "topicId TEXT, ordinal INTEGER, name TEXT, normalizedName TEXT, userId TEXT, imageUrl TEXT, text TEXT, time INTEGER, votesUp INTEGER, votesDown INTEGER, removed INTEGER)";

    private static final String SQL_CREATE_TABLE_VOTES = "CREATE TABLE votes (" +
            BaseColumns._ID + " INTEGER PRIMARY KEY," +
            "messageId TEXT, vote INTEGER)";

    private static final String SQL_CREATE_TABLE_RECEIVED_MESSAGES = "CREATE TABLE received_messages (" +
            BaseColumns._ID + " INTEGER PRIMARY KEY)";

    private static final String SQL_CREATE_TABLE_SENT_MESSAGES = "CREATE TABLE sent_messages (" +
            BaseColumns._ID + " INTEGER PRIMARY KEY)";

    private static final String SQL_CREATE_TABLE_TOPICS_EXTRAS = "CREATE TABLE topics_extras (" +
            BaseColumns._ID + " TEXT PRIMARY KEY, cursor TEXT, content TEXT)";

    private class DbHelper extends SQLiteOpenHelper {
        // If you change the database schema, you must increment the database version.
        public static final int DATABASE_VERSION = 19;

        public DbHelper(Context context) {
            super(context, "database"+ Constants.SOURCE_ID+".db", null, DATABASE_VERSION);
        }
        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(SQL_CREATE_TABLE_TOPICS);
            db.execSQL(SQL_CREATE_TABLE_MESSAGES);
            db.execSQL(SQL_CREATE_TABLE_VOTES);
            db.execSQL(SQL_CREATE_TABLE_RECEIVED_MESSAGES);
            db.execSQL(SQL_CREATE_TABLE_SENT_MESSAGES);
            db.execSQL(SQL_CREATE_TABLE_TOPICS_EXTRAS);
        }
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            onDelete(db);
            onCreate(db);
        }
        public void onDelete(SQLiteDatabase db){
            db.execSQL("DROP TABLE IF EXISTS topics");
            db.execSQL("DROP TABLE IF EXISTS messages");
            db.execSQL("DROP TABLE IF EXISTS votes");
            db.execSQL("DROP TABLE IF EXISTS received_messages");
            db.execSQL("DROP TABLE IF EXISTS sent_messages");
            db.execSQL("DROP TABLE IF EXISTS topics_extras");
        }
        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            onUpgrade(db, oldVersion, newVersion);
        }
    }

    private static DbHelper helper = null;
    private static Object sync = new Object();
    private Context context;
    public Database(Context context) {
        synchronized(sync){
            if(helper == null){
                helper = new DbHelper(context);
            }
        }
        this.context = context;
    }
    private static SQLiteDatabase database = null;
    private static int numOpens = 0;
    private SQLiteDatabase openDatabase(){
        synchronized(sync){
            if(numOpens == 0){
                database = helper.getWritableDatabase();
            }
            ++numOpens;
        }
        return database;
    }
    private synchronized void closeDatabase(){
        synchronized(sync){
            --numOpens;
            if(numOpens == 0){
                database.close();
            }
        }
    }

    static private String normalize(String string){
        if(string == null) {
            return string;
        }
        return Normalizer.normalize(string.toLowerCase(Locale.getDefault()), Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
    }

    public void clearAllTables(){
        SQLiteDatabase db = openDatabase();
        helper.onDelete(db);
        helper.onCreate(db);
        closeDatabase();
    }

    /////////////////////////////////////////////////////////////
    // TOPICS
    /////////////////////////////////////////////////////////////

    public void addTopics(JSONArray jsonArray){
        SQLiteDatabase db = openDatabase();
        try {
            db.beginTransaction();

            for (int i = 0; i < jsonArray.length(); ++i) {
                try {
                    addTopic(db, jsonArray.getJSONObject(i));
                } catch (JSONException e) {
                    Log.w(TAG, e.toString());
                }
            }
            db.setTransactionSuccessful();
        }finally{
            db.endTransaction();
        }
        closeDatabase();
    }

    public void addTopic(JSONObject jsonObject){
        SQLiteDatabase db = openDatabase();
        addTopic(db, jsonObject);
        closeDatabase();
    }
    private void addTopic(SQLiteDatabase db, JSONObject jsonObject){
        ContentValues values = new ContentValues();
        Utils.addString(values, jsonObject, "id");
        Utils.addLong(values, jsonObject, "time");
        Utils.addString(values, jsonObject, "title");
        Utils.addString(values, jsonObject, "description");
        Utils.addString(values, jsonObject, "url");
        Utils.addInt(values, jsonObject, "numMessages");
        Utils.addInt(values, jsonObject, "removed");
        Utils.addString(values, jsonObject, "linkUrl");
        Utils.addString(values, jsonObject, "userName");
        Utils.addString(values, jsonObject, "userImageUrl");
        Utils.addString(values, jsonObject, "userId");

        db.replace("topics", null, values);
    }

    private final int pastDaysLimit = 5;
    private String userId="";
    public Cursor getTopics(TopicsFragment.TYPE type) {
        SQLiteDatabase db = openDatabase();

        String query = "SELECT * FROM topics WHERE removed != 1";
       if(type == TopicsFragment.TYPE.LATEST) {
            query += " ORDER BY time DESC";
        }else if(type==TopicsFragment.TYPE.MY_POSTS){
            query+=" and userId='"+userId+"' ";
       }
        else{
            //query += " AND DATETIME(time/1000, 'unixepoch') > DATETIME('now', '-"+pastDaysLimit+" days') ORDER BY numMessages DESC";
           query +=" ORDER BY numMessages DESC";
        }
        Cursor cursor = db.rawQuery(query, null);

        return cursor;
    }

    public Topic getTopic(String topicId){
        SQLiteDatabase db = openDatabase();

        String query = "SELECT * FROM topics WHERE _id = '"+topicId+"'";
        Cursor cursor = db.rawQuery(query, null);
        Topic topic = null;
        int idIndex = cursor.getColumnIndex("_id");
        int titleIndex = cursor.getColumnIndex("title");
        int descriptionIndex = cursor.getColumnIndex("description");
        int userNameIndex = cursor.getColumnIndex("userName");
        int urlIndex = cursor.getColumnIndex("url");
        int userIdIndex = cursor.getColumnIndex("userId");
        int timeIndex = cursor.getColumnIndex("time");
        int numMessagesIndex=cursor.getColumnIndex("numMessages");
        if(cursor.moveToNext()){
            String id = cursor.getString(idIndex);
            String title = cursor.getString(titleIndex);
            String description = cursor.getString(descriptionIndex);
            String userName = cursor.getString(userNameIndex);
            String imageUrl = cursor.getString(urlIndex);
            String userId = cursor.getString(userIdIndex);
            Long time = cursor.getLong(timeIndex);
            Long numMessages=cursor.getLong(numMessagesIndex);

            topic = new Topic(id, title, description, userName, imageUrl, userId, time,numMessages);
        }

        closeDatabase();
        return topic;
    }

    /////////////////////////////////////////////////////////////
    // MESSAGES
    /////////////////////////////////////////////////////////////

    public void addMessages(JSONArray jsonArray){
        SQLiteDatabase db = openDatabase();
        try {
            db.beginTransaction();

            for (int i = 0; i < jsonArray.length(); ++i) {
                try {
                    addMessage(db, jsonArray.getJSONObject(i));
                } catch (JSONException e) {
                    Log.w(TAG, e.toString());
                }
            }
            db.setTransactionSuccessful();
        }finally{
            db.endTransaction();
        }
        closeDatabase();
    }
    public void addMessage(JSONObject jsonObject){
        SQLiteDatabase db = openDatabase();
        addMessage(db, jsonObject);
        closeDatabase();
    }
    private void addMessage(SQLiteDatabase db, JSONObject jsonObject){
        ContentValues values = new ContentValues();
        Utils.addString(values, jsonObject, "topicId");
        Utils.addString(values, jsonObject, "id");
        Utils.addLong(values, jsonObject, "time");
        Utils.addString(values, jsonObject, "name");
        Utils.addString(values, jsonObject, "normalizedName");
        Utils.addString(values, jsonObject, "imageUrl");
        Utils.addString(values, jsonObject, "text");
        Utils.addLong(values, jsonObject, "votesUp");
        Utils.addLong(values, jsonObject, "votesDown");
        Utils.addLong(values, jsonObject, "ordinal");
        Utils.addString(values, jsonObject, "userId");
        Utils.addBoolean(values, jsonObject, "removed");

        if(jsonObject.has("topicName") || jsonObject.has("topicDescription")){
            try {
                String topicId = jsonObject.getString("topicId");
                Topic topic = getTopic(topicId);
                if(topic == null){
                    JSONObject jsonTopic = new JSONObject();
                    jsonTopic.put("id", topicId);
                    if(jsonObject.has("topicName")) {
                        jsonTopic.put("title", jsonObject.getString("topicName"));
                    }
                    if(jsonObject.has("topicDescription")) {
                        jsonTopic.put("description", jsonObject.getString("topicDescription"));
                    }
                    if(jsonObject.has("topicUserName")) {
                        jsonTopic.put("userName", jsonObject.getString("topicUserName"));
                    }
                    addTopic(db, jsonTopic);
                }
            }catch(JSONException e){}
        }

        db.replace("messages", null, values);
    }

    public void addSentMessages(JSONArray jsonArray){
        addPersonalMessages(jsonArray, "sent_messages");
    }
    public void addReceivedMessages(JSONArray jsonArray){
        addPersonalMessages(jsonArray, "received_messages");
    }
    private void addPersonalMessages(JSONArray jsonArray, String tableName){
        addMessages(jsonArray);

        SQLiteDatabase db = openDatabase();
        try {
            db.beginTransaction();
            for (int i = 0; i < jsonArray.length(); ++i) {
                try {
                    addPersonalMessage(db, tableName, jsonArray.getJSONObject(i));
                } catch (JSONException e) {
                    Log.w(TAG, e.toString());
                }
            }
            db.setTransactionSuccessful();
        }finally{
            db.endTransaction();
        }
        closeDatabase();
    }

    private void addPersonalMessage(SQLiteDatabase db, String tableName, JSONObject jsonObject){
        ContentValues values = new ContentValues();
        Utils.addLong(values, jsonObject, "id");
        db.replace(tableName, null, values);
    }

    public Cursor getMessages(String topicId, MessagesFragment.TYPE type) {
        SQLiteDatabase db = openDatabase();

        String query = "";
        if(topicId != null){
            query += "SELECT M.* FROM messages M WHERE M.topicId = '"+topicId+"'";
        }else{
            query += "SELECT M.*, T.title, T.userName AS topicUserName, T.description FROM messages M LEFT JOIN topics T ON T._id = M.topicId WHERE 1 = 1";
        }
        if(type == MessagesFragment.TYPE.LATEST){
            query += " ORDER BY M.time DESC";
        }else if(type == MessagesFragment.TYPE.MOST_VOTED){
            if(topicId == null) {
                query += " AND DATETIME(M.time/1000, 'unixepoch') > DATETIME('now', '-" + pastDaysLimit + " days')";
            }
            query += " ORDER BY (M.votesUp - M.votesDown) DESC, M.time ASC";
        }
        Cursor cursor = db.rawQuery(query, null);

        return cursor;
    }

    public Cursor getReceivedMessages() {
        SQLiteDatabase db = openDatabase();

        String query = "SELECT M.*, T.title, T.userName AS topicUserName, T.description " +
                " FROM received_messages RM, messages M LEFT JOIN topics T" +
                " ON M.topicId = T._id" +
                " WHERE T._id = M.topicId AND RM._id = M._id" +
                " ORDER BY time desc";

        Cursor cursor = db.rawQuery(query, null);

        return cursor;
    }
    public Cursor getSentMessages(String userName) {
        SQLiteDatabase db = openDatabase();

        String query = "SELECT M.*, T.title, T.userName AS topicUserName, T.description ";
        if(userName != null && !userName.isEmpty()){
            userName = normalize(userName);
            query += "FROM messages M LEFT JOIN topics T ON M.topicId = T._id WHERE M.normalizedName LIKE '"+userName+"' ORDER BY M.time desc";
        }else{
            query += "FROM sent_messages SM, messages M LEFT JOIN topics T ON M.topicId = T._id WHERE SM._id = M._id ORDER BY M.time desc";
        }

        Cursor cursor = db.rawQuery(query, null);

        return cursor;
    }

    public Message getMessageByOrdinal(String topicId, Long ordinal){
        String query = "SELECT * FROM messages WHERE topicId = "+topicId+" AND ordinal = "+ordinal;
        return getMessageQuery(query);
    }

    public Message getMessage(String messageId) {
        String query = "SELECT * FROM messages WHERE _id ='"+messageId+"'";
        return getMessageQuery(query);
    }

    private Message getMessageQuery(String query){
        SQLiteDatabase db = openDatabase();
        Message message = null;

        Cursor cursor = db.rawQuery(query, null);
        if(cursor.moveToNext()){
            message = new Message(
                    cursor.getString(cursor.getColumnIndex("_id")),
                    cursor.getString(cursor.getColumnIndex("userId")),
                    cursor.getString(cursor.getColumnIndex("name")),
                    cursor.getString(cursor.getColumnIndex("text")),
                    cursor.getLong(cursor.getColumnIndex("votesUp")),
                    cursor.getLong(cursor.getColumnIndex("votesDown")),
                    cursor.getLong(cursor.getColumnIndex("time")),
                    cursor.getLong(cursor.getColumnIndex("ordinal")),
                    cursor.getString(cursor.getColumnIndex("imageUrl")),
                    cursor.getString(cursor.getColumnIndex("topicId")));
        }

        closeDatabase();
        return message;
    }

    /////////////////////////////////////////////////////////////
    // VOTES
    /////////////////////////////////////////////////////////////
    public int addVote(String messageId, boolean up){
        SQLiteDatabase db = openDatabase();
        int result = -1;
        try {
            db.beginTransaction();
            Cursor cursor=db.rawQuery("SELECT * FROM votes WHERE messageId ='"+messageId+"'", null);
            if(cursor.moveToNext()  && cursor.getInt(cursor.getColumnIndex("vote"))*(up?1:-1)<0){
             removeVote(messageId,!up);
             result=-2;
            }
            if(!cursor.moveToFirst() || result==-2 ) {
                ContentValues values = new ContentValues();
                values.put("messageId", messageId);
                values.put("vote", up ? 1 : -1);
                db.insert("votes", null, values);

                String field = up ? "votesUp" : "votesDown";
                db.execSQL("UPDATE messages SET "+field+" = "+field+" + 1 WHERE _id = '"+messageId+"'");

                db.setTransactionSuccessful();
                result=1;
            }
        }finally{
            db.endTransaction();
        }

        closeDatabase();
        return result;
    }

    public void removeVote(String messageId, boolean up){
        SQLiteDatabase db = openDatabase();

        try {
            db.beginTransaction();

            if(db.delete("votes", "messageId = '"+messageId+"'", null) == 1){
                String field = up ? "votesUp" : "votesDown";
                db.execSQL("UPDATE messages SET "+field+" = "+field+" - 1 WHERE _id = '"+messageId+"'");
            }

            db.setTransactionSuccessful();
        }finally{
            db.endTransaction();
        }

        closeDatabase();
    }

    /////////////////////////////////////////////////////////////
    // TOPIC EXTRAS
    /////////////////////////////////////////////////////////////
    public void setTopicCursor(String topicId, String content){
        setTopicExtra(topicId, "cursor", content);
    }
    public String getTopicCursor(String topicId){
        return getTopicExtra(topicId, "cursor");
    }
    public void setTopicContent(String topicId, String content){
        setTopicExtra(topicId, "content", content);
    }
    public String getTopicContent(String topicId){
        return getTopicExtra(topicId, "content");
    }
    public void setTopicExtra(String topicId, String key, String value){
        SQLiteDatabase db = openDatabase();

        ContentValues values = new ContentValues();
        values.put(key, value);

        int rows = db.update("topics_extras", values, "_id = "+topicId, null);

        if(rows == 0 && value != null){
            values.put("_id", topicId);
            db.insert("topics_extras", null, values);
        }

        closeDatabase();
    }

    public String getTopicExtra(String topicId, String key){
        SQLiteDatabase db = openDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM topics_extras WHERE _id = '" + topicId+"'", null);

        String value = null;
        if(cursor.moveToNext()){
            value = cursor.getString(cursor.getColumnIndex(key));
        }

        closeDatabase();
        return value;
    }

    public void removeTopic(String topicId){
        SQLiteDatabase db = openDatabase();

        try {
            db.beginTransaction();
            Cursor cursor = db.rawQuery("SELECT * FROM messages WHERE topicId = '" + topicId+"'", null);

            String value = null;
            if(cursor.moveToNext()){
                value = cursor.getString(cursor.getColumnIndex("_id"));
                db.delete("votes", "messageId = '"+value+"'", null);
                db.delete("messages", "_id = '"+value+"'", null);
            }


            db.delete("topics", "topicId = '"+topicId+"'", null);

            db.setTransactionSuccessful();
        }finally{
            db.endTransaction();
        }

        closeDatabase();
    };
    public void setUserId(String userId) {
        this.userId = userId;
    }
}
