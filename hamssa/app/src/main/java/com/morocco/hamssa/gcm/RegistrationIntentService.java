package com.morocco.hamssa.gcm;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.gcm.GcmPubSub;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.morocco.hamssa.utils.Constants;
import com.morocco.hamssa.utils.HTTPTask;
import com.morocco.hamssa.utils.NameValuePair;
import com.morocco.hamssa.utils.Utils;

import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

/**
 * Created by hmontaner on 11/06/15.
 */
public class RegistrationIntentService extends IntentService {

    private static final String TAG = RegistrationIntentService.class.getSimpleName();
    private static final String[] TOPICS = {"global"};
    private final String SENDER_ID = "607996762750";

    public RegistrationIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        try {
            synchronized (TAG) {
                // Initially this call goes out to the network to retrieve the token, subsequent calls are local.
                InstanceID instanceID = InstanceID.getInstance(this);
                String token = instanceID.getToken(SENDER_ID, GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);

                Log.i(TAG, "GCM Registration Token: " + token);

                String oldToken = prefs.getString(Constants.SP.GCM_REGISTRATION_ID, "");
                if(!oldToken.equalsIgnoreCase(token)){
                    sendRegistrationToServer(getApplicationContext(), prefs, token, oldToken);
                    subscribeTopics(token);
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "Failed to complete token refresh", e);
        }
    }


    private void sendRegistrationToServer(Context context, final SharedPreferences prefs, final String token, String oldToken){
        Log.i(TAG, "Sending GCM token to server");
        List<NameValuePair> params = HTTPTask.getParams(context);
        params.add(new NameValuePair("notification_token", token));
        params.add(new NameValuePair("notification_platform", "android"));
        if(oldToken != null && !oldToken.isEmpty()){
            params.add(new NameValuePair("old_notification_token", oldToken));
        }
        new HTTPTask(context, Constants.TASK.ADD_NOTIFICATION_TOKEN, new HTTPTask.Callback() {
            @Override
            public void onDataReceived(JSONObject jsonObject) {
                if(Utils.isSuccess(jsonObject)){
                    prefs.edit().putString(Constants.SP.GCM_REGISTRATION_ID, token).apply();
                }
            }
        }).executeParallel(params);
    }

    private void subscribeTopics(String token) throws IOException {
        for (String topic : TOPICS) {
            GcmPubSub pubSub = GcmPubSub.getInstance(this);
            pubSub.subscribe(token, "/topics/" + topic, null);
        }
    }
}
