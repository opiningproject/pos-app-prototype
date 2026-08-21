package com.opining.pos;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.view.WindowManager;
import android.net.http.SslError;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.sunmi.peripheral.printer.InnerPrinterCallback;
import com.sunmi.peripheral.printer.InnerPrinterException;
import com.sunmi.peripheral.printer.InnerPrinterManager;
import com.sunmi.peripheral.printer.SunmiPrinterService;

public class MainActivity extends Activity {

    private static final String LOCAL_URL = "file:///android_asset/index.html";
    private static final String ONLINE_URL = "https://admin.dryfftjwieiwjw.online/";

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

        // Keep screen awake while app is in foreground
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

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
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setAllowFileAccessFromFileURLs(true);
        s.setAllowUniversalAccessFromFileURLs(true);
        s.setMediaPlaybackRequiresUserGesture(false);

        webView.addJavascriptInterface(new PrinterBridge(), "AndroidPrinter");
        webView.addJavascriptInterface(new StatusBarBridge(), "AndroidStatusBar");
        webView.addJavascriptInterface(new AuthBridge(), "AndroidAuth");
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed(); // Handle certificate mismatches gracefully
            }
        });
        webView.loadUrl(LOCAL_URL);
    }

    /** Exposed to the web app as window.AndroidStatusBar: recolour the system status bar */
    private class StatusBarBridge {
        @JavascriptInterface
        public void setColor(final String hex, final boolean lightIcons) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        getWindow().setStatusBarColor(Color.parseColor(hex));
                        View dv = getWindow().getDecorView();
                        int flags = dv.getSystemUiVisibility();
                        if (lightIcons) {
                            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR; // light icons for a dark bar
                        } else {
                            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;  // dark icons for a light bar
                        }
                        dv.setSystemUiVisibility(flags);
                    } catch (Exception ignored) {
                    }
                }
            });
        }
    }

    /** Exposed to the web app as window.AndroidAuth */
    private class AuthBridge {
        private final ApiClient apiClient = new ApiClient();

        private String extractToken(org.json.JSONObject response) {
            if (response == null) return "session_active";
            if (response.has("token") && !response.isNull("token")) {
                String t = response.optString("token", "");
                if (!t.isEmpty()) return t;
            }
            if (response.has("data") && !response.isNull("data")) {
                org.json.JSONObject dataObj = response.optJSONObject("data");
                if (dataObj != null) {
                    if (dataObj.has("token") && !dataObj.isNull("token")) {
                        String t = dataObj.optString("token", "");
                        if (!t.isEmpty()) return t;
                    }
                    if (dataObj.has("access_token") && !dataObj.isNull("access_token")) {
                        String t = dataObj.optString("access_token", "");
                        if (!t.isEmpty()) return t;
                    }
                }
            }
            if (response.has("access_token") && !response.isNull("access_token")) {
                String t = response.optString("access_token", "");
                if (!t.isEmpty()) return t;
            }
            return "session_active";
        }

        private String escapeJsString(String str) {
            if (str == null) return "";
            return str.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n").replace("\r", "");
        }

        @JavascriptInterface
        public void login(final String email, final String password) {
            apiClient.login(email, password, new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(org.json.JSONObject response) {
                    final String token = extractToken(response);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            webView.evaluateJavascript("javascript:if(typeof onNativeLoginSuccess === 'function') onNativeLoginSuccess('" + escapeJsString(token) + "');", null);
                        }
                    });
                }

                @Override
                public void onError(final Exception error) {
                    final String message = error != null && error.getMessage() != null ? error.getMessage() : "Login failed";
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            webView.evaluateJavascript("javascript:if(typeof onNativeLoginError === 'function') onNativeLoginError('" + escapeJsString(message) + "');", null);
                        }
                    });
                }
            });
        }

        @JavascriptInterface
        public void changeOrderStatus(final String orderId, final String orderStatus, final String token) {
            apiClient.changeOrderStatus(orderId, orderStatus, token, new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(org.json.JSONObject response) {
                    android.util.Log.d("AuthBridge", "Native changeOrderStatus success: " + (response != null ? response.toString() : "ok"));
                }

                @Override
                public void onError(Exception error) {
                    android.util.Log.e("AuthBridge", "Native changeOrderStatus error", error);
                }
            });
        }

        @JavascriptInterface
        public void cancelOrder(final String orderId, final String status, final String token) {
            apiClient.cancelOrder(orderId, status, token, new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(org.json.JSONObject response) {
                    android.util.Log.d("AuthBridge", "Native cancelOrder success: " + (response != null ? response.toString() : "ok"));
                }

                @Override
                public void onError(Exception error) {
                    android.util.Log.e("AuthBridge", "Native cancelOrder error", error);
                }
            });
        }

        @JavascriptInterface
        public void getCategories(final String token) {
            apiClient.getCategories(token, new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(org.json.JSONObject response) {
                    android.util.Log.d("AuthBridge", "Native getCategories success: " + (response != null ? response.toString() : "ok"));
                }

                @Override
                public void onError(Exception error) {
                    android.util.Log.e("AuthBridge", "Native getCategories error", error);
                }
            });
        }

        @JavascriptInterface
        public void changeDishStatus(final String dishId, final int status, final String token) {
            apiClient.changeDishStatus(dishId, status, token, new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(org.json.JSONObject response) {
                    android.util.Log.d("AuthBridge", "Native changeDishStatus success: " + (response != null ? response.toString() : "ok"));
                }

                @Override
                public void onError(Exception error) {
                    android.util.Log.e("AuthBridge", "Native changeDishStatus error", error);
                }
            });
        }
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
