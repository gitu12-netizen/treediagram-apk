package com.example.treediagram;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
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

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int REQ_PICK_FILE = 1001;
    private WebView webView;

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);   // dibutuhkan untuk autosave (localStorage)

        // Agar alert() dan confirm() di index.html tampil
        webView.setWebChromeClient(new WebChromeClient());

        // Jembatan: JavaScript memanggil Android.saveDiagram(...) dan Android.pickFile()
        webView.addJavascriptInterface(new AndroidBridge(), "Android");

        webView.loadUrl("file:///android_asset/index.html");
        setContentView(webView);
    }

    // Dipanggil setelah pengguna memilih file dari penyimpanan
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_PICK_FILE) return;

        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            callJs("onFileError", JSONObject.quote("Pemilihan file dibatalkan"));
            return;
        }

        Uri uri = data.getData();
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            if (in == null) throw new Exception("tidak bisa dibuka");
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int n;
            while ((n = in.read(chunk)) != -1) {
                buf.write(chunk, 0, n);
            }
            String text = new String(buf.toByteArray(), StandardCharsets.UTF_8);
            callJs("onFileLoaded", JSONObject.quote(text));
        } catch (Exception e) {
            callJs("onFileError", JSONObject.quote("Gagal membaca file"));
        }
    }

    // quotedArg harus sudah berupa string JavaScript yang di-quote
    private void callJs(final String fn, final String quotedArg) {
        runOnUiThread(() -> webView.evaluateJavascript("window." + fn + "(" + quotedArg + ")", null));
    }

    private void showToast(final String message) {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show());
    }

    private static String safeName(String s) {
        String clean = (s == null) ? "" : s.replaceAll("[^A-Za-z0-9 _-]", "").trim();
        return clean.isEmpty() ? "diagram" : clean;
    }

    public class AndroidBridge {

        @JavascriptInterface
        public void pickFile() {
            runOnUiThread(() -> {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/json", "text/plain"});
                MainActivity.this.startActivityForResult(intent, REQ_PICK_FILE);
            });
        }

        @JavascriptInterface
        public void saveDiagram(String json, String title) {
            String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date());
            String name = safeName(title) + "-" + stamp + ".json";
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
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
