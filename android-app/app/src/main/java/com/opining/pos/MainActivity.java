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
