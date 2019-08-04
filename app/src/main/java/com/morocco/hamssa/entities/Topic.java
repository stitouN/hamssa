package com.morocco.hamssa.entities;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.widget.Toast;


import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;
import com.google.gson.Gson;
import com.morocco.hamssa.R;
import com.morocco.hamssa.data.Database;
import com.morocco.hamssa.utils.Constants;
import com.morocco.hamssa.utils.HTTPTask;
import com.morocco.hamssa.utils.NameValuePair;
import com.morocco.hamssa.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by hmontaner on 10/09/15.
 */
public class Topic {
    String id;
    String title;
    String description;
    String userName;
    String imageUrl;
    //String audioUrl;
    String userId;
    Long time;
    Long numMessages;
    public Topic(String id, String title, String description, String userName, String imageUrl, String userId, Long time,Long numMessages){
        this.id = id;
        this.title = title;
        this.description = description;
        this.userName = userName;
        this.imageUrl = imageUrl;
        //this.audioUrl = audioUrl;
        this.userId = userId;
        this.time = time;
        this.numMessages=numMessages;
    }
    public String getId(){
        return id;
    }
    public String getTitle(){
        return title;
    }
    public String getDescription() { return description; }
    public String getUserName() { return userName; }
    public Long getTime(){ return time; }
    public String getContent(Context context) {
        return new Database(context).getTopicContent(id);
    }
    public String getImageUrl(){ return imageUrl; }
    //public String getAudioUrl(){ return audioUrl;}
    public boolean isPersonal(){
        return userName != null && !userName.isEmpty();
    }
    public Long getNumMessages(){return numMessages;}

    public interface OnContentListener{
        void onContent(boolean success, String content);
    }
    public void fetchContent(final Context context, final OnContentListener listener){
         //TODO récupération du topic par son ID
        // fetchContent(context, id, listener);
    }

    public static void fetchContent(final Context context, final String topicId, final OnContentListener listener){
        FirebaseFunctions functions = FirebaseFunctions.getInstance();
        List<NameValuePair> params = HTTPTask.getParams(context);
        Constants.TASK task = Constants.TASK.GET_TOPIC;
        Map<String, Object> data = new HashMap<>();
        data.put("token", params.get(1).getValue());
        data.put("topicId",topicId);
        functions.getHttpsCallable(task.toString()).call(data).addOnCompleteListener(new OnCompleteListener<HttpsCallableResult>() {
            @Override
            public void onComplete(@NonNull Task<HttpsCallableResult> task) {
                Gson gson = new Gson();
                if(task.isSuccessful()) {
                    String result = gson.toJson(task.getResult().getData());
                    Cursor cursor = null;
                    try {
                        JSONObject jsonObject = new JSONObject(result);
                        if (jsonObject != null) {
                            JSONArray jsonArray = new JSONArray();
                            jsonArray.put(jsonObject);
                            Database db = new Database(context);
                            db.addTopics(jsonArray);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }else{
                    String message = "connection error";
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                }
            }

    });
      /*  new HTTPTask(context, task, new HTTPTask.Callback() {
            boolean success = false;
            String content;
            @Override
            public void parseResponse(JSONObject jsonObject){
                if(Utils.isSuccess(jsonObject)){
                    try {
                        success = true;
                        JSONObject topicJsonObject = jsonObject.getJSONObject("topic");
                        Database db = new Database(context);
                        db.addTopic(topicJsonObject);
                        content = topicJsonObject.getString("content");
                        db.setTopicContent(topicId, content);
                    }catch(JSONException e){}
                }
            }
            @Override
            public void onDataReceived(JSONObject jsonObject) {
                if(listener != null){
                    listener.onContent(success, content);
                }
            }
        }).executeParallel(params);*/
    }

    public boolean isMine(Context context){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getString(Constants.SP.USER_ID, "").equals(userId);
    }

}
