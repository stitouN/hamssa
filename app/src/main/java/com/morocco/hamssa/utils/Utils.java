package com.morocco.hamssa.utils;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.provider.BaseColumns;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.morocco.hamssa.R;
import com.morocco.hamssa.adapters.MessageCursorRecyclerViewAdapter;
import com.morocco.hamssa.data.Database;
import com.morocco.hamssa.entities.Message;
import com.morocco.hamssa.entities.Topic;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by hmontaner on 01/08/15.
 */
public class Utils {
    static final String TAG = Utils.class.getSimpleName();
    static public boolean isSuccess(JSONObject jsonObject) {
        boolean ret = false;
        try {
            if (jsonObject != null && jsonObject.has("result")) {
                String result = jsonObject.getString("result");
                if (result != null && result.equalsIgnoreCase("1")) {
                    ret = true;
                }
            }
        } catch (JSONException e) {
        }
        return ret;
    }

    static public void addString(ContentValues values, JSONObject jsonObject, String key) {
        if (jsonObject.has(key)) {
            try {
                String value = jsonObject.getString(key);
                String valuesKey = key.equalsIgnoreCase("id") ? BaseColumns._ID : key;
                values.put(valuesKey, value);
            } catch (JSONException e) {

            }
        }
    }
    static public void addInt(ContentValues values, JSONObject jsonObject, String key) {
        if (jsonObject.has(key)) {
            try {
                int value = jsonObject.getInt(key);
                String valuesKey = key.equalsIgnoreCase("id") ? BaseColumns._ID : key;
                values.put(valuesKey, value);
            } catch (JSONException e) {

            }
        }
    }

    static public void addLong(ContentValues values, JSONObject jsonObject, String key) {
        if (jsonObject.has(key)) {
            try {
                Long value = jsonObject.getLong(key);
                String valuesKey = key.equalsIgnoreCase("id") ? BaseColumns._ID : key;
                values.put(valuesKey, value);
            } catch (JSONException e) {

            }
        }
    }

    static public void addBoolean(ContentValues values, JSONObject jsonObject, String key) {
        if (jsonObject.has(key)) {
            try {
                boolean value = jsonObject.getBoolean(key);
                values.put(key, value);
            } catch (JSONException e) {

            }
        }
    }


    static private final String sharedPreferencesName = "login";
    static private final String spUserId = "userId";
    static private final String spUserToken = "tokenId";
    static private final String spUserName = "userName";




    static public String msToElapsedTime(Context context, long ms) {
        double time = (new Date().getTime() - ms) / (1000 * 60);
        if(time < 1){
            return context.getString(R.string.a_moment_ago);
        }
        if(time < 60){
            long units = Math.round(Math.ceil(time));
            int resId = units == 1 ? R.string.minute_ago : R.string.minutes_ago;
            return context.getString(resId, units);
        }
        time /= 60;
        if(time < 24){
            long units = Math.round(Math.ceil(time));
            int resId = units == 1 ? R.string.hour_ago : R.string.hours_ago;
            return context.getString(resId, units);
        }
        time /= 24;
        if (time < 30) {
            long units = Math.round(Math.ceil(time));
            int resId = units == 1 ? R.string.day_ago : R.string.days_ago;
            return context.getString(resId, units);
        } else {
            time /= 30;
            if (time < 12) {
                long units = Math.round(Math.ceil(time));
                int resId = units == 1 ? R.string.month_ago : R.string.months_ago;
                return context.getString(resId, units);
            } else {
                time /= 12;
                long units = Math.round(Math.ceil(time));
                int resId = units == 1 ? R.string.year_ago : R.string.years_ago;
                return context.getString(resId, units);
            }
        }
    }

    static public Bitmap processActivityResult(Context context, Intent imageReturnedIntent, int maxSize){
        Uri selectedImage = imageReturnedIntent.getData();
        return getBitmapFromUri(context, selectedImage, maxSize);
    }
    static public Bitmap getBitmapFromUri(Context context, Uri uri, int maxSize){
        Bitmap bitmap = null;
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(context.getContentResolver().openInputStream(uri), null, o);
            int scaleFactor = Math.max(o.outWidth/maxSize, o.outHeight/maxSize);

            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scaleFactor;
            bitmap = BitmapFactory.decodeStream(context.getContentResolver().openInputStream(uri), null, o2);

            bitmap = ImageUtils.correctImageOrientation(context, bitmap, uri);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    static public Pattern patternMessageReference = Pattern.compile("#([0-9]+)");
    static public List<Long> parseMessageReferences(String text){
        List<Long> list = new ArrayList<>();
        Matcher matcher = patternMessageReference.matcher(text);
        while(matcher.find()){
            try {
                list.add(Long.parseLong(matcher.group(1)));
            }catch(NumberFormatException e){}
        }
        return list;
    }

    static public Pattern patternUserReference = Pattern.compile("@([a-zA-Z0-9_ñÑçÇáéíóúÁÉÍÓÚàÀèÈòÒüÜ]+)");
    static public List<String> parseUserReferences(String text){
        List<String> list = new ArrayList<>();
        Matcher matcher = patternUserReference.matcher(text);
        while(matcher.find()){
            list.add(matcher.group(1));
        }
        return list;
    }

    static public AlertDialog showMessageDialog(Context context, Message message, String topicId, String messageId, MessageCursorRecyclerViewAdapter adapter){
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);

        if(message != null) {
            View view = LayoutInflater.from(context).inflate(R.layout.message_list_item, null);
            // TODO: This ViewHolder method complicates the adapter too much
            MessageCursorRecyclerViewAdapter.ViewHolder viewHolder = adapter.new ViewHolder(view, messageId);
            MessageCursorRecyclerViewAdapter.fillMessage(adapter, viewHolder, message.getText()
                    , message.getOrdinal(), message.getName(), message.getVotesUp()
                    , message.getVotesDown(), message.getTime(), message.getImageURL(), topicId, null, null);
            dialogBuilder.setView(view);
        }else{
            View view = LayoutInflater.from(context).inflate(R.layout.waiting_message_list_item, null);
            dialogBuilder.setView(view);
        }

        AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.show();
        return alertDialog;
    }

    static public AlertDialog showMessageReplyDialog(final Context context, final Message message){
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);

        View view = LayoutInflater.from(context).inflate(R.layout.message_reply_dialog, null);
        dialogBuilder.setView(view);

        final AlertDialog alertDialog = dialogBuilder.create();

        final EditText editText = (EditText)view.findViewById(R.id.editText);
        final String text = "#"+message.getOrdinal()+" ";
        editText.setText(text);
        editText.post(new Runnable() {
            @Override
            public void run() {
                editText.setSelection(text.length());
                InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
            }
        });

        view.findViewById(R.id.send).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = editText.getText().toString();
                send(context, message.getTopicId(), text);
                alertDialog.dismiss();
            }
        });


        alertDialog.show();
        return alertDialog;
    }

    // TODO: Code duplicated from TopicActivity:setupSend()

    static private void send(final Context context, String topicId, String text){
        List<NameValuePair> params = HTTPTask.getParams(context);
        params.add(new NameValuePair("topic_id", topicId));
        params.add(new NameValuePair("text", text));
        List<Long> messageOrdinals = Utils.parseMessageReferences(text);
        Database db = new Database(context);
        for(Long messageOrdinal : messageOrdinals){
            Message message = db.getMessageByOrdinal(topicId, messageOrdinal);
            if(message != null) {
                params.add(new NameValuePair("target_user_ids", message.getUserId()));
            }
        }
        List<String> userNames = Utils.parseUserReferences(text);
        for(String name : userNames){
            params.add(new NameValuePair("target_user_names", name));
        }
        Constants.TASK task = Constants.TASK.CREATE_MESSAGE;

        final ProgressDialog progressDialog = ProgressDialog.show(context, context.getString(R.string.please_wait), context.getString(R.string.connecting_with_server), true);
        new HTTPTask(context, task, new HTTPTask.Callback() {
            boolean success = false;
            @Override
            public void parseResponse(JSONObject jsonObject){
                if(Utils.isSuccess(jsonObject)){
                    success = true;
                }
            }
            @Override
            public void onDataReceived(JSONObject jsonObject) {
                progressDialog.dismiss();
                String message = context.getString(R.string.connection_error);
                if(success){
                    message = context.getString(R.string.message_sent);
                }
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            }
        }).executeParallel(params);
    }

    static public boolean checkPlayServices(Activity activity) {
        final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(activity);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, activity,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Toast.makeText(activity, "GCM: This device is not supported", Toast.LENGTH_LONG).show();
                activity.finish();
            }
            return false;
        }
        return true;
    }

    public static  void constructTopicsJsonArray(JSONObject jsonObject, JSONArray jsonArray, JSONArray keys) throws JSONException {
        for(int i=0;i<keys.length();i++){
            JSONObject topicResult=jsonObject.getJSONObject(keys.get(i).toString());
                    JSONObject topic=new JSONObject();
                    topic.put("userId",topicResult.getString("userId"));
                    topic.put("userName",topicResult.getString("userName"));
                    topic.put("id",keys.get(i));
                    topic.put("title",topicResult.getString("title"));
                    topic.put("description",topicResult.getString("description"));
                    topic.put("url",topicResult.getString("imageUrl"));
                    topic.put("audioUrl",topicResult.getString("audioUrl"));
                    topic.put("order",topicResult.getLong("order"));
                    topic.put("numMessages",topicResult.getLong("numMessages"));
                    topic.put("removed",topicResult.getLong("removed"));
                    jsonArray.put(topic);


        }
    }

    public static void constructMessagesJsonArray(JSONObject jsonObject, JSONArray jsonArray, JSONArray keys,String topicId) throws JSONException {
        for(int i=0;i<keys.length();i++){
            JSONObject message = new JSONObject();
            JSONObject messageResult=jsonObject.getJSONObject(keys.get(i).toString());
            message.put("id",keys.get(i).toString());
            message.put("userId",messageResult.getString("normalizedName"));
            message.put("topicId",topicId);
            message.put("imageUrl","");
            message.put("name",messageResult.getString("name"));
            message.put("normalizedName",messageResult.getString("normalizedName"));
            message.put("ordinal",messageResult.getLong("order"));
            message.put("removed",messageResult.get("removed"));
            message.put("text",messageResult.getString("text"));
            message.put("time",messageResult.getLong("time"));
            message.put("votesDown",messageResult.getLong("votesDown"));
            message.put("votesUp",messageResult.get("votesUp"));

            jsonArray.put(message);
        }
    }

}
