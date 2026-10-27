package com.cleanframe.launcher;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.bluetooth.BluetoothAdapter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewClientCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MainActivity extends Activity {
    private static final String START_URL = "https://appassets.androidplatform.net/assets/index.html";
    private WebView web;
    private final Map<String, byte[]> iconCache = new HashMap<>();

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        web = new WebView(this);
        web.setBackgroundColor(Color.BLACK);
        web.setFocusable(true);
        web.setFocusableInTouchMode(true);
        setContentView(web);
        hideSystemBars();

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(false);

        final WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();
        // Local assets only: this launcher never needs the network.
        web.setWebViewClient(new WebViewClientCompat() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
                Uri u = r.getUrl();
                if ("appassets.androidplatform.net".equals(u.getHost())) {
                    String path = u.getPath() == null ? "" : u.getPath();
                    if (path.startsWith("/appicon/")) return iconResponse(path.substring(9));
                    return loader.shouldInterceptRequest(u);
                }
                return new WebResourceResponse("text/plain", "utf-8", 403, "Blocked", null, new ByteArrayInputStream(new byte[0]));
            }
        });
        web.addJavascriptInterface(new Bridge(), "AndroidBridge");
        web.loadUrl(START_URL);
        web.requestFocus();
    }

    private void hideSystemBars() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemBars();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (web != null) web.evaluateJavascript("document.dispatchEvent(new Event('visibilitychange'))", null);
    }

    /** The remote's Back key goes to the page, which has its own "Leave Home?" flow. */
    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            if (e.getAction() == KeyEvent.ACTION_UP && web != null)
                web.evaluateJavascript("window.__androidBack && window.__androidBack()", null);
            return true;
        }
        return super.dispatchKeyEvent(e);
    }

    private WebResourceResponse iconResponse(String file) {
        String pkg = file.endsWith(".png") ? file.substring(0, file.length() - 4) : file;
        byte[] png = iconCache.get(pkg);
        if (png == null) {
            try {
                PackageManager pm = getPackageManager();
                Drawable d = pm.getApplicationIcon(pkg);
                int size = 192;
                Bitmap b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
                Canvas c = new Canvas(b);
                d.setBounds(0, 0, size, size);
                d.draw(c);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                b.compress(Bitmap.CompressFormat.PNG, 100, out);
                png = out.toByteArray();
                iconCache.put(pkg, png);
            } catch (Exception ex) {
                return new WebResourceResponse("text/plain", "utf-8", 404, "Not found", null, new ByteArrayInputStream(new byte[0]));
            }
        }
        return new WebResourceResponse("image/png", null, new ByteArrayInputStream(png));
    }

    private void start(final Intent i) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
            }
        });
    }

    /** Tries each intent in turn on the UI thread; the first one the system accepts wins. */
    private void startFirst(final Intent... options) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                for (Intent i : options) {
                    try {
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(i);
                        return;
                    } catch (ActivityNotFoundException | SecurityException ignored) { }
                }
            }
        });
    }

    private class Bridge {
        /** Connection and Bluetooth state, for the System screen. No location permission is needed. */
        @JavascriptInterface
        public String getStatus() {
            JSONObject o = new JSONObject();
            try {
                String net = "none";
                ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                if (cm != null) {
                    Network n = cm.getActiveNetwork();
                    NetworkCapabilities c = n == null ? null : cm.getNetworkCapabilities(n);
                    if (c != null) {
                        if (c.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) net = "ethernet";
                        else if (c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) net = "wifi";
                        else net = "other";
                    }
                }
                o.put("network", net);
                BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter();
                o.put("bluetooth", bt == null ? "none" : (bt.isEnabled() ? "on" : "off"));
            } catch (Exception ignored) { }
            return o.toString();
        }

        @JavascriptInterface
        public String getInputs() {
            JSONArray out = new JSONArray();
            try {
                TvInputManager tv = (TvInputManager) getSystemService(Context.TV_INPUT_SERVICE);
                if (tv == null) return out.toString();
                for (TvInputInfo info : tv.getTvInputList()) {
                    if (!info.isPassthroughInput()) continue;
                    CharSequence custom = info.loadCustomLabel(MainActivity.this);
                    CharSequence label = (custom != null && custom.length() > 0) ? custom : info.loadLabel(MainActivity.this);
                    JSONObject o = new JSONObject();
                    o.put("id", info.getId());
                    o.put("label", String.valueOf(label));
                    o.put("type", info.getType());
                    out.put(o);
                }
            } catch (Exception ignored) { }
            return out.toString();
        }

        @JavascriptInterface
        public boolean switchInput(String inputId) {
            try {
                Uri uri = TvContract.buildChannelUriForPassthroughInput(inputId);
                startFirst(new Intent(Intent.ACTION_VIEW, uri));
                return true;
            } catch (Exception e) { return false; }
        }

        /** Opens a Settings page; falls back to the main Settings app when the TV build lacks that page. */
        @JavascriptInterface
        public boolean openSetting(String kind) {
            String action;
            switch (kind) {
                case "wifi": action = Settings.ACTION_WIFI_SETTINGS; break;
                case "about": action = Settings.ACTION_DEVICE_INFO_SETTINGS; break;
                case "bluetooth": action = Settings.ACTION_BLUETOOTH_SETTINGS; break;
                case "display": action = Settings.ACTION_DISPLAY_SETTINGS; break;
                case "sound": action = Settings.ACTION_SOUND_SETTINGS; break;
                default: action = Settings.ACTION_SETTINGS;
            }
            startFirst(new Intent(action), new Intent(Settings.ACTION_SETTINGS));
            return true;
        }

        @JavascriptInterface
        public String getApps() {
            PackageManager pm = getPackageManager();
            Set<String> seen = new HashSet<>();
            JSONArray out = new JSONArray();
            String[] cats = { Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER };
            for (String cat : cats) {
                Intent q = new Intent(Intent.ACTION_MAIN).addCategory(cat);
                for (ResolveInfo ri : pm.queryIntentActivities(q, 0)) {
                    String pkg = ri.activityInfo.packageName;
                    if (pkg.equals(getPackageName()) || !seen.add(pkg)) continue;
                    try {
                        JSONObject o = new JSONObject();
                        o.put("appId", pkg);
                        o.put("name", String.valueOf(ri.loadLabel(pm)));
                        o.put("icon", "/appicon/" + pkg + ".png");
                        out.put(o);
                    } catch (Exception ignored) { }
                }
            }
            return out.toString();
        }

        @JavascriptInterface
        public boolean launch(String pkg) {
            PackageManager pm = getPackageManager();
            Intent i = pm.getLeanbackLaunchIntentForPackage(pkg);
            if (i == null) i = pm.getLaunchIntentForPackage(pkg);
            if (i == null) return false;
            start(i);
            return true;
        }

        @JavascriptInterface
        public boolean screensaver() {
            startFirst(new Intent(Intent.ACTION_MAIN).setClassName("com.android.systemui", "com.android.systemui.Somnambulator"),
                    new Intent(Settings.ACTION_DREAM_SETTINGS), new Intent(Settings.ACTION_SETTINGS));
            return true;
        }

        @JavascriptInterface
        public boolean systemHome() {
            PackageManager pm = getPackageManager();
            Intent q = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
            for (ResolveInfo ri : pm.queryIntentActivities(q, 0)) {
                if (ri.activityInfo.packageName.equals(getPackageName())) continue;
                Intent i = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                        .setClassName(ri.activityInfo.packageName, ri.activityInfo.name);
                start(i);
                return true;
            }
            return false;
        }

        @JavascriptInterface
        public boolean settings() {
            start(new Intent(Settings.ACTION_SETTINGS));
            return true;
        }
    }
}
