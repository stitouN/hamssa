package com.morocco.hamssa;

import android.app.Application;

import com.facebook.FacebookSdk;

/**
 * Created by hmontaner on 13/10/15.
 */
public class MyApplication extends Application {
    @Override
    public void onCreate(){
        super.onCreate();
        FacebookSdk.sdkInitialize(getApplicationContext());
    }
}
