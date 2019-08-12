package com.morocco.hamssa;


import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.morocco.hamssa.data.Database;
import com.morocco.hamssa.entities.RecordVoice;
import com.morocco.hamssa.entities.Topic;
import com.morocco.hamssa.utils.SendTopic;
import com.xw.repo.BubbleSeekBar;

import java.io.File;
import java.io.IOException;


public class RecordVoiceActivity extends AppCompatActivity {

    public static final String ARG_TOPIC_ID = "arg_topic_id";
    FloatingActionButton fab;
    RadioGroup rg;
    RecordVoice recordVoice;
    SendTopic sendTopic;
    Topic topic;
    BubbleSeekBar bubbleSeekBar;
    ImageView btn_play_stop;
    TextView valueRate;
    private final String audioFilePath = Environment.getExternalStorageDirectory().getAbsolutePath() +"/voice.amr";

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_voice);

        recordVoice = new RecordVoice();
        sendTopic = new SendTopic(this);

        fab = findViewById(R.id.btn_start_send_record);
        bubbleSeekBar = findViewById(R.id.seekbar_rate);
        valueRate = findViewById(R.id.value_rate);
        chronometer = findViewById(R.id.chronometer_record);
        chronometerSound = findViewById(R.id.chronometer_sound);
        btn_play_stop = findViewById(R.id.btn_play_stop);

        bubbleSeekBar.setOnProgressChangedListener(new BubbleSeekBar.OnProgressChangedListener() {
            @Override
            public void onProgressChanged(int progress, float progressFloat) {
                valueRate.setText(""+progressFloat);
                recordVoice.stopSound();
                btn_play_stop.setBackgroundResource(R.drawable.ic_play_button);
            }

            @Override
            public void getProgressOnActionUp(int progress, float progressFloat) {
                valueRate.setText(""+progressFloat);

            }

            @Override
            public void getProgressOnFinally(int progress, float progressFloat) {

            }
        });




        if (getIntent().hasExtra(ARG_TOPIC_ID)) {
            String topicId = getIntent().getStringExtra(ARG_TOPIC_ID);
            topic = new Database(this).getTopic(topicId);
        }


        setupActionBar();


    }

    private Chronometer chronometer, chronometerSound ;
    boolean isStartRecord = false;
    boolean isPlaying = false;
    public void ButtonsClick(View v) throws IOException {



        switch (v.getId()){
            case R.id.btn_start_send_record:
                if(!isStartRecord){
                    if(requestAudioPermissions()){
                        chronometer.setBase(SystemClock.elapsedRealtime());
                        chronometer.start();
                        recordVoice.setAudioFilePath(audioFilePath);
                        recordVoice.startRecording();
                        fab.setImageResource(R.drawable.ic_back_arrow);
                        findViewById(R.id.layout_record_voice).setVisibility(View.VISIBLE);
                        isStartRecord = true;
                    }
                }else{
                    //audioUrl = null;
                    //cancel record
                    isStartRecord = false;
                    Intent newTopic = new Intent(this, MainActivity.class);
                    startActivity(newTopic);
                }

                break;
            case R.id.btn_save_record:
                recordVoice.stopRecording();
                chronometerSound.setBase(chronometer.getBase());
                findViewById(R.id.layout_record_voice).setVisibility(View.GONE);
                findViewById(R.id.card_player).setVisibility(View.VISIBLE);
                fab.setImageResource(R.drawable.ic_back_arrow);
                isStartRecord = true;
                break;
            case R.id.btn_cancel_record:
                fab.setImageResource(R.drawable.ic_microphone);
                findViewById(R.id.layout_record_voice).setVisibility(View.GONE);
                isStartRecord = false;
                break;
            case R.id.btn_close_player:
                recordVoice.setAudioFilePath("");
                findViewById(R.id.card_player).setVisibility(View.GONE);
                fab.setImageResource(R.drawable.ic_microphone);
                isStartRecord = false;
                break;
            case R.id.btn_play_stop:

                if(!isPlaying){
                    recordVoice.playSound(Float.parseFloat(valueRate.getText().toString()));
                    btn_play_stop.setBackgroundResource(R.drawable.ic_stop);
                    isPlaying = true;
                }else{
                    btn_play_stop.setBackgroundResource(R.drawable.ic_play_button);
                    isPlaying = false;
                }



        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_new_topic, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        final EditText des = findViewById(R.id.title);
        final String text = des.getText().toString();
        audioUrl = recordVoice.getAudioUri();

        if (id == R.id.action_send) {

            if( text != "" && !text.isEmpty()){
                if( audioUrl != null) {
                    sendTopic.send(des.getText().toString(), audioUrl, "sounds");
                }else{
                    Toast.makeText(RecordVoiceActivity.this, "empty audio ", Toast.LENGTH_LONG).show();

                }
            }else{
                Toast.makeText(RecordVoiceActivity.this, getString(R.string.title_empty), Toast.LENGTH_LONG).show();
            }
        }

        return super.onOptionsItemSelected(item);
    }

    private void setupActionBar() {
        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            if (topic != null) {
                actionBar.setTitle(getString(R.string.editing));
            }
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case AUDIO_REQ_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //start recording
                } else {
                    Toast.makeText(this, "Permissions Denied to record audio", Toast.LENGTH_LONG).show();
                }
                return;
        }
    }

    private final int AUDIO_REQ_CODE = 1;
    Uri audioUrl;
    private boolean requestAudioPermissions() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

            //When permission is not granted by user, show them message why this permission is needed.
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.RECORD_AUDIO)) {
                Toast.makeText(this, "Please grant permissions to record audio", Toast.LENGTH_LONG).show();
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.RECORD_AUDIO}, AUDIO_REQ_CODE);

            } else {

                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.RECORD_AUDIO}, AUDIO_REQ_CODE);
            }
        }

        return (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED);
    }



}
