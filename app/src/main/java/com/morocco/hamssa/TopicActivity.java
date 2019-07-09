package com.morocco.hamssa;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EdgeEffect;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;
import com.google.gson.Gson;
import com.morocco.hamssa.adapters.MessagesFragmentPagerAdapter;
import com.morocco.hamssa.adapters.TopicsFragmentPagerAdapter;
import com.morocco.hamssa.data.Database;
import com.morocco.hamssa.entities.Message;
import com.morocco.hamssa.entities.Topic;
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

public class TopicActivity extends AppCompatActivity implements MessagesFragment.OnMessageClickListener{

    private static final String TAG = TopicActivity.class.getSimpleName();

    public static final String ARG_TOPIC_ID = "arg_topic_id";

    Topic topic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_topic);

        if(!getIntent().hasExtra(ARG_TOPIC_ID)){
            Log.e(TAG, "No topic id");
            finish();
        }
        String topicId = getIntent().getStringExtra(ARG_TOPIC_ID);
        topic = new Database(this).getTopic(topicId);

        setupActionBar();
        setupViewPager();
        setupSend();
    }

    private void setupActionBar(){
        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);

            String activityTitle = topic.getTitle();
            if (activityTitle == null || activityTitle.isEmpty()) {
                activityTitle = topic.getDescription();
            }
            if (topic.getUserName() != null) {
                setTitle("@" + topic.getUserName());
                actionBar.setSubtitle(activityTitle);
            } else {
                setTitle(activityTitle);
            }
        }
    }

    ViewPager viewPager;
    MessagesFragmentPagerAdapter fragmentPagerAdapter;
    private void setupViewPager(){
        fragmentPagerAdapter = new MessagesFragmentPagerAdapter(getSupportFragmentManager(), this, topic.getId());

        viewPager = (ViewPager) findViewById(R.id.viewpager);
        viewPager.setAdapter(fragmentPagerAdapter);
        viewPager.setOffscreenPageLimit(3);

        final TabLayout tabLayout = (TabLayout) findViewById(R.id.sliding_tabs);
        tabLayout.setupWithViewPager(viewPager);
    }


    private void setupSend(){
        final Context context = this;
        findViewById(R.id.send).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseFunctions functions = FirebaseFunctions.getInstance();
                Map<String, Object> data=new HashMap<>();
                final EditText editText = (EditText)findViewById(R.id.editText);
                String text = editText.getText().toString();
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
                if(!text.isEmpty()){
                    List<NameValuePair> params = HTTPTask.getParams(context);
                    data.put("topicId",topic.getId());
                    data.put("text",text);
                    data.put("token",params.get(1).getValue());
                    data.put("imageUrl","");
                    data.put("name","name");
                    data.put("numMessages",topic.getNumMessages());
                    List<Long> messageOrdinals = Utils.parseMessageReferences(text);
                    Database db = new Database(context);
                   /* for(Long messageOrdinal : messageOrdinals){
                        Message message = db.getMessageByOrdinal(topic.getId(), messageOrdinal);
                        if(message != null) {
                            params.add(new NameValuePair("target_user_ids", message.getUserId()));
                        }
                    }
                    List<String> userNames = Utils.parseUserReferences(text);
                    for(String name : userNames){
                        params.add(new NameValuePair("target_user_names", name));
                    }*/
                    Constants.TASK task = Constants.TASK.CREATE_MESSAGE;

                    final ProgressDialog progressDialog = ProgressDialog.show(context, getString(R.string.please_wait), getString(R.string.connecting_with_server), true);
                    functions.getHttpsCallable(task.toString())
                            .call(data)
                            .addOnCompleteListener(new OnCompleteListener<HttpsCallableResult>() {
                                @Override
                                public void onComplete(@NonNull Task<HttpsCallableResult> task) {
                                    if(task.isSuccessful()) {
                                    try{
                                    progressDialog.dismiss();
                                    Gson gson = new Gson();
                                    String result = gson.toJson(task.getResult().getData());
                                    JSONObject jsonObject = new JSONObject(result);
                                        if (jsonObject != null) {
                                        String message = getString(R.string.message_sent);
                                        editText.setText("");
                                        fragmentPagerAdapter.notifyMessageSent();
                                        viewPager.setCurrentItem(0, true);
                                         Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                                        }
                                    }catch (JSONException e) {
                                        progressDialog.dismiss();
                                    }}else{
                                        String message = getString(R.string.connection_error);
                                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                                    }

                                }
                            });
                }
            }
        });
    }

    public void OnMessageClick(String messageId){
       /* Message message = new Database(this).getMessage(messageId);
       EditText editText = (EditText)findViewById(R.id.editText);
        editText.append("#"+message.getOrdinal()+" ");*/
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if(topic.isPersonal()) {
            getMenuInflater().inflate(R.menu.menu_topic, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(id == R.id.action_expand){
            Intent intent = new Intent(this, TopicDetailActivity.class);
            intent.putExtra(TopicDetailActivity.ARG_TOPIC_ID, topic.getId());
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }
}
