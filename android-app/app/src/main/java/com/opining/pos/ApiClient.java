package com.opining.pos;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

public class ApiClient {

    private static final String TAG = "ApiClient";
    private static final String BASE_URL = "https://admin.opiningstore.com";
    
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
                    if (urlConnection instanceof HttpsURLConnection) {
                        ((HttpsURLConnection) urlConnection).setHostnameVerifier(new HostnameVerifier() {
                            @Override
                            public boolean verify(String hostname, SSLSession session) {
                                return true;
                            }
                        });
                    }
                    urlConnection.setRequestMethod("POST");
                    urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                    urlConnection.setRequestProperty("Accept", "application/json");
                    urlConnection.setDoOutput(true);
                    urlConnection.setConnectTimeout(10000);
                    urlConnection.setReadTimeout(10000);

                    // Build form-urlencoded request body matching api.js
                    String postData = "email=" + URLEncoder.encode(email != null ? email : "", "UTF-8")
                            + "&password=" + URLEncoder.encode(password != null ? password : "", "UTF-8");
                    byte[] input = postData.getBytes(StandardCharsets.UTF_8);

                    // Write output stream
                    try (OutputStream os = urlConnection.getOutputStream()) {
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

    /**
     * Change status of an order.
     * Pass orderId and orderStatus (e.g., "2" for InKitchen, "6" for Delivered).
     */
    public void changeOrderStatus(final String orderId, final String orderStatus, final String token, final ApiCallback callback) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection urlConnection = null;
                try {
                    URL url = new URL(BASE_URL + "/api/v1/changeOrderStatus");
                    urlConnection = (HttpURLConnection) url.openConnection();
                    if (urlConnection instanceof HttpsURLConnection) {
                        ((HttpsURLConnection) urlConnection).setHostnameVerifier(new HostnameVerifier() {
                            @Override
                            public boolean verify(String hostname, SSLSession session) {
                                return true;
                            }
                        });
                    }
                    urlConnection.setRequestMethod("POST");
                    urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                    urlConnection.setRequestProperty("Accept", "application/json");
                    if (token != null && !token.isEmpty()) {
                        urlConnection.setRequestProperty("Authorization", "Bearer " + token);
                    }
                    urlConnection.setDoOutput(true);
                    urlConnection.setConnectTimeout(10000);
                    urlConnection.setReadTimeout(10000);

                    // Build form-urlencoded request body matching api.js
                    String safeId = orderId != null ? orderId : "";
                    String safeStatus = orderStatus != null ? orderStatus : "";
                    String postData = "id=" + URLEncoder.encode(safeId, "UTF-8")
                            + "&order_id=" + URLEncoder.encode(safeId, "UTF-8")
                            + "&order_status=" + URLEncoder.encode(safeStatus, "UTF-8");
                    byte[] input = postData.getBytes(StandardCharsets.UTF_8);

                    try (OutputStream os = urlConnection.getOutputStream()) {
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
                    Log.d(TAG, "changeOrderStatus Response Code: " + responseCode + ", Response: " + responseString);

                    if (responseCode >= 200 && responseCode < 300) {
                        final JSONObject responseJson = new JSONObject(responseString);

                        boolean isSuccess = true;
                        String apiMessage = "Failed to change order status";

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
                                    if (callback != null) callback.onSuccess(responseJson);
                                }
                            });
                        } else {
                            final Exception error = new Exception(finalMessage);
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (callback != null) callback.onError(error);
                                }
                            });
                        }
                    } else {
                        String errorMessage = "Request failed with status code: " + responseCode;
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
                                if (callback != null) callback.onError(error);
                            }
                        });
                    }

                } catch (final Exception e) {
                    Log.e(TAG, "changeOrderStatus network exception", e);
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (callback != null) callback.onError(e);
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

    /**
     * Cancel an order.
     * Pass orderId and status (e.g., "7").
     */
    public void cancelOrder(final String orderId, final String status, final String token, final ApiCallback callback) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection urlConnection = null;
                try {
                    URL url = new URL(BASE_URL + "/api/v1/cancelOrder");
                    urlConnection = (HttpURLConnection) url.openConnection();
                    if (urlConnection instanceof HttpsURLConnection) {
                        ((HttpsURLConnection) urlConnection).setHostnameVerifier(new HostnameVerifier() {
                            @Override
                            public boolean verify(String hostname, SSLSession session) {
                                return true;
                            }
                        });
                    }
                    urlConnection.setRequestMethod("POST");
                    urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                    urlConnection.setRequestProperty("Accept", "application/json");
                    if (token != null && !token.isEmpty()) {
                        urlConnection.setRequestProperty("Authorization", "Bearer " + token);
                    }
                    urlConnection.setDoOutput(true);
                    urlConnection.setConnectTimeout(10000);
                    urlConnection.setReadTimeout(10000);

                    String safeId = orderId != null ? orderId : "";
                    String safeStatus = status != null && !status.isEmpty() ? status : "7";
                    String postData = "id=" + URLEncoder.encode(safeId, "UTF-8")
                            + "&order_id=" + URLEncoder.encode(safeId, "UTF-8")
                            + "&status=" + URLEncoder.encode(safeStatus, "UTF-8")
                            + "&order_status=" + URLEncoder.encode(safeStatus, "UTF-8");
                    byte[] input = postData.getBytes(StandardCharsets.UTF_8);

                    try (OutputStream os = urlConnection.getOutputStream()) {
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
                    Log.d(TAG, "cancelOrder Response Code: " + responseCode + ", Response: " + responseString);

                    if (responseCode >= 200 && responseCode < 300) {
                        final JSONObject responseJson = new JSONObject(responseString);
                        boolean isSuccess = true;
                        String apiMessage = "Failed to cancel order";

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
                                    if (callback != null) callback.onSuccess(responseJson);
                                }
                            });
                        } else {
                            final Exception error = new Exception(finalMessage);
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (callback != null) callback.onError(error);
                                }
                            });
                        }
                    } else {
                        String errorMessage = "Request failed with status code: " + responseCode;
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
                                if (callback != null) callback.onError(error);
                            }
                        });
                    }

                } catch (final Exception e) {
                    Log.e(TAG, "cancelOrder network exception", e);
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (callback != null) callback.onError(e);
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

    /**
     * Fetch all categories from server.
     */
    public void getCategories(final String token, final ApiCallback callback) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection urlConnection = null;
                try {
                    URL url = new URL(BASE_URL + "/api/v1/getCategories");
                    urlConnection = (HttpURLConnection) url.openConnection();
                    if (urlConnection instanceof HttpsURLConnection) {
                        ((HttpsURLConnection) urlConnection).setHostnameVerifier(new HostnameVerifier() {
                            @Override
                            public boolean verify(String hostname, SSLSession session) {
                                return true;
                            }
                        });
                    }
                    urlConnection.setRequestMethod("POST");
                    urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                    urlConnection.setRequestProperty("Accept", "application/json");
                    if (token != null && !token.isEmpty()) {
                        urlConnection.setRequestProperty("Authorization", "Bearer " + token);
                    }
                    urlConnection.setDoOutput(true);
                    urlConnection.setConnectTimeout(10000);
                    urlConnection.setReadTimeout(10000);

                    try (OutputStream os = urlConnection.getOutputStream()) {
                        os.write(new byte[0]);
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
                    Log.d(TAG, "getCategories Response Code: " + responseCode + ", Response: " + responseString);

                    if (responseCode >= 200 && responseCode < 300) {
                        final JSONObject responseJson = new JSONObject(responseString);
                        boolean isSuccess = true;
                        String apiMessage = "Failed to fetch categories";

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
                                    if (callback != null) callback.onSuccess(responseJson);
                                }
                            });
                        } else {
                            final Exception error = new Exception(finalMessage);
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (callback != null) callback.onError(error);
                                }
                            });
                        }
                    } else {
                        String errorMessage = "Request failed with status code: " + responseCode;
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
                                if (callback != null) callback.onError(error);
                            }
                        });
                    }

                } catch (final Exception e) {
                    Log.e(TAG, "getCategories network exception", e);
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (callback != null) callback.onError(e);
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

    /**
     * Change dish status (1 active, 0 inactive).
     * Endpoint: /api/v1/change-dish-status/{id}
     */
    public void changeDishStatus(final String dishId, final int status, final String token, final ApiCallback callback) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection urlConnection = null;
                try {
                    String safeDishId = dishId != null ? dishId : "";
                    URL url = new URL(BASE_URL + "/api/v1/change-dish-status/" + safeDishId);
                    urlConnection = (HttpURLConnection) url.openConnection();
                    if (urlConnection instanceof HttpsURLConnection) {
                        ((HttpsURLConnection) urlConnection).setHostnameVerifier(new HostnameVerifier() {
                            @Override
                            public boolean verify(String hostname, SSLSession session) {
                                return true;
                            }
                        });
                    }
                    urlConnection.setRequestMethod("POST");
                    urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                    urlConnection.setRequestProperty("Accept", "application/json");
                    if (token != null && !token.isEmpty()) {
                        urlConnection.setRequestProperty("Authorization", "Bearer " + token);
                    }
                    urlConnection.setDoOutput(true);
                    urlConnection.setConnectTimeout(10000);
                    urlConnection.setReadTimeout(10000);

                    String safeStatus = String.valueOf(status);
                    String postData = "status=" + URLEncoder.encode(safeStatus, "UTF-8")
                            + "&dish_id=" + URLEncoder.encode(safeDishId, "UTF-8")
                            + "&id=" + URLEncoder.encode(safeDishId, "UTF-8");
                    byte[] input = postData.getBytes(StandardCharsets.UTF_8);

                    try (OutputStream os = urlConnection.getOutputStream()) {
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
                    Log.d(TAG, "changeDishStatus Response Code: " + responseCode + ", Response: " + responseString);

                    if (responseCode >= 200 && responseCode < 300) {
                        final JSONObject responseJson = new JSONObject(responseString);
                        boolean isSuccess = true;
                        String apiMessage = "Failed to change dish status";

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
                                    if (callback != null) callback.onSuccess(responseJson);
                                }
                            });
                        } else {
                            final Exception error = new Exception(finalMessage);
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (callback != null) callback.onError(error);
                                }
                            });
                        }
                    } else {
                        String errorMessage = "Request failed with status code: " + responseCode;
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
                                if (callback != null) callback.onError(error);
                            }
                        });
                    }

                } catch (final Exception e) {
                    Log.e(TAG, "changeDishStatus network exception", e);
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (callback != null) callback.onError(e);
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

    /**
     * Fetch new order details.
     * Endpoint: /api/v1/getNewOrderDetails
     */
    public void getNewOrderDetails(final String orderId, final String token, final ApiCallback callback) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection urlConnection = null;
                try {
                    URL url = new URL(BASE_URL + "/api/v1/getNewOrderDetails");
                    urlConnection = (HttpURLConnection) url.openConnection();
                    if (urlConnection instanceof HttpsURLConnection) {
                        ((HttpsURLConnection) urlConnection).setHostnameVerifier(new HostnameVerifier() {
                            @Override
                            public boolean verify(String hostname, SSLSession session) {
                                return true;
                            }
                        });
                    }
                    urlConnection.setRequestMethod("POST");
                    urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                    urlConnection.setRequestProperty("Accept", "application/json");
                    if (token != null && !token.isEmpty()) {
                        urlConnection.setRequestProperty("Authorization", "Bearer " + token);
                    }
                    urlConnection.setDoOutput(true);
                    urlConnection.setConnectTimeout(10000);
                    urlConnection.setReadTimeout(10000);

                    String safeId = orderId != null ? orderId : "";
                    String postData = "order_id=" + URLEncoder.encode(safeId, "UTF-8");
                    byte[] input = postData.getBytes(StandardCharsets.UTF_8);

                    try (OutputStream os = urlConnection.getOutputStream()) {
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
                    Log.d(TAG, "getNewOrderDetails Response Code: " + responseCode + ", Response: " + responseString);

                    if (responseCode >= 200 && responseCode < 300) {
                        final JSONObject responseJson = new JSONObject(responseString);
                        boolean isSuccess = true;
                        String apiMessage = "Failed to fetch new order details";

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
                                    if (callback != null) callback.onSuccess(responseJson);
                                }
                            });
                        } else {
                            final Exception error = new Exception(finalMessage);
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (callback != null) callback.onError(error);
                                }
                            });
                        }
                    } else {
                        String errorMessage = "Request failed with status code: " + responseCode;
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
                                if (callback != null) callback.onError(error);
                            }
                        });
                    }

                } catch (final Exception e) {
                    Log.e(TAG, "getNewOrderDetails network exception", e);
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (callback != null) callback.onError(e);
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
