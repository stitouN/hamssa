package com.morocco.hamssa;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.morocco.hamssa.data.Database;
import com.morocco.hamssa.entities.RecordVoice;
import com.morocco.hamssa.entities.Topic;
import com.morocco.hamssa.utils.SendTopic;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

public class NewTopicActivity extends AppCompatActivity implements View.OnClickListener {

    public static final String ARG_TOPIC_ID = "arg_topic_id";
    private static final int TAKE_PICTURE = 1;
    private static final int PICK_IMAGE_REQUEST = 2;
    private int REQUEST_PERMISSION_REQ_CODE=2;
    private String mCurrentPhotoPath;
    Topic topic;
    ImageView imageView;
    EditText contentText;
    Uri imageUrl;
    Random rnd;
    RecordVoice recordVoice;
    SendTopic sendTopic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_topic);

        contentText = findViewById(R.id.content);
        imageView = findViewById(R.id.image);
        rnd = new Random();
        recordVoice = new RecordVoice();
        sendTopic = new SendTopic(this);
        GetImagesFromFirebaseStorage("image1.jpg");



        for(int i=1; i<14; i++) {
            final ImageButton back =(ImageButton)findViewById(R.id.back+i);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                back.setClipToOutline(true);
            }
            back.setOnClickListener(this);
        }

        if (getIntent().hasExtra(ARG_TOPIC_ID)) {
            String topicId = getIntent().getStringExtra(ARG_TOPIC_ID);
            topic = new Database(this).getTopic(topicId);
        }


        setupActionBar();
        setupInputs();


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_new_topic, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        final EditText des = findViewById(R.id.content);
        final String text = des.getText().toString();

        if (id == R.id.action_send) {

            if( text != "" && !text.isEmpty()){
                if( imageUrl != null) {
                    sendTopic.send(des.getText().toString(), imageUrl, "images");
                }
            }else{
                Toast.makeText(NewTopicActivity.this, getString(R.string.empty_description), Toast.LENGTH_LONG).show();
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK)
            switch (requestCode){
                case PICK_IMAGE_REQUEST:
                    imageUrl = data.getData();
                    //Uri selectedImage = data.getData();
                    imageView.setImageURI(imageUrl);
                    break;
                case TAKE_PICTURE:
                    imageUrl = Uri.fromFile(new File(mCurrentPhotoPath));
                    imageView.setImageURI(imageUrl);
                    break;
            }
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


    private void setupInputs() {

        LoadTopicForEditing();

        findViewById(R.id.camera).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Intent intent =  new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                if (ContextCompat.checkSelfPermission(NewTopicActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(NewTopicActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION_REQ_CODE);
                }
                if (ContextCompat.checkSelfPermission(NewTopicActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED /*&& intent.resolveActivity(getPackageManager()) != null*/) {

                    try {
                        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        intent.putExtra(MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(NewTopicActivity.this, "com.morocco.hamssa.fileprovider", createImageFile()));
                        startActivityForResult(intent, TAKE_PICTURE);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });

        findViewById(R.id.gallery).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                // Show only images, no videos or anything else
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                // Always show the chooser (if there are multiple options available)
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
            }
        });

        findViewById(R.id.btn_change_text_color).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int color = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
                contentText.setTextColor(color);
                contentText.setHintTextColor(color);
            }
        });

        findViewById(R.id.btn_mic).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent recordVoice = new Intent(NewTopicActivity.this, RecordVoiceActivity.class);
                startActivity(recordVoice);

            }
        });





    }


    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }


    ///////////////////// START PART STATIC IMAGES //////////////////
    ImageButton btnBack = null;
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onClick(View view) {
        mCurrentPhotoPath = null;
        ImageView image = (ImageView)findViewById(R.id.image);
        btnBack = (ImageButton)findViewById(view.getId());
        image.setImageBitmap(null);
        image.destroyDrawingCache();
        image.setBackground(btnBack.getDrawable());
        //
        GetImagesFromFirebaseStorage(btnBack.getContentDescription().toString());
    }
    private void GetImagesFromFirebaseStorage(String imageSelected){
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageReference = storage.getReference();
        storageReference.child("images/"+imageSelected).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                imageUrl = uri;
            }
        });
    }
    ///////////////////////////// END PART STATIC IMAGES //////////////////


    private void LoadTopicForEditing(){
        if (topic != null) {
            final TextView content = (TextView) findViewById(R.id.content);
            content.setText(topic.getContent(this));
            String url = topic.getImageUrl();
            if (url != null && !url.isEmpty()) {
                ImageView imageView = (ImageView) findViewById(R.id.image);
                Glide.with(this)
                        .load(topic.getImageUrl())
                        .crossFade()
                        .into(imageView);

            }

        }
    }







}





