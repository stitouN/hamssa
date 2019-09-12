package com.morocco.hamssa.utils;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;



import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.StringBody;
//import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.firebase.functions.FirebaseFunctions;

public class HTTPTask extends AsyncTask<List<NameValuePair>, String, JSONObject>{
    static final String TAG = HTTPTask.class.getSimpleName();
    static public abstract class Callback{
        // Executed in background thread
        public void parseResponse(JSONObject jsonObject){}
        // Executed in UI thread
        public void onDataReceived(JSONObject jsonObject){};
    }

    private String task;
    private String url;
    private Callback callback;
    private Context context = null;
    private Bitmap bitmap;

    public HTTPTask(Context context, Constants.TASK task, Callback callback){
        this.task = task.toString();
        this.url = Constants.BACKEND+task;
        this.callback = callback;
        this.context = context;
    }

    public HTTPTask(Context context, String url, Bitmap bitmap, Callback callback){
        this.task = url;
        this.url = url;
        this.bitmap = bitmap;
        this.callback = callback;
        this.context = context;
    }

    @Override
    protected JSONObject doInBackground(List<NameValuePair>... params) {
        Log.v("HTTPTask", "task: "+task);
        if(bitmap == null){
            return send(params);
        }else{
            return sendWithBitmap(params);
        }
    }

    private JSONObject send(List<NameValuePair>... params){
        final JSONObject jsonObject = null;
        FirebaseFunctions functions = FirebaseFunctions.getInstance();


        InputStream stream = null;
        try {
            Map<String, Object> data = encodeURLParams(params[0]);
           // Map<String, Object> data=new HashMap<>();
            //data.put("name","najlaa");


//            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//            //connection.setReadTimeout(10000);
//            //connection.setConnectTimeout(15000);
//            connection.setRequestMethod("GET");
//            connection.setDoInput(true);
//            connection.connect();
//            stream = connection.getInputStream();
//            String response = readStream(stream);
            //jsonObject = new JSONObject(response);
        }
//        catch(JSONException e){
//            Log.w(TAG, e.toString());
//        }
        finally{
            if(stream != null){
                try {
                    stream.close();
                }catch(IOException e){}
            }
        }
        callback.parseResponse(jsonObject);
        return jsonObject;
    }

    private JSONObject sendWithBitmap(List<NameValuePair>... params){
        JSONObject jsonObject = null;

        // TODO: Use HttpURLConnection, but it has no support for images
        HttpClient httpclient = new DefaultHttpClient(/*my_httpParams*/);
        try {
            HttpRequestBase httpRequest = null;

            HttpPost httpPost = new HttpPost(url);

            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
            byte[] data = outputStream.toByteArray();
            builder.addPart("image", new ByteArrayBody(data, "image.jpg"));

            for(NameValuePair pair : params[0]){
                builder.addPart(pair.getName(), new StringBody(pair.getValue(), ContentType.TEXT_PLAIN));
            }

            httpPost.setEntity(builder.build());
            httpRequest = httpPost;

            HttpResponse response = httpclient.execute(httpRequest);

            try {
                InputStream stream = response.getEntity().getContent();
                String responseString = readStream(stream);
                jsonObject = new JSONObject(responseString);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        callback.parseResponse(jsonObject);
        return jsonObject;
    }

    private         Map<String, Object>  encodeURLParams(List<NameValuePair> params){
        String ret = "";
        Map<String, Object> data = new HashMap<>();
        for(NameValuePair pair : params){
            if(pair.getName() != null && pair.getValue() != null){
                try {
                    String name = URLEncoder.encode(pair.getName(), "utf-8");
                    String value = URLEncoder.encode(pair.getValue(), "utf-8");
                    data.put(name,value);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        }
        return data;
    }

    public String readStream(InputStream stream) throws IOException, UnsupportedEncodingException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
        StringBuilder builder = new StringBuilder();
        for(String line; (line = reader.readLine()) != null;) {
            builder.append(line).append("\n");
        }
        return builder.toString();
    }

    @Override
    protected void onPostExecute(JSONObject jsonObject){
        callback.onDataReceived(jsonObject);
    }

    @Override
    protected void onCancelled(){}

    public static List<NameValuePair> getParams(Context context){
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);

        params.add(new NameValuePair("user_id", sp.getString(Constants.SP.USER_ID,"")));
        params.add(new NameValuePair("token", sp.getString(Constants.SP.USER_TOKEN, "")));
        return params;
    }

    public void executeParallel(List<NameValuePair> params){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB){
            executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
        }else{
            execute(params);
        }
    }
}