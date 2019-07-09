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
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
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
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.gson.Gson;
import com.morocco.hamssa.data.Database;
import com.morocco.hamssa.entities.Topic;
import com.morocco.hamssa.utils.Constants;
import com.morocco.hamssa.utils.HTTPTask;
import com.morocco.hamssa.utils.ImageUploader;
import com.morocco.hamssa.utils.ImageUtils;
import com.morocco.hamssa.utils.NameValuePair;
import com.morocco.hamssa.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class NewTopicActivity extends AppCompatActivity {

    public static final String ARG_TOPIC_ID = "arg_topic_id";
    Topic topic;
    private String mCurrentPhotoPath;
    private int REQUEST_PERMISSION_REQ_CODE=2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_topic);

        if(getIntent().hasExtra(ARG_TOPIC_ID)) {
            String topicId = getIntent().getStringExtra(ARG_TOPIC_ID);
            topic = new Database(this).getTopic(topicId);
        }

        setupActionBar();
        setupInputs();
    }

    private void setupActionBar(){
        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            if(topic != null){
                actionBar.setTitle(getString(R.string.editing));
            }
        }
    }


    Bitmap bitmap = null;
    private static final int TAKE_PICTURE = 1;
    private int PICK_IMAGE_REQUEST=2;
    Uri imageUrl;
    File image;
    private void setupInputs(){
        if(topic != null){
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
            if(url != null && !url.isEmpty()){
                findViewById(R.id.image).setVisibility(View.VISIBLE);
                ImageView imageView = (ImageView)findViewById(R.id.image);
                Glide.with(this)
                        .load(topic.getImageUrl())
                        .crossFade()
                        .into(imageView);
                findViewById(R.id.new_image).setVisibility(View.GONE);

            }
        }

        findViewById(R.id.imageView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent =  new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                if (ContextCompat.checkSelfPermission(NewTopicActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(NewTopicActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION_REQ_CODE);
                }
                if (ContextCompat.checkSelfPermission(NewTopicActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED && intent.resolveActivity(getPackageManager()) != null) {
                    // Create the File where the photo should go
                    File photoFile = null;
                    try {
                        photoFile = createImageFile();
                    } catch (IOException ex) {
                        // Error occurred while creating the File
                    }
                    // Continue only if the File was successfully created
                    if (photoFile != null) {
                        Uri photoURI = FileProvider.getUriForFile(NewTopicActivity.this,
                                "com.morocco.hamssa.fileprovider",
                                photoFile);

                        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                        startActivityForResult(intent, TAKE_PICTURE);

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
        findViewById(R.id.image).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bitmap = null;
                findViewById(R.id.image).setVisibility(View.GONE);
                findViewById(R.id.new_image).setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);
        if(requestCode == TAKE_PICTURE && resultCode == Activity.RESULT_OK){
            imageUrl=Uri.parse(mCurrentPhotoPath);
            bitmap = Utils.processActivityResult(this, imageReturnedIntent, 800);
            ImageView imageView = (ImageView)findViewById(R.id.image);
            imageView.setImageURI(imageUrl);
            imageView.setVisibility(View.VISIBLE);
            findViewById(R.id.new_image).setVisibility(View.GONE);
        }else if(requestCode==PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK ){
            imageUrl=imageReturnedIntent.getData();
            bitmap = Utils.processActivityResult(this, imageReturnedIntent, 800);
            ImageView imageView = (ImageView)findViewById(R.id.image);
            imageView.setImageBitmap(bitmap);
            imageView.setVisibility(View.VISIBLE);
            findViewById(R.id.new_image).setVisibility(View.GONE);
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

        if(id == R.id.action_send){
            send();
        }

        return super.onOptionsItemSelected(item);
    }

    private void send(){
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageReference = storage.getReference();
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Uploading...");
        EditText editText1 = (EditText)findViewById(R.id.description);
        final String description = editText1.getText().toString();
        EditText editText2 = (EditText)findViewById(R.id.content);
        final String content = editText2.getText().toString();
        final String topicId = topic != null ? topic.getId() : null;
      //  final String imageUrl = topic != null ? topic.getImageUrl() : null;
        if(!description.isEmpty()) {
            if(imageUrl == null) {
                sendAndFinish(topicId, description, content, "", null);
            }else{
                final Context context = this;
                StorageReference ref = storageReference.child("images/"+ UUID.randomUUID().toString());
               /* StorageMetadata metadata = new StorageMetadata.Builder()
                        .setCustomMetadata("clientId", Constants.CLIENT_TOKEN)
                        .build();
                ref.*/
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
                                        sendAndFinish(topicId, description, content, downloadUrl, "");
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


    private void sendAndFinish(String topicId, String description, String content, String imageUrl, String blobKey){
        List<NameValuePair> params = HTTPTask.getParams(this);
        FirebaseFunctions functions = FirebaseFunctions.getInstance();
        Map<String, Object> data=new HashMap<>();
        data.put("token",params.get(1).getValue());
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
                if(task.isSuccessful()){
                String result = gson.toJson(task.getResult().getData());
                boolean success = false;
                int messageId = R.string.connection_error;
                    try {
                        JSONObject jsonObject = new JSONObject(result);
                        if (jsonObject != null) {
                        JSONArray jsonArray = new JSONArray();
                        JSONArray keys=jsonObject.names();
                        Utils.constructTopicsJsonArray(jsonObject, jsonArray, keys);
                        Database db = new Database(context);
                        db.addTopics(jsonArray);
                        success = true;
                    }else if(jsonObject != null && jsonObject.has("error")) {

                            int error = jsonObject.getInt("error");
                            if (error == 2) {
                                messageId = R.string.you_dont_have_permission;
                            } else if (error == 3) {
                                messageId = R.string.you_are_not_the_owner_of_this_topic;
                            }
                    }
                    progressDialog.dismiss();
                    if(success){
                        finish();
                    }else{
                        Toast.makeText(context, getString(messageId), Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }}else{
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
}
