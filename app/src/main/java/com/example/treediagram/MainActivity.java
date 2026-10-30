package com.example.treediagram;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WebView webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);

        // Agar alert() di index.html tampil
        webView.setWebChromeClient(new WebChromeClient());

        // Jembatan: JavaScript memanggil Android.saveDiagram(...)
        webView.addJavascriptInterface(new AndroidBridge(), "Android");

        webView.loadUrl("file:///android_asset/index.html");
        setContentView(webView);
    }

    private void showToast(final String message) {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show());
    }

    public class AndroidBridge {

        @JavascriptInterface
        public void saveDiagram(String json) {
            String name = "diagram-"
                    + new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date())
                    + ".json";
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Android 10 ke atas: simpan ke folder Download, tanpa izin tambahan
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Downloads.DISPLAY_NAME, name);
                    values.put(MediaStore.Downloads.MIME_TYPE, "application/json");
                    values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                    Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                    if (uri == null) throw new Exception("tidak bisa membuat file");
                    try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                        out.write(bytes);
                    }
                    showToast("Tersimpan di Download: " + name);
                } else {
                    // Android 7-9: simpan ke folder khusus aplikasi
                    File dir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
                    if (dir == null) dir = getFilesDir();
                    if (!dir.exists()) dir.mkdirs();
                    File file = new File(dir, name);
                    try (OutputStream out = new FileOutputStream(file)) {
                        out.write(bytes);
                    }
                    showToast("Tersimpan: " + file.getAbsolutePath());
                }
            } catch (Exception e) {
                showToast("Gagal menyimpan: " + e.getMessage());
            }
        }
    }
}
