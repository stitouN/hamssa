package com.morocco.hamssa;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.preference.PreferenceManager;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.morocco.hamssa.adapters.TopicsFragmentPagerAdapter;
import com.morocco.hamssa.data.Database;
import com.morocco.hamssa.entities.Message;
import com.morocco.hamssa.gcm.RegistrationIntentService;
import com.morocco.hamssa.share.ShareWindow;
import com.morocco.hamssa.utils.Constants;
import com.morocco.hamssa.utils.Utils;

import java.util.Locale;

public class MainActivity extends AppCompatActivity implements MessagesFragment.OnMessageClickListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences preferences = getSharedPreferences("Settings", 0);
        SetLocale(preferences.getString("My_lang", ""));
        setContentView(R.layout.activity_main);
        if(isLogged(this)) {
            setupActionBar();
            setupViewPager();
            setupFABButton();
            if(Utils.checkPlayServices(this)){
                // Start IntentService to register this application with GCM
                Intent intent = new Intent(this, RegistrationIntentService.class);
                startService(intent);
            }
            LoginActivity.fetchMyUserInfo(this);
        }else{
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        }


    }

    public static boolean isLogged(Context context){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return !sp.getString(Constants.SP.USER_ID,"").isEmpty();
    }

    private void setupActionBar(){
        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        setTitle(getString(R.string.app_name));
        mToolbar.setTitleTextColor(getResources().getColor(android.R.color.white));
    }

    private void setupFABButton(){
        View fab = findViewById(R.id.fab);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        if(sp.getLong(Constants.SP.USER_ROLE, 0) == 1 || Constants.EVERYBODY_CAN_POST){
            fab.setVisibility(View.VISIBLE);
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(MainActivity.this, NewTopicActivity.class));
                }
            });
        }
    }

    ViewPager viewPager;
    TopicsFragmentPagerAdapter fragmentPagerAdapter;
    private void setupViewPager(){
        fragmentPagerAdapter = new TopicsFragmentPagerAdapter(getSupportFragmentManager(), this);

        viewPager = (ViewPager) findViewById(R.id.viewpager);
        viewPager.setAdapter(fragmentPagerAdapter);
        viewPager.setOffscreenPageLimit(3);

        final TabLayout tabLayout = (TabLayout) findViewById(R.id.sliding_tabs);
        tabLayout.setupWithViewPager(viewPager);

        viewPager.setCurrentItem(1, false);
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
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(id == R.id.action_configuration){
            startActivity(new Intent(this, ConfigurationActivity.class));
        }else if(id == R.id.action_messages){
            startActivity(new Intent(this, MyMessagesActivity.class));
        }else if(id == R.id.action_share){
            ShareWindow inviteWindow = new ShareWindow(this, null, null);
            View anchorView = findViewById(R.id.action_share);
            if(anchorView != null) {
                inviteWindow.show(anchorView);
            }else{
                anchorView = findViewById(R.id.toolbar);
                inviteWindow.showCenter(anchorView);
            }

        }

        return super.onOptionsItemSelected(item);
    }




    public void SetLocale(String lang){
        Locale myLocale = new Locale(lang);
        Locale.setDefault(myLocale);
        Configuration conf = new Configuration();
        conf.locale = myLocale;
        getBaseContext().getResources().updateConfiguration(conf, getBaseContext().getResources().getDisplayMetrics());

    }






}
