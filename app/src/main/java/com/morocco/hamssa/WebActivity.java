package com.morocco.hamssa;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.webkit.WebView;
import android.widget.Toast;

import java.net.MalformedURLException;
import java.net.URL;

public class WebActivity extends AppCompatActivity {

    public static final String ARG_URL = "arg_url";
    public static final String ARG_TITLE = "arg_title";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        String title = getIntent().getStringExtra(ARG_TITLE);
        if(title != null){
            getSupportActionBar().setTitle(title);
        }

        WebView webView  = (WebView)findViewById(R.id.webview);
        webView.getSettings().setJavaScriptEnabled(true);

        String urlString = getIntent().getStringExtra(ARG_URL);

        webView.loadUrl(urlString);
    }

}
