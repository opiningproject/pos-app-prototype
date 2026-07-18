package com.opining.pos;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.sunmi.peripheral.printer.InnerPrinterCallback;
import com.sunmi.peripheral.printer.InnerPrinterException;
import com.sunmi.peripheral.printer.InnerPrinterManager;
import com.sunmi.peripheral.printer.SunmiPrinterService;

public class MainActivity extends Activity {

    private static final String APP_URL = "https://opiningproject.github.io/pos-app-prototype/";

    private WebView webView;
    private SunmiPrinterService printerService;

    private final InnerPrinterCallback printerCallback = new InnerPrinterCallback() {
        @Override
        protected void onConnected(SunmiPrinterService service) {
            printerService = service;
        }

        @Override
        protected void onDisconnected() {
            printerService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // White status bar with dark icons (matches the app header)
        getWindow().setStatusBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        // Bind to the SUNMI built-in printer service
        try {
            InnerPrinterManager.getInstance().bindService(getApplicationContext(), printerCallback);
        } catch (InnerPrinterException e) {
            e.printStackTrace();
        }

        webView = new WebView(this);
        setContentView(webView);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);

        webView.addJavascriptInterface(new PrinterBridge(), "AndroidPrinter");
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl(APP_URL);
    }

    /** Exposed to the web app as window.AndroidPrinter */
    private class PrinterBridge {

        @JavascriptInterface
        public boolean isReady() {
            return printerService != null;
        }

        /**
         * Print a receipt from a JSON array of simple commands:
         * {t:"align", v:"left|center|right"}, {t:"size", v:24},
         * {t:"text", v:"..."}, {t:"feed", n:3}, {t:"cut"}
         */
        @JavascriptInterface
        public void printReceipt(String cmdsJson) {
            if (printerService == null || cmdsJson == null) {
                return;
            }
            try {
                org.json.JSONArray arr = new org.json.JSONArray(cmdsJson);
                try { printerService.printerInit(null); } catch (Exception ignored) {}
                for (int i = 0; i < arr.length(); i++) {
                    org.json.JSONObject c = arr.getJSONObject(i);
                    String t = c.optString("t");
                    try {
                        if ("align".equals(t)) {
                            String v = c.optString("v");
                            int a = "center".equals(v) ? 1 : ("right".equals(v) ? 2 : 0);
                            printerService.setAlignment(a, null);
                        } else if ("size".equals(t)) {
                            printerService.setFontSize((float) c.optDouble("v", 24), null);
                        } else if ("bold".equals(t)) {
                            byte on = (byte) (c.optBoolean("v", false) ? 1 : 0);
                            printerService.sendRAWData(new byte[]{0x1B, 0x45, on}, null);
                        } else if ("text".equals(t)) {
                            printerService.printText(c.optString("v"), null);
                        } else if ("cols".equals(t)) {
                            String left = c.optString("left");
                            String right = c.optString("right");
                            int wl = c.optInt("wl", 32);
                            int wr = c.optInt("wr", 16);
                            printerService.printColumnsString(
                                    new String[]{left, right},
                                    new int[]{wl, wr},
                                    new int[]{0, 2},
                                    null);
                        } else if ("feed".equals(t)) {
                            printerService.lineWrap(c.optInt("n", 1), null);
                        } else if ("cut".equals(t)) {
                            printerService.cutPaper(null);
                        }
                    } catch (Exception ignored) {
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @JavascriptInterface
        public void printBitmap(String dataUrl) {
            if (printerService == null || dataUrl == null) {
                return;
            }
            try {
                String base64 = dataUrl.replaceFirst("^data:image/[a-zA-Z]+;base64,", "");
                byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (bitmap == null) {
                    return;
                }
                printerService.printBitmap(bitmap, null);
                printerService.lineWrap(3, null);
                try {
                    printerService.cutPaper(null);
                } catch (Exception ignored) {
                    // Not all SUNMI models have an auto-cutter
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        try {
            InnerPrinterManager.getInstance().unBindService(getApplicationContext(), printerCallback);
        } catch (Exception ignored) {
        }
        super.onDestroy();
    }
}
