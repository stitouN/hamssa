package com.morocco.hamssa;

import android.content.Intent;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import com.morocco.hamssa.adapters.MessagesFragmentPagerAdapter;
import com.morocco.hamssa.adapters.MyMessagesFragmentPagerAdapter;
import com.morocco.hamssa.data.Database;
import com.morocco.hamssa.entities.Message;
import com.morocco.hamssa.gcm.GcmIntentService;

public class MyMessagesActivity extends AppCompatActivity implements MessagesFragment.OnMessageClickListener{

    static public final String ARG_USER_NAME = "user_name";
    static public final String ARG_TYPE = "type";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_messages);
        setupActionBar();
        setupViewPager();
        GcmIntentService.notifyMessagesSeen(this);
    }

    private void setupActionBar(){
        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }
        if(getIntent().hasExtra(ARG_USER_NAME)){
            setTitle("@"+getIntent().getStringExtra(ARG_USER_NAME));
        }else{
            setTitle(getString(R.string.action_messages));
        }

        mToolbar.setTitleTextColor(getResources().getColor(android.R.color.white));
    }

    ViewPager viewPager;
    MyMessagesFragmentPagerAdapter fragmentPagerAdapter;
    private void setupViewPager(){
        if(getIntent().hasExtra(ARG_USER_NAME)){
            String userName = getIntent().getStringExtra(ARG_USER_NAME);
            fragmentPagerAdapter = new MyMessagesFragmentPagerAdapter(getSupportFragmentManager(), this, userName);
        }else {
            fragmentPagerAdapter = new MyMessagesFragmentPagerAdapter(getSupportFragmentManager(), this);
        }

        viewPager = (ViewPager) findViewById(R.id.viewpager);
        viewPager.setAdapter(fragmentPagerAdapter);
        viewPager.setOffscreenPageLimit(3);

        int typeInt = getIntent().getIntExtra(ARG_TYPE, -1);
        MessagesFragment.TYPE type = MessagesFragment.TYPE.getType(typeInt);
        if(type == MessagesFragment.TYPE.SENT && fragmentPagerAdapter.getCount() > 1) {
            viewPager.setCurrentItem(1, false);
        }

        final TabLayout tabLayout = (TabLayout) findViewById(R.id.sliding_tabs);
        tabLayout.setupWithViewPager(viewPager);
    }

    public void OnMessageClick(String messageId){
        Message message = new Database(this).getMessage(messageId);
        String topicId = message.getTopicId();
        Intent intent = new Intent(this, TopicActivity.class);
        intent.putExtra(TopicActivity.ARG_TOPIC_ID, topicId);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_my_messages, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        return super.onOptionsItemSelected(item);
    }
}
