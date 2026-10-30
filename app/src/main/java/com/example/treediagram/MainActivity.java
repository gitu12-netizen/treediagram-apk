package com.example.treediagram;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;

public class MainActivity extends Activity {

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WebView webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);

        // Agar alert() di index.html tampil
        webView.setWebChromeClient(new WebChromeClient());

        // Memuat index.html dari folder assets di dalam APK
        webView.loadUrl("file:///android_asset/index.html");

        setContentView(webView);
    }
}
