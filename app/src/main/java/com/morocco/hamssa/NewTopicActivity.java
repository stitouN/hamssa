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
import android.media.AudioManager;
import android.media.Image;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
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
    Topic topic;
    private String mCurrentPhotoPath;
    String audioPath = null;
    MediaRecorder mediaRecorder;
    public static final int RequestPermissionCode = 1;
    Random rnd;
    ImageView imageView;
    boolean isRecording = false, isPlaying = false;
    EditText contentText;
    final Context context = this;
    Animation fade_out;
    RecordVoice recordVoice;
    CardView player;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_topic);

        rnd = new Random();

        //Tree elements Content topic
        contentText = (EditText)findViewById(R.id.content);
        imageView = (ImageView)findViewById(R.id.image);
        player = (CardView)findViewById(R.id.player);

        //animation
        fade_out = AnimationUtils.loadAnimation(this, R.anim.fade_out);
        //create object from class RecordVoice;
        recordVoice = new RecordVoice();

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


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void setupInputs() {
        if (topic != null) {
            TextView description = (TextView) findViewById(R.id.description);
            description.setText(topic.getDescription());
            final TextView content = (TextView) findViewById(R.id.content);
            content.setText(topic.getContent(this));

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

            //get sound from topic for edit
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

        //button change color content text
        findViewById(R.id.btn_change_text_color).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                  int color = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));

                  contentText.setTextColor(color);
                  contentText.setHintTextColor(color);

            }
        });

        //btn record
        findViewById(R.id.btn_record).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // custom dialog
                final Dialog dialog = new Dialog(context);
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.setContentView(R.layout.custom);
                // if button is clicked, close the custom dialog
                dialog.findViewById(R.id.dialog_exit).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        recordVoice.deleteFile();
                        dialog.dismiss();
                    }
                });
                final ImageButton btnRec = (ImageButton)dialog.findViewById(R.id.btn_start_stop_record);
                final Chronometer chronometer = (Chronometer)dialog.findViewById(R.id.chronometer);
                btnRec.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        if(!isRecording){
                            btnRec.setImageResource(R.drawable.ic_stop_black_24dp);
                            dialog.findViewById(R.id.bottom_ly).setVisibility(View.GONE);
                            chronometer.setBase(SystemClock.elapsedRealtime());
                            chronometer.start();
                            audioPath = Environment.getExternalStorageDirectory()+ "/voice.3gpp";
                            recordVoice.setAudioPath(audioPath);
                            try {
                                recordVoice.startRecording();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            isRecording = true;
                        }else {
                            btnRec.setImageResource(R.drawable.ic_fiber_manual_record_black_24dp);
                            dialog.findViewById(R.id.bottom_ly).setVisibility(View.VISIBLE);
                            chronometer.stop();
                            recordVoice.stopRecording();
                            isRecording = false;
                        }

                    }
                });
                dialog.findViewById(R.id.btn_save).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                            imageUrl = null;
                            imageView.setVisibility(View.GONE);
                            contentText.setVisibility(View.GONE);
                            player.setVisibility(View.VISIBLE);
                            Toast.makeText(NewTopicActivity.this, "voice saved",Toast.LENGTH_SHORT).show();

                        dialog.dismiss();
                    }
                });
                dialog.findViewById(R.id.btn_cancel).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        recordVoice.deleteFile();
                        dialog.dismiss();
                    }
                });
                dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        Toast.makeText(NewTopicActivity.this, "cancel", Toast.LENGTH_SHORT).show();
                    }
                });

                dialog.show();
            }
        });

        for(int i=1; i<14; i++) {
            ImageButton back =(ImageButton)findViewById(R.id.back+i);
            back.setClipToOutline(true);
            back.setOnClickListener(this);
        }


        //call controllPlayer()
        controllPlayer();



    }// end method SetupInputs()

    private static final int TAKE_PICTURE = 1;
    private static final int PICK_IMAGE_REQUEST = 2;
    Uri imageUrl;
    Uri audioUrl;

    private void pickImage(){
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, TAKE_PICTURE);
    }

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
        final String content = contentText.getText().toString();
        final String topicId = topic != null ? topic.getId() : null;


            if(imageUrl == null) {
                if (recordVoice.getAudioPath() == null) {

                    sendAndFinish(topicId, "", content, "", "", null);

                } else {
                    audioUrl = Uri.fromFile(new File(recordVoice.getAudioPath()));
                    uploadImageOrAudio("sounds/", audioPath, topicId, "", "");
                }
            }else {

                uploadImageOrAudio("images/", mCurrentPhotoPath, topicId, "", content);
            }




    }

    Uri ImageOrAudio_Url;
    private void uploadImageOrAudio(final String child, String path, final String id,final String titre, final String contenu ){

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageReference = storage.getReference();

        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Uploading...");

        StorageReference ref = storageReference.child(child + UUID.randomUUID().toString());

        if(path != "") {
            ImageOrAudio_Url = Uri.fromFile(new File(path));

            ref.putFile(ImageOrAudio_Url).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    taskSnapshot.getStorage().getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                        @Override
                        public void onComplete(@NonNull Task<Uri> task) {
                            progressDialog.dismiss();
                            String downloadUrl = task.getResult().toString();
                            if (child == "images/") {
                                sendAndFinish(id, titre, contenu, downloadUrl, "", null);
                            } else {
                                sendAndFinish(id, titre, "", "", downloadUrl, null);
                            }
                        }
                    });
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    progressDialog.dismiss();

                }
            }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                    double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot
                            .getTotalByteCount());
                    progressDialog.setMessage("Uploaded " + (int) progress + "%");
                }
            });
        //}else{
         //   Toast.makeText(getApplicationContext(), "File path is null:"+path, Toast.LENGTH_SHORT).show();
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

    private void addPictureToDevice(){
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        intent.setData(contentUri);
        this.sendBroadcast(intent);
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

    }

    private void controllPlayer(){

        findViewById(R.id.btn_close_player).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                recordVoice.deleteFile();
                audioPath = null;
                player.setAnimation(fade_out);
                player.setVisibility(View.GONE);
                imageView.setVisibility(View.VISIBLE);
                contentText.setVisibility(View.VISIBLE);
            }
        });

        findViewById(R.id.btn_play_pause).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!isPlaying){
                    //play sound call method changeVoice(type)
                    //get type voice from spinner and change voice
                    //change icon play to pause
                    //seekbar start
                    isPlaying = true;
                }else{
                    //pause sound
                    //change icon pause to play
                    //seekbar stop
                    isPlaying = false;
                }
            }
        });

    }
}





