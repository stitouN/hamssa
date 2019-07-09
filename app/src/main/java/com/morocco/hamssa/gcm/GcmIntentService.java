package com.morocco.hamssa.gcm;


import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.InboxStyle;
import android.support.v4.app.TaskStackBuilder;
import android.text.Html;
import android.text.SpannableString;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.gcm.GcmListenerService;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.morocco.hamssa.LoginActivity;
import com.morocco.hamssa.MainActivity;
import com.morocco.hamssa.MessagesFragment;
import com.morocco.hamssa.MyMessagesActivity;
import com.morocco.hamssa.R;
import com.morocco.hamssa.TopicDetailActivity;
import com.morocco.hamssa.data.Database;
import com.morocco.hamssa.entities.Message;
import com.morocco.hamssa.entities.Topic;
import com.morocco.hamssa.utils.Constants;
import com.morocco.hamssa.utils.HTTPTask;
import com.morocco.hamssa.utils.NameValuePair;
import com.morocco.hamssa.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * This {@code IntentService} does the actual handling of the GCM message.
 * {@code GcmBroadcastReceiver} (a {@code WakefulBroadcastReceiver}) holds a
 * partial wake lock for this service while the service does its work. When the
 * service is finished, it calls {@code completeWakefulIntent()} to release the
 * wake lock.
 */
public class GcmIntentService extends GcmListenerService {
    static final String TAG = GcmIntentService.class.getSimpleName();
    static private NotificationManager notificationManager = null;

    @Override
    public void onMessageReceived(String from, Bundle data) {
        String message = data.getString("message");
        Log.d(TAG, "Message received from: " + from);

        if(MainActivity.isLogged(this)) {
            manageNotification(data);
        }
    }
    
    private static List<SpannableString> unreadMessages = new ArrayList<>();

    public enum TYPE{
        MESSAGE, VOTES, TOPIC;
        public static TYPE getType(String ordinal){
            if(ordinal == null){
                return null;
            }
            Integer ordinalInt = null;
            try {
                ordinalInt = Integer.parseInt(ordinal);
            }catch(NumberFormatException e){
                return null;
            }
            if(ordinalInt == 0){
                return MESSAGE;
            }else if(ordinalInt == 1){
                return VOTES;
            }else if(ordinalInt == 2){
                return TOPIC;
            }
            return null;

        }
    }

	public void manageNotification(Bundle extras) {
		String action = extras.getString("action", "-1");
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        boolean notifyFollowing = sp.getBoolean(Constants.SP.NOTIFY_FOLLOWING, true);
        boolean notifyReferences = sp.getBoolean(Constants.SP.NOTIFY_REFERENCES, true);
        boolean notifyVotes = sp.getBoolean(Constants.SP.NOTIFY_VOTES, true);

		if (action.equalsIgnoreCase("0")) {
            if(!notifyReferences){
                return;
            }
            manageMessage(extras);
		}else if(action.equalsIgnoreCase("1")){
            if(!notifyVotes){
                return;
            }
            Long numVotes = 0L;
            try {
                numVotes = Long.parseLong(extras.getString("num_votes", ""));
            }catch(NumberFormatException e){}
            String text = extras.getString("text");
            SpannableString notificationMessage = new SpannableString(text);
            String title = getApplicationContext().getString(R.string.you_received_likes, numVotes);
            Long messageId = 0L;
            try{
                messageId = Long.parseLong(extras.getString("message_id", ""));
            }catch(NumberFormatException e){}
           // showNotification(title, notificationMessage, title, TYPE.VOTES, messageId);
		}else if(action.equalsIgnoreCase("2")){
            if(!notifyFollowing){
                return;
            }
            manageNewTopicCreated(extras);
        }
	}

    private void manageMessage(Bundle extras){
        String messageId;
        try{
            messageId = extras.getString("message_id", "");
        }catch(NumberFormatException e){
            return;
        }
        final String messageIdFinal = messageId;
        final Context context = this;
        Message message = new Database(this).getMessage(messageId);
        if(message != null){
            manageMessage(message);
        }else{
            List<NameValuePair> params = HTTPTask.getParams(this);
            params.add(new NameValuePair("message_id", messageId));
            new HTTPTask(this, Constants.TASK.GET_MESSAGE, new HTTPTask.Callback() {
                Message message = null;
                @Override
                public void parseResponse(JSONObject jsonObject){
                    if(Utils.isSuccess(jsonObject)){
                        try {
                            if(jsonObject.has("message")) {
                                Database db = new Database(context);
                                db.addMessage(jsonObject.getJSONObject("message"));
                                message = db.getMessage(messageIdFinal);
                            }
                        }catch(JSONException e){}
                    }
                }
                @Override
                public void onDataReceived(JSONObject jsonObject) {
                    if(message != null){
                        manageMessage(message);
                    }
                }
            }).executeParallel(params);
        }
    }
    private void manageMessage(Message message){
        String name = message.getName();
        String text = message.getText();
        SpannableString notificationMessage = new SpannableString(Html.fromHtml("<strong>@"+name+":</strong> "+text));
        String bigContentTitle = getApplicationContext().getString(R.string.you_have_been_mentioned);
        if(unreadMessages.size() > 1){
            bigContentTitle = getApplicationContext().getString(R.string.you_have_been_mentioned_times, unreadMessages.size());
        }
       // showNotification(bigContentTitle, notificationMessage, bigContentTitle, TYPE.MESSAGE, message.getId());
    }

    private void manageNewTopicCreated(Bundle extras){
        String name = extras.getString("name");
        String text = extras.getString("text");
        final SpannableString notificationMessage = new SpannableString(Html.fromHtml("<strong>@"+name+":</strong> "+text));
        final String bigContentTitle = getApplicationContext().getString(R.string.new_content);
        final String topicId;
        try{
            topicId = extras.getString("topic_id", null);
        }catch(NumberFormatException e){
            return;
        }
        Topic topic = new Database(this).getTopic(topicId);
        if(topic != null) {
            showNotification(bigContentTitle, notificationMessage, bigContentTitle, TYPE.TOPIC, topicId);
        }else{
            Topic.fetchContent(this, topicId, new Topic.OnContentListener() {
                @Override
                public void onContent(boolean success, String content) {
                    if(success) {
                        showNotification(bigContentTitle, notificationMessage, bigContentTitle, TYPE.TOPIC, topicId);
                    }
                }
            });
        }
    }
    
    private void showNotification(String title, SpannableString text, String bigContentTitle, TYPE type, String itemId){

    	Intent intent;
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        if(type == TYPE.TOPIC){
            intent = new Intent(this, TopicDetailActivity.class);
            intent.putExtra(TopicDetailActivity.ARG_TOPIC_ID, itemId);
            stackBuilder.addParentStack(TopicDetailActivity.class);
        }else {
            intent = new Intent(this, MyMessagesActivity.class);
            MessagesFragment.TYPE typeArg;
            if(type == TYPE.VOTES){
                typeArg = MessagesFragment.TYPE.SENT;
            }else{
                typeArg = MessagesFragment.TYPE.RECEIVED;
            }
            intent.putExtra(MyMessagesActivity.ARG_TYPE, typeArg.ordinal());
            stackBuilder.addParentStack(MyMessagesActivity.class);
        }
    	
    	// Adds the Intent to the top of the stack
    	stackBuilder.addNextIntent(intent);
    	PendingIntent contentIntent = stackBuilder.getPendingIntent(new Random().nextInt(), PendingIntent.FLAG_UPDATE_CURRENT);

    	String tickerMessage = title;

    	final NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
    	mBuilder.setSmallIcon(R.drawable.ic_notification)
    		.setTicker(tickerMessage)
	    	.setAutoCancel(true)
	    	.setLights(0xffF44336, 500, 500)
            .setContentTitle(title)
            .setContentText(text);

        int color = getResources().getColor(R.color.main);
        mBuilder.setColor(color);

    	if(true){
    		Uri soundUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.bell);
    		mBuilder.setSound(soundUri);
    	}

        if(type == TYPE.MESSAGE) {
            synchronized (unreadMessages) {
                unreadMessages.add(text);
            }

            InboxStyle style = new NotificationCompat.InboxStyle().setBigContentTitle(bigContentTitle);
            style.setSummaryText(getApplicationContext().getString(R.string.app_name));
            for (SpannableString line : unreadMessages) {
                style.addLine(line);
            }
            mBuilder.setStyle(style);
        }

        mBuilder.setContentIntent(contentIntent);
    	
    	if(notificationManager == null){
    		notificationManager = (NotificationManager)this.getSystemService(Context.NOTIFICATION_SERVICE);
    	}
    	//TODO push notification
      //  int requestCode = type == TYPE.MESSAGE ? 0 : cast(itemId);
        //notificationManager.notify(requestCode, mBuilder.build());
    }
    static Integer cast(Long id){
    	return (int) (id % Integer.MAX_VALUE);
    }
    static public void notifyMessagesSeen(Context context){
    	synchronized(unreadMessages){
    		unreadMessages.clear();
    	}
    }
}

