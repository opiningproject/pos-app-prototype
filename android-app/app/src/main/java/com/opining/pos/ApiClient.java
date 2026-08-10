package com.opining.pos;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ApiClient {

    private static final String TAG = "ApiClient";
    private static final String BASE_URL = "https://9569.dryfftjwieiwjw.online";
    
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface ApiCallback {
        void onSuccess(JSONObject response);
        void onError(Exception error);
    }

    /**
     * Authenticate user with email and password.
     */
    public void login(final String email, final String password, final ApiCallback callback) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection urlConnection = null;
                try {
                    URL url = new URL(BASE_URL + "/api/v1/login");
                    urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setRequestMethod("POST");
                    urlConnection.setRequestProperty("Content-Type", "application/json; utf-8");
                    urlConnection.setRequestProperty("Accept", "application/json");
                    urlConnection.setDoOutput(true);
                    urlConnection.setConnectTimeout(10000);
                    urlConnection.setReadTimeout(10000);

                    // Build request JSON
                    JSONObject requestJson = new JSONObject();
                    requestJson.put("email", email);
                    requestJson.put("password", password);

                    String jsonInputString = requestJson.toString();

                    // Write output stream
                    try (OutputStream os = urlConnection.getOutputStream()) {
                        byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                        os.write(input, 0, input.length);
                    }

                    int responseCode = urlConnection.getResponseCode();
                    BufferedReader br;
                    if (responseCode >= 200 && responseCode < 300) {
                        br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), StandardCharsets.UTF_8));
                    } else {
                        br = new BufferedReader(new InputStreamReader(urlConnection.getErrorStream(), StandardCharsets.UTF_8));
                    }

                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }

                    final String responseString = response.toString();
                    Log.d(TAG, "Response Code: " + responseCode + ", Response: " + responseString);

                    if (responseCode >= 200 && responseCode < 300) {
                        final JSONObject responseJson = new JSONObject(responseString);
                        
                        // Check if the API response itself indicates an error (e.g. status: "0")
                        boolean isSuccess = true;
                        String apiMessage = "api.something_wrong";
                        
                        if (responseJson.has("status")) {
                            Object statusObj = responseJson.get("status");
                            if (statusObj instanceof String) {
                                if ("0".equals(statusObj)) isSuccess = false;
                            } else if (statusObj instanceof Number) {
                                if (((Number) statusObj).intValue() == 0) isSuccess = false;
                            } else if (statusObj instanceof Boolean) {
                                if (!((Boolean) statusObj)) isSuccess = false;
                            }
                        }
                        
                        if (responseJson.has("message")) {
                            apiMessage = responseJson.getString("message");
                        }
                        
                        final boolean finalSuccess = isSuccess;
                        final String finalMessage = apiMessage;
                        
                        if (finalSuccess) {
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    callback.onSuccess(responseJson);
                                }
                            });
                        } else {
                            final Exception error = new Exception(finalMessage);
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    callback.onError(error);
                                }
                            });
                        }
                    } else {
                        String errorMessage = "Login failed with status code: " + responseCode;
                        try {
                            JSONObject errJson = new JSONObject(responseString);
                            if (errJson.has("message")) {
                                errorMessage = errJson.getString("message");
                            }
                        } catch (Exception ignored) {}
                        
                        final Exception error = new Exception(errorMessage);
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onError(error);
                            }
                        });
                    }

                } catch (final Exception e) {
                    Log.e(TAG, "Network request exception", e);
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onError(e);
                        }
                    });
                } finally {
                    if (urlConnection != null) {
                        urlConnection.disconnect();
                    }
                }
            }
        });
    }
}
