package com.morocco.hamssa;

import android.app.Activity;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;
import com.google.gson.Gson;
import com.morocco.hamssa.adapters.MessageCursorRecyclerViewAdapter;
import com.morocco.hamssa.data.Database;
import com.morocco.hamssa.utils.Constants;
import com.morocco.hamssa.utils.HTTPTask;
import com.morocco.hamssa.utils.ImageUploader;
import com.morocco.hamssa.utils.ImageUtils;
import com.morocco.hamssa.utils.NameValuePair;
import com.morocco.hamssa.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ConfigurationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuration);


        setupActionBar();
        setupProfileImage();
        setupEmail();
        setupNotifications();
        setupLogout();
        ButtonChangeLanguage();

    }



    private void setupActionBar(){
        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }
        setTitle(getString(R.string.action_configuration));
        mToolbar.setTitleTextColor(getResources().getColor(android.R.color.white));
    }

    private void setupLogout(){
        final Context context = this;

        findViewById(R.id.logout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
                String email = sp.getString(Constants.SP.USER_EMAIL, "");
                if(!email.equalsIgnoreCase("")){
                    logout(context);
                }else{
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setTitle(R.string.warning)
                            .setMessage(R.string.logout_warning)
                            .setNegativeButton(R.string.back, null)
                            .setPositiveButton(R.string.continue_anyway, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    logout(context);
                                }
                            }).show();
                }
            }
        });
    }
    private void logout(Context context){
        LoginActivity.logout(context);
        Intent intent = new Intent(context, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    static final public int REQUEST_IMAGE_PICK = 1;
    private void setupProfileImage(){
        ImageView imageView = (ImageView)findViewById(R.id.image);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        String imageUrl = sp.getString(Constants.SP.USER_IMAGE_URL, null);
        if(imageUrl != null && !imageUrl.isEmpty()){
            Glide.with(this)
                    .using(new MessageCursorRecyclerViewAdapter.MyUrlLoader(this))
                    .load(new MessageCursorRecyclerViewAdapter.MyDataModel(imageUrl))
                    .asBitmap()
                    .centerCrop()
                    .into(new BitmapImageViewTarget(imageView) {
                        @Override
                        protected void setResource(Bitmap bitmap) {
                            super.setResource(ImageUtils.getRoundImage(bitmap, true));
                        }
                    });
        }
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(intent, REQUEST_IMAGE_PICK);
                }
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);
        if(requestCode == REQUEST_IMAGE_PICK && resultCode == Activity.RESULT_OK){
            Bitmap bitmap = Utils.processActivityResult(this, imageReturnedIntent, 600);
            sendProfileImage(bitmap);
        }
    }

    private void sendProfileImage(final Bitmap bitmap){
        new ImageUploader(this, true, new ImageUploader.OnReceiveJSON(){
            @Override
            public void onJSON(JSONObject jsonObject) {
                if(jsonObject != null){
                    try {
                        String url = jsonObject.getString("servingURL");
                        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ConfigurationActivity.this);
                        sp.edit().putString(Constants.SP.USER_IMAGE_URL, url).apply();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    setupProfileImage();
                }else{
                    Toast.makeText(ConfigurationActivity.this, getString(R.string.connection_error), Toast.LENGTH_SHORT).show();
                }
            }
        }).upload(bitmap);
    }


    private void setupEmail(){
        final Context context = this;
        findViewById(R.id.email_help).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(context)
                        .setTitle(getString(R.string.email_link_title))
                        .setMessage(getString(R.string.email_link_info))
                        .setPositiveButton(android.R.string.yes, null)
                        .show();
            }
        });

        final View wrapper1 = findViewById(R.id.email_bind1);
        final View wrapper2 = findViewById(R.id.email_bind2);

        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        String email = sp.getString(Constants.SP.USER_EMAIL, "");
        final TextView textView = (TextView)findViewById(R.id.current_email);
        if(!email.isEmpty()) {
            textView.setText(email);
        }
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                wrapper1.setVisibility(View.GONE);
                wrapper2.setVisibility(View.VISIBLE);
            }
        });
        final EditText editText = (EditText)findViewById(R.id.email);
        editText.setText(email);

        findViewById(R.id.send_email).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String email = editText.getText().toString();
                if(!email.isEmpty()){
                    FirebaseFunctions functions = FirebaseFunctions.getInstance();
                    List<NameValuePair> params = HTTPTask.getParams(context);
                    Map<String, Object> data = new HashMap<>();
                    data.put("token", params.get(1).getValue());
                    data.put("email",email);
                    Constants.TASK task = Constants.TASK.SET_EMAIL;

                    final ProgressDialog progressDialog = ProgressDialog.show(context, getString(R.string.please_wait), getString(R.string.connecting_with_server), true);
                    functions.getHttpsCallable(task.toString()).call(data).addOnCompleteListener(new OnCompleteListener<HttpsCallableResult>() {
                        @Override
                        public void onComplete(@NonNull Task<HttpsCallableResult> task) {
                            Gson gson = new Gson();
                            if(task.isSuccessful()) {
                                String result = gson.toJson(task.getResult().getData());
                                Cursor cursor = null;
                                try {
                                    JSONObject jsonObject = new JSONObject(result);
                                    if (jsonObject != null) {
                                        LoginActivity.parseUserInfo(context, jsonObject);
                                        progressDialog.dismiss();
                                        new AlertDialog.Builder(context)
                                                .setTitle(getString(R.string.password_sent_title))
                                                .setMessage(getString(R.string.password_sent))
                                                .setPositiveButton(android.R.string.yes, null)
                                                .show();
                                        String email = sp.getString(Constants.SP.USER_EMAIL, "");
                                        textView.setText(email);
                                        editText.setText(email);
                                        wrapper1.setVisibility(View.VISIBLE);
                                        wrapper2.setVisibility(View.GONE);
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }else{
                                String message = getString(R.string.connection_error);
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                            }
                        }});
                  /*  new HTTPTask(context, task, new HTTPTask.Callback() {
                        boolean success = false;
                        @Override
                        public void parseResponse(JSONObject jsonObject){
                            if(Utils.isSuccess(jsonObject)){
                                success = true;
                                LoginActivity.parseUserInfo(context, jsonObject);
                            }
                        }
                        @Override
                        public void onDataReceived(JSONObject jsonObject) {
                            progressDialog.dismiss();
                            if(success) {
                                new AlertDialog.Builder(context)
                                        .setTitle(getString(R.string.password_sent_title))
                                        .setMessage(getString(R.string.password_sent))
                                        .setPositiveButton(android.R.string.yes, null)
                                        .show();
                                String email = sp.getString(Constants.SP.USER_EMAIL, "");
                                textView.setText(email);
                                editText.setText(email);
                                wrapper1.setVisibility(View.VISIBLE);
                                wrapper2.setVisibility(View.GONE);
                            }else{
                                String message = getString(R.string.connection_error);
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                            }
                            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
                        }
                    }).executeParallel(params);*/
                }
            }
        });
    }

    private void setupNotifications(){
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        boolean notifyFollowing = sp.getBoolean(Constants.SP.NOTIFY_FOLLOWING, true);
        boolean notifyReferences = sp.getBoolean(Constants.SP.NOTIFY_REFERENCES, true);
        boolean notifyVotes = sp.getBoolean(Constants.SP.NOTIFY_VOTES, true);

        CheckBox checkBoxFollowing = (CheckBox)findViewById(R.id.notify_following_checkbox);
        CheckBox checkBoxReferences = (CheckBox)findViewById(R.id.notify_references_checkbox);
        CheckBox checkBoxVotes = (CheckBox)findViewById(R.id.notify_votes_checkbox);

        checkBoxFollowing.setChecked(notifyFollowing);
        checkBoxReferences.setChecked(notifyReferences);
        checkBoxVotes.setChecked(notifyVotes);

        checkBoxFollowing.setTag(Constants.SP.NOTIFY_FOLLOWING);
        checkBoxReferences.setTag(Constants.SP.NOTIFY_REFERENCES);
        checkBoxVotes.setTag(Constants.SP.NOTIFY_VOTES);

        CompoundButton.OnCheckedChangeListener listener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                sp.edit().putBoolean((String)buttonView.getTag(), isChecked).apply();
            }
        };

        checkBoxFollowing.setOnCheckedChangeListener(listener);
        checkBoxReferences.setOnCheckedChangeListener(listener);
        checkBoxVotes.setOnCheckedChangeListener(listener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_configuration, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        return super.onOptionsItemSelected(item);
    }

    private void ButtonChangeLanguage(){

        findViewById(R.id.btn_change_language).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ShowChangeLanguageDialog();
            }
        });



    }



    private void ShowChangeLanguageDialog() {

        final String[] listLanguages = getResources().getStringArray(R.array.listOfLanguages);
        final String[] listValue = {};
        AlertDialog.Builder builder = new AlertDialog.Builder(ConfigurationActivity.this);
        builder.setTitle(getString(R.string.choose_languages))
                .setSingleChoiceItems(listLanguages, -1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        if(i == 0) saveLanguage("en");
                        if(i == 1) saveLanguage("ar");
                        if(i == 2) saveLanguage("es");
                        if(i == 3) saveLanguage("fr");

                    }
                });

        builder.setPositiveButton(getString(R.string.text_ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                dialogInterface.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }




    public void saveLanguage(String lang){
        SharedPreferences.Editor editor = getSharedPreferences("Settings",0).edit();
        editor.putString("My_lang", lang);
        editor.commit();
    }


}
