package com.morocco.hamssa;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.Image;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.gson.Gson;
import com.morocco.hamssa.data.Database;
import com.morocco.hamssa.entities.RecordVoice;
import com.morocco.hamssa.entities.Topic;
import com.morocco.hamssa.utils.Constants;
import com.morocco.hamssa.utils.HTTPTask;
import com.morocco.hamssa.utils.NameValuePair;
import com.morocco.hamssa.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class NewTopicActivity extends AppCompatActivity implements View.OnClickListener {

    public static final String ARG_TOPIC_ID = "arg_topic_id";
    private static final int TAKE_PICTURE = 1;
    private static final int PICK_IMAGE_REQUEST = 2;
    private int REQUEST_PERMISSION_REQ_CODE=2;
    private final int AUDIO_REQ_CODE = 1;
    private String mCurrentPhotoPath;
    Topic topic;
    ImageView imageView;
    EditText contentText;
    Uri imageUrl;
    Uri audioUrl;
    FloatingActionButton fab;
    Random rnd;
    RecordVoice recordVoice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_topic);

         fab = findViewById(R.id.btn_start_send_record);
         contentText = findViewById(R.id.content);
         imageView = findViewById(R.id.image);
         rnd = new Random();
         recordVoice = new RecordVoice();


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

        if (id == R.id.action_send) {
            send();
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

    private boolean requestAudioPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

            //When permission is not granted by user, show them message why this permission is needed.
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
                Toast.makeText(this, "Please grant permissions to record audio", Toast.LENGTH_LONG).show();
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, AUDIO_REQ_CODE);

            } else {

                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, AUDIO_REQ_CODE);
            }
        }

        return (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED);
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
                findViewById(R.id.Bottom).setVisibility(View.GONE);
                findViewById(R.id.layout_record).setVisibility(View.VISIBLE);
                findViewById(R.id.layout_images_text).setVisibility(View.GONE);

                fab.setImageResource(R.drawable.ic_microphone);
                findViewById(R.id.layout_record_voice).setVisibility(View.GONE);
                findViewById(R.id.player_card).setVisibility(View.GONE);

            }
        });





    }

    private void send() {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageReference = storage.getReference();
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Uploading...");
        EditText editTextt_title = (EditText)findViewById(R.id.title);
        final String title = editTextt_title.getText().toString();
        EditText editText2 = (EditText)findViewById(R.id.content);
        final String content = editText2.getText().toString();
        final String topicId = topic != null ? topic.getId() : null;
        //  final String imageUrl = topic != null ? topic.getImageUrl() : null;
        if(!content.isEmpty()) {
            if(imageUrl != null) {

                StorageReference ref = storageReference.child("images/"+ UUID.randomUUID().toString());
                if(mCurrentPhotoPath!=null) {
                    imageUrl = Uri.fromFile(new File(mCurrentPhotoPath));
                }
                ref.putFile(imageUrl)
                        .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                                taskSnapshot.getStorage().getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Uri> task) {
                                        progressDialog.dismiss();
                                        String downloadUrl = task.getResult().toString();
                                        sendAndFinish(topicId, "", content, downloadUrl, "", null);
                                    }
                                });

                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                progressDialog.dismiss();

                            }
                        })
                        .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                                double progress = (100.0*taskSnapshot.getBytesTransferred()/taskSnapshot
                                        .getTotalByteCount());
                                progressDialog.setMessage("Uploaded "+(int)progress+"%");
                            }
                        });
            }
        }else if(!title.isEmpty()){
            if(audioUrl != null){


                StorageReference ref = storageReference.child("sounds/"+ UUID.randomUUID().toString());
                if(recordVoice.getAudioFilePath()!=null) {
                    audioUrl = Uri.fromFile(new File(recordVoice.getAudioFilePath()));
                }
                ref.putFile(audioUrl)
                        .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                                taskSnapshot.getStorage().getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Uri> task) {
                                        progressDialog.dismiss();
                                        String downloadUrl = task.getResult().toString();
                                        sendAndFinish(topicId, title, "", "", downloadUrl, null);
                                    }
                                });

                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                progressDialog.dismiss();

                            }
                        })
                        .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                                double progress = (100.0*taskSnapshot.getBytesTransferred()/taskSnapshot
                                        .getTotalByteCount());
                                progressDialog.setMessage("Uploaded "+(int)progress+"%");
                            }
                        });

            }else{
                Toast.makeText(this, "audioUrl is null", Toast.LENGTH_SHORT).show();
            }
        }else{
            Toast.makeText(this, getString(R.string.empty_description), Toast.LENGTH_SHORT).show();
        }


    }



    private void sendAndFinish(String topicId, String description, String content, String imageUrl, String audioUrl, String blobKey) {
        List<NameValuePair> params = HTTPTask.getParams(this);
        FirebaseFunctions functions = FirebaseFunctions.getInstance();
        Map<String, Object> data = new HashMap<>();
        data.put("token", params.get(1).getValue());
        data.put("title", description);
        data.put("content", content);
        data.put("image_url", imageUrl);
        data.put("audioUrl", audioUrl);
        //params.add(new NameValuePair("blob_key", blobKey));
        Constants.TASK task = Constants.TASK.CREATE_TOPIC;
        final ProgressDialog progressDialog = ProgressDialog.show(this, getString(R.string.please_wait), getString(R.string.connecting_with_server), true);
        final Context context = this;
        functions.getHttpsCallable(task.toString()).call(data).addOnCompleteListener(new OnCompleteListener<HttpsCallableResult>() {
            @Override
            public void onComplete(@NonNull Task<HttpsCallableResult> task) {
                Gson gson = new Gson();
                if (task.isSuccessful()) {
                    String result = gson.toJson(task.getResult().getData());
                    boolean success = false;
                    int messageId = R.string.connection_error;
                    try {
                        JSONObject jsonObject = new JSONObject(result);
                        if (jsonObject != null) {
                            JSONArray jsonArray = new JSONArray();
                            JSONArray keys = jsonObject.names();
                            Utils.constructTopicsJsonArray(jsonObject, jsonArray, keys);
                            Database db = new Database(context);
                            db.addTopics(jsonArray);
                            success = true;
                        } else if (jsonObject != null && jsonObject.has("error")) {

                            int error = jsonObject.getInt("error");
                            if (error == 2) {
                                messageId = R.string.you_dont_have_permission;
                            } else if (error == 3) {
                                messageId = R.string.you_are_not_the_owner_of_this_topic;
                            }
                        }
                        progressDialog.dismiss();
                        if (success) {
                            finish();
                        } else {
                            Toast.makeText(context, getString(messageId), Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    String message = getString(R.string.connection_error);
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                }
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
            //TextView description = (TextView) findViewById(R.id.description);
            //description.setText(topic.getDescription());
            final TextView content = (TextView) findViewById(R.id.content);
            content.setText(topic.getContent(this));

            String url = topic.getImageUrl();
            if (url != null && !url.isEmpty()) {
                ImageView imageView = (ImageView) findViewById(R.id.image);
                Glide.with(this)
                        .load(topic.getImageUrl())
                        .crossFade()
                        .into(imageView);
                //findViewById(R.id.new_image).setVisibility(View.GONE);

            }

            //String audioUrl = topic.getAudioUrl();
            /*if(auioUrl != null && !url.isEmpty()){
                //show player for editing
            }*/

        }
    }

    boolean isStartRecord = false;
    public void ButtonsClick(View v){



        switch (v.getId()){
            case R.id.btn_start_send_record:
                if(!isStartRecord){
                    findViewById(R.id.layout_record_voice).setVisibility(View.VISIBLE);
                    fab.setImageResource(R.drawable.ic_back_arrow);
                    isStartRecord = true;
                }else{
                    //audioUrl = null;
                    //cancel record
                    findViewById(R.id.layout_record).setVisibility(View.GONE);
                    findViewById(R.id.Bottom).setVisibility(View.VISIBLE);
                    findViewById(R.id.layout_images_text).setVisibility(View.VISIBLE);
                    isStartRecord = false;
                }

                break;
            case R.id.btn_save_record:
                findViewById(R.id.layout_record_voice).setVisibility(View.GONE);
                findViewById(R.id.player_card).setVisibility(View.VISIBLE);
                fab.setImageResource(R.drawable.ic_back_arrow);
                isStartRecord = true;
                break;
            case R.id.btn_cancel_record:
                fab.setImageResource(R.drawable.ic_microphone);
                findViewById(R.id.layout_record_voice).setVisibility(View.GONE);
                isStartRecord = false;
                break;
            case R.id.btn_close_player:
                findViewById(R.id.player_card).setVisibility(View.GONE);
                fab.setImageResource(R.drawable.ic_microphone);
                isStartRecord = false;
                break;
            case R.id.play_pause_btn: findViewById(v.getId()).setBackgroundResource(R.drawable.ic_pause_button);
                break;

        }

    }





}





