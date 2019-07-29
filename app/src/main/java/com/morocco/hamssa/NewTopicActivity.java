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
    Topic topic;
    private String mCurrentPhotoPath;
    String audioPath = null;
    public static final int RequestPermissionCode = 1;
    Random rnd;
    ImageView imageView;
    boolean isRecording = false, isPlaying = false, isFromActtivityResult = false;
    EditText contentText;
    final Context context = this;
    Animation fade_out;
    RecordVoice recordVoice;
    CardView player;
    private int REQUEST_PERMISSION_REQ_CODE=2;
    Spinner spinnerType;
    String typeVoice;
    ImageButton btn_paly_pause;
    SoundPool soundPool;
    int soundId;
    long elapsedMillis;




    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_topic);

        rnd = new Random();

        contentText = (EditText)findViewById(R.id.content);
        imageView = (ImageView)findViewById(R.id.image);
        player = (CardView)findViewById(R.id.player);
        spinnerType = (Spinner)findViewById(R.id.spinner_type_voice);
        btn_paly_pause = (ImageButton)findViewById(R.id.btn_play_pause);

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

        //This part for edit topic
        if (topic != null) {
            //TextView description = (TextView) findViewById(R.id.description);
            //description.setText(topic.getDescription());
            final TextView content = (TextView) findViewById(R.id.content);
            content.setText(topic.getContent(this));

            String url = topic.getImageUrl();
            Toast.makeText(NewTopicActivity.this, topic.getImageUrl().toString(), Toast.LENGTH_SHORT).show();
            if (url != null && !url.isEmpty()) {
                ImageView imageView = (ImageView) findViewById(R.id.image);
                Glide.with(this)
                        .load(topic.getImageUrl())
                        .crossFade()
                        .into(imageView);
                //findViewById(R.id.new_image).setVisibility(View.GONE);

            }

        }
        //end part edit topic

        // Take photo from camera

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

        // Select Image or photo from gallery
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
                            audioPath = Environment.getExternalStorageDirectory()+ "/voice.wav";
                            recordVoice.setOutputFile(audioPath);
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
                            elapsedMillis = chronometer.getBase();
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
                            audioUrl = Uri.fromFile(new File(recordVoice.getOutputFile()));
                            Toast.makeText(NewTopicActivity.this, "voice saved",Toast.LENGTH_SHORT).show();
                            controlPlayer();

                        dialog.dismiss();
                    }
                });
                dialog.findViewById(R.id.btn_cancel).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        recordVoice.deleteFile();
                        audioUrl = null;
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

        spinnerType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                 if(position == 0) typeVoice = "Type 1";
                 if(position == 1) typeVoice = "Type 2";
                 if(position == 2) typeVoice = "Type 3";

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        for(int i=1; i<14; i++) {
            final ImageButton back =(ImageButton)findViewById(R.id.back+i);
            back.setClipToOutline(true);
            back.setOnClickListener(this);
        }

    }// end method SetupInputs()

    private static final int TAKE_PICTURE = 1;
    private static final int PICK_IMAGE_REQUEST = 2;
    Uri imageUrl;
    Uri audioUrl;


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK)
            switch (requestCode){
                case PICK_IMAGE_REQUEST:
                    imageUrl = data.getData();
                    //Uri selectedImage = data.getData();
                    isFromActtivityResult = true;
                    imageView.setImageURI(imageUrl);
                    break;
                case TAKE_PICTURE:
                    imageUrl = Uri.fromFile(new File(mCurrentPhotoPath));
                    imageView.setImageURI(imageUrl);
                    isFromActtivityResult = true;
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
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Uploading...");
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageReference = storage.getReference();

        /*if(content != null && !content.isEmpty()){
            sendAndFinish(topicId, "", content, "", "", null);
        }else{
            Toast.makeText(NewTopicActivity.this, "please write some text!", Toast.LENGTH_SHORT).show();
        }*/

       /*if(imageUrl.toString() != null){

            if(isFromActtivityResult){ //imageUri from gallery or camera

                StorageReference ref = storageReference.child("images/" + UUID.randomUUID().toString());

                ref.putFile(imageUrl).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
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
            }else{//imageUri from firebase
                sendAndFinish(topicId, "", content, imageUrl.toString(), "", null);
            }
        }else{

        }*/

        //if(audioUrl != null){  //audio from device
            StorageReference ref = storageReference.child("sounds/" + UUID.randomUUID().toString());

            ref.putFile(audioUrl).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    taskSnapshot.getStorage().getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                        @Override
                        public void onComplete(@NonNull Task<Uri> task) {
                            progressDialog.dismiss();
                            String downloadUrl = task.getResult().toString();
                            sendAndFinish(topicId, "", content, "", downloadUrl, null);
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

       // }



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

    ///////////////////// for record
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
    /////////////

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
        isFromActtivityResult = false;

    }

    private void controlPlayer(){

        Chronometer  chronometer = (Chronometer)findViewById(R.id.chronometer_player);
        chronometer.setBase(elapsedMillis);
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

                    btn_paly_pause.setBackgroundResource(R.drawable.ic_pause_black_24dp);
                    if(typeVoice == "Type 1") playSound(1.3f);
                    if(typeVoice == "Type 2") playSound(1.8f);
                    if(typeVoice == "Type 3") playSound(0.9f);

                    try {
                        Toast.makeText(NewTopicActivity.this, ""+recordVoice.getCurrentPos(),Toast.LENGTH_SHORT).show();
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                btn_paly_pause.setBackgroundResource(R.drawable.ic_play_arrow_black_24dp);
                            }
                        },recordVoice.getCurrentPos());

                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                    isPlaying = true;
                }else{

                    stopSound();
                    btn_paly_pause.setBackgroundResource(R.drawable.ic_play_arrow_black_24dp);
                    isPlaying = false;
                }
            }
        });

    }


    public void playSound(final float r) {

        Thread streamThread = new Thread(new Runnable() {

            @Override
            public void run() {


                soundPool = new SoundPool(1,AudioManager.STREAM_MUSIC, 0);
                soundId = soundPool.load(recordVoice.getOutputFile(), 1);
                soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
                    @Override
                    public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                        soundPool.play(soundId,100, 100, 0, 0, r);
                    }
                });

            }
        });

        streamThread.start();

    }

    private void stopSound(){
        soundPool.stop(soundId);
    }

    private void pauseSound(){
        soundPool.pause(soundId);
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



}





