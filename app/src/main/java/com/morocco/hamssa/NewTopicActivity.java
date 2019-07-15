package com.morocco.hamssa;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.gson.Gson;
import com.morocco.hamssa.data.Database;
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

public class NewTopicActivity extends AppCompatActivity {

    public static final String ARG_TOPIC_ID = "arg_topic_id";
    Topic topic;
    private String mCurrentPhotoPath;
    private int REQUEST_PERMISSION_REQ_CODE = 2;
    private boolean isRecording = false;
    String AudioSavePathInDevice = null;
    MediaRecorder mediaRecorder;
    public static final int RequestPermissionCode = 1;
    Chronometer chronometer;
    SoundPool soundPool = null;
    int soundId;
    boolean loaded, isPlaying = false;
    Button btn_record;
    ImageButton btn_play_pause;
    ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_topic);

        imageView = (ImageView)findViewById(R.id.image);

        if (getIntent().hasExtra(ARG_TOPIC_ID)) {
            String topicId = getIntent().getStringExtra(ARG_TOPIC_ID);
            topic = new Database(this).getTopic(topicId);
        }


        setupActionBar();
        setupInputs();




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


    Bitmap bitmap = null;
    private static final int TAKE_PICTURE = 1;
    private static final int PICK_IMAGE_REQUEST = 2;
    private int AUDIO_REC_CODE = 3;
    Uri audioUrl;
    Uri imageUrl;
    File image;



    private void pickImage(){
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, TAKE_PICTURE);
    }

    private void setupInputs() {
        if (topic != null) {
            TextView description = (TextView) findViewById(R.id.description);
            description.setText(topic.getDescription());
            final TextView content = (TextView) findViewById(R.id.content);
            content.setText(topic.getContent(this));
            /*topic.fetchContent(this, new Topic.OnContentListener() {
                @Override
                public void onContent(boolean success, String contentString) {
                    if (success) {
                        if(contentString != null && !contentString.isEmpty()) {
                            content.setText(contentString);
                        }
                    } else {
                        Toast.makeText(NewTopicActivity.this, getString(R.string.connection_error), Toast.LENGTH_SHORT).show();
                    }
                }
            });*/

            String url = topic.getImageUrl();
            if (url != null && !url.isEmpty()) {
                findViewById(R.id.image).setVisibility(View.VISIBLE);
                ImageView imageView = (ImageView) findViewById(R.id.image);
                Glide.with(this)
                        .load(topic.getImageUrl())
                        .crossFade()
                        .into(imageView);
                //findViewById(R.id.new_image).setVisibility(View.GONE);

            }

            //this for audio
            String audioUrl = topic.getAudioUrl();
            if(audioUrl != null && !audioUrl.isEmpty()){
                //show audio player
            }
        }

        // Take photo from camera
        findViewById(R.id.imageView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                int checkContextCompat = ContextCompat.checkSelfPermission(NewTopicActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE);
                int pm_pg = PackageManager.PERMISSION_GRANTED;
                int checkActivityCompat = ActivityCompat.checkSelfPermission(NewTopicActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

                if (checkContextCompat != pm_pg || checkActivityCompat != pm_pg) {
                    ActivityCompat.requestPermissions(NewTopicActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, TAKE_PICTURE);
                }else {
                    pickImage();
                }

             }
        });

        // Select Image or photo from gallery
        findViewById(R.id.gallery).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent pickPhoto = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(pickPhoto , PICK_IMAGE_REQUEST);
            }
        });

    }// end method SetupInputs()

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK)
            switch (requestCode){
                case PICK_IMAGE_REQUEST:
                    imageUrl = data.getData();
                    Uri selectedImage = data.getData();
                    imageView.setImageURI(selectedImage);
                    break;
                case TAKE_PICTURE:
                    Bundle extra = data.getExtras();
                    Bitmap btm = (Bitmap) extra.get("data");
                    imageView.setImageBitmap(btm);
                    try {
                        createImageFile();
                        addPictureToDevice();
                    }catch (Exception e){
                        e.printStackTrace();
                    }

                    break;
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

        if (id == R.id.action_send) {
            send();
        }

        return super.onOptionsItemSelected(item);
    }

    private void send() {

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageReference = storage.getReference();

        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Uploading...");

        EditText editText1 = (EditText) findViewById(R.id.description);
        final String description = editText1.getText().toString();

        EditText editText2 = (EditText) findViewById(R.id.content);
        final String content = editText2.getText().toString();

        final String topicId = topic != null ? topic.getId() : null;
        //  final String imageUrl = topic != null ? topic.getImageUrl() : null;
        if (!description.isEmpty()) {
            if (imageUrl == null) {
                sendAndFinish(topicId, description, content, "", "", null);
            } else {
                final Context context = this;
                StorageReference ref = storageReference.child("images/" + UUID.randomUUID().toString());
               /* StorageMetadata metadata = new StorageMetadata.Builder()
                        .setCustomMetadata("clientId", Constants.CLIENT_TOKEN)
                        .build();
                ref.*/
                if (mCurrentPhotoPath != null) {
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
                                double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot
                                        .getTotalByteCount());
                                progressDialog.setMessage("Uploaded " + (int) progress + "%");
                            }
                        });
              /*  new ImageUploader(this, false, new ImageUploader.OnReceiveJSON() {
                    @Override
                    public void onJSON(JSONObject jsonObject) {
                        Integer errorStringId = null;
                        if(jsonObject != null) {
                            try {
                                String imageUrl = jsonObject.getString("servingURL");
                                String blobKey = jsonObject.getString("blobKey");
                                sendAndFinish(topicId, description, content, imageUrl, blobKey);
                            } catch (JSONException e) {
                                errorStringId = R.string.connection_error;
                            }
                        }else{
                            errorStringId = R.string.connection_error;
                        }
                        if(errorStringId != null){
                            Toast.makeText(context, context.getString(errorStringId), Toast.LENGTH_SHORT).show();
                        }
                    }
                }).upload(bitmap);*/
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

    private void addPictureToDevice(){
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        intent.setData(contentUri);
        this.sendBroadcast(intent);
    }

    //Audio Recording...

    private void startRecording() {
        if (checkPermission()) {

            AudioSavePathInDevice = Environment.getExternalStorageDirectory().getAbsolutePath() + "/voice.3gp";

            chronometer.setBase(SystemClock.elapsedRealtime());
            chronometer.start();
            MediaRecorderReady();

            try {
                mediaRecorder.prepare();
                mediaRecorder.start();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            ActivityCompat.requestPermissions(NewTopicActivity.this, new String[]{WRITE_EXTERNAL_STORAGE, RECORD_AUDIO}, RequestPermissionCode);
        }

    }

    private void stopRecording() {

        mediaRecorder.stop();

        chronometer.stop();
        chronometer.setBase(SystemClock.elapsedRealtime());

        soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        soundId = soundPool.load(AudioSavePathInDevice, 1);
        soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                loaded = true;
            }
        });

    }

    public void MediaRecorderReady() {
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
        mediaRecorder.setOutputFile(AudioSavePathInDevice);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case RequestPermissionCode:
                if (grantResults.length > 0) {
                    boolean StoragePermission = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean RecordPermission = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if (StoragePermission && RecordPermission) {
                        Toast.makeText(NewTopicActivity.this, "Permission Granted", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(NewTopicActivity.this, "Permission Denied", Toast.LENGTH_LONG).show();
                    }
                }
                break;
        }
    }

    public boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(), WRITE_EXTERNAL_STORAGE);
        int result1 = ContextCompat.checkSelfPermission(getApplicationContext(), RECORD_AUDIO);
        return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED;


    }

    private void changeVoice(){
        if (loaded) {
            soundPool.play(soundId, 100, 100, 1, 0, 1.3f);
        };
    }

}





