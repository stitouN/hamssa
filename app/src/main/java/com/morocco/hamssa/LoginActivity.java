package com.morocco.hamssa;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;
import com.google.gson.Gson;
import com.morocco.hamssa.adapters.LoginFragmentPagerAdapter;
import com.morocco.hamssa.adapters.TopicsFragmentPagerAdapter;
import com.morocco.hamssa.data.Database;
import com.morocco.hamssa.utils.Constants;
import com.morocco.hamssa.utils.HTTPTask;
import com.morocco.hamssa.utils.NameValuePair;
import com.morocco.hamssa.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = LoginActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        setupActionBar();
        setupViewPager();
    }

    private void setupActionBar(){
        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        setTitle(getString(R.string.login));
    }

    ViewPager viewPager;
    LoginFragmentPagerAdapter fragmentPagerAdapter;
    private void setupViewPager(){
        fragmentPagerAdapter = new LoginFragmentPagerAdapter(getSupportFragmentManager(), this);

        viewPager = (ViewPager) findViewById(R.id.viewpager);
        viewPager.setAdapter(fragmentPagerAdapter);

        final TabLayout tabLayout = (TabLayout) findViewById(R.id.sliding_tabs);
        tabLayout.setupWithViewPager(viewPager);
    }

    static final String LAST_TIME = "last_time_user_info_retrieved";


    public static boolean parseUserInfo(Context context, JSONObject jsonObject){
        try {
            if (jsonObject.has("user_id")) {
                String id = jsonObject.getString("user_id");
                String token = jsonObject.getString("user_token");
                Long role = 0L;
                /*if (user.has("role")) {
                    role = user.getLong("role");
                }*/
                String email = null;
                if(jsonObject.has("email")){
                    email = jsonObject.getString("email");
                }
                String imageUrl = null;
                if(jsonObject.has("imageUrl")){
                    imageUrl = jsonObject.getString("imageUrl");
                }
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
                sp.edit().putString(Constants.SP.USER_ID, id)
                        .putString(Constants.SP.USER_TOKEN, token)
                        .putLong(Constants.SP.USER_ROLE, role)
                        .putString(Constants.SP.USER_EMAIL, email)
                        .putString(Constants.SP.USER_IMAGE_URL, imageUrl)
                        .apply();
                return true;
            }
        } catch (JSONException e) {}
        return false;
    }

    // Called on onCreate of main activity to retrieve user info
    // in case it has changed from another device

    static public void fetchMyUserInfo(final Context context){
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        FirebaseFunctions functions = FirebaseFunctions.getInstance();
        Long prevTime = sp.getLong(LAST_TIME, 0L);
        final Long currentTime = new Date().getTime();
        if(currentTime - prevTime > 1000 * 60 * 5 /*5 minutes*/){
            List<NameValuePair> params = HTTPTask.getParams(context);
            Map<String, Object> data=new HashMap<>();
            data.put("token",params.get(1).getValue());
            Constants.TASK task = Constants.TASK.GET_MY_USER_INFO;
            functions.getHttpsCallable(task.toString()).call(data).addOnCompleteListener(new OnCompleteListener<HttpsCallableResult>() {
                @Override
                public void onComplete(@NonNull Task<HttpsCallableResult> task) {
                    Gson gson=new Gson();
                    if(task.isSuccessful()){
                    String result = gson.toJson(task.getResult().getData());
                    try {
                        JSONObject jsonObject = new JSONObject(result);
                        if(jsonObject!=null && jsonObject.has("username")){
                            if (parseUserInfo(context, jsonObject)) {
                                sp.edit().putLong(LAST_TIME, currentTime).apply();
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }}
                }
            });
           /* new HTTPTask(context, task, new HTTPTask.Callback() {
                @Override
                public void parseResponse(JSONObject jsonObject) {
                    if (Utils.isSuccess(jsonObject)) {
                        if (parseUserInfo(context, jsonObject)) {
                            sp.edit().putLong(LAST_TIME, currentTime).apply();
                        }
                    }
                }
            }).executeParallel(params);*/
        }
    }

    public static void endLogin(Context context){
        context.startActivity(new Intent(context, MainActivity.class));
    }

    public static void logout(Context context){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String token = prefs.getString(Constants.SP.GCM_REGISTRATION_ID, "");

        List<NameValuePair> params = HTTPTask.getParams(context);
        params.add(new NameValuePair("notification_platform", "android"));
        params.add(new NameValuePair("notification_token", token));

        // Best effort: do not wait for the webservice to return
        new HTTPTask(context, Constants.TASK.LOGOUT, new HTTPTask.Callback() {
            @Override
            public void parseResponse(JSONObject jsonObject){
                if(Utils.isSuccess(jsonObject)){
                    Log.i(TAG, "Logout successful");
                }
            }
        }).executeParallel(params);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().clear().apply();
        Database db = new Database(context);
        db.clearAllTables();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_login, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        return super.onOptionsItemSelected(item);
    }
}
