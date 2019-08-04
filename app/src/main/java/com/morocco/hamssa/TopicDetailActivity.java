package com.morocco.hamssa;

import android.content.Intent;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.morocco.hamssa.data.Database;
import com.morocco.hamssa.entities.Topic;
import com.morocco.hamssa.share.ShareWindow;
import com.morocco.hamssa.utils.Utils;

public class TopicDetailActivity extends AppCompatActivity {

    private static final String TAG = TopicActivity.class.getSimpleName();

    public static final String ARG_TOPIC_ID = "arg_topic_id";

    Topic topic;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_topic_detail);

        String topicId = getIntent().getStringExtra(ARG_TOPIC_ID);
        topic = new Database(this).getTopic(topicId);

        setupActionBar();
        setupTopicContent();
    }

    private void setupActionBar(){
        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);

            String title = topic.getTitle();
            if(title != null && !title.isEmpty()){
                actionBar.setTitle(title);
            }
        }
    }

    private void setupTopicContent(){
        TextView author = (TextView)findViewById(R.id.author);
        author.setText("@"+topic.getUserName());
        TextView time = (TextView)findViewById(R.id.time);
        time.setText(Utils.msToElapsedTime(this, topic.getTime()));
        TextView description = (TextView)findViewById(R.id.description);
        description.setText(topic.getDescription());

        String url = topic.getImageUrl();
        if(url != null && !url.isEmpty()) {
            findViewById(R.id.image_wrapper).setVisibility(View.VISIBLE);
            ImageView imageView = (ImageView) findViewById(R.id.image);
            Glide.with(this)
                    .load(url)
                    .crossFade()
                    .into(imageView);
        }
        /*String audioUrl = topic.getAudioUrl();
        if(audioUrl != null && !audioUrl.isEmpty()){
            //add sound here
        }*/

        final TextView contentTextView = (TextView)findViewById(R.id.content);
        String content = topic.getContent(this);
        if(content != null){
            contentTextView.setText(content);
        }
        topic.fetchContent(this, new Topic.OnContentListener() {
            @Override
            public void onContent(boolean success, String content) {
                if (success) {
                    contentTextView.setText(content);
                } else {
                    Toast.makeText(TopicDetailActivity.this, getString(R.string.connection_error), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_topic_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(id == R.id.action_messages){
            Intent intent = new Intent(this, TopicActivity.class);
            intent.putExtra(TopicActivity.ARG_TOPIC_ID, topic.getId());
            startActivity(intent);
        }else if(id == R.id.action_share){
            String shareText = "@"+topic.getUserName()+" "+topic.getDescription();
            ShareWindow inviteWindow = new ShareWindow(this, shareText, topic.getImageUrl());
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
}
