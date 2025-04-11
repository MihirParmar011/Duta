package com.pm.appdev.duta.Common;

import android.app.ActivityManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.Network;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.pm.appdev.duta.R;
import android.os.Handler;
import android.os.Looper; // Add this import at the top
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Util {

    public  static  boolean connectionAvailable(Context context){
        ConnectivityManager connectivityManager  = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if(connectivityManager !=null && connectivityManager.getActiveNetworkInfo()!=null)
        {
            return  connectivityManager.getActiveNetworkInfo().isAvailable();
        }
        else {
            return  false;
        }
    }

    public static void showToastOnMainThread(Context context, String message) {
        if (context != null && message != null) {
            new Handler(Looper.getMainLooper()).post(() ->
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            );
        }
    }

    public static void updateDeviceToken(final Context context, String token) {
        // 1. Check if token is valid
        if (token == null || token.isEmpty()) {
            Log.w("DeviceToken", "Received empty or null token, skipping update");
            return;
        }

        // 2. Get current Firebase user
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser != null) {
            String currentUid = currentUser.getUid();
            DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
            DatabaseReference tokensRef = rootRef.child("Tokens");

            // 3. Check if user exists in Tokens node
            tokensRef.child(currentUid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        // User exists in Tokens - update their token
                        Map<String, Object> updateData = new HashMap<>();
                        updateData.put("device_token", token);

                        tokensRef.child(currentUid).updateChildren(updateData)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("DeviceToken", "Device token updated successfully");
                                })
                                .addOnFailureListener(e -> {
                                    showToastOnMainThread(context, context.getString(R.string.failed_to_save_device_token));
                                    Log.e("DeviceToken", "Failed to save token", e);
                                });
                    } else {
                        // User not found in Tokens node
                        showToastOnMainThread(context, "User not found in database");
                        Log.w("DeviceToken", "User UID not found in Tokens node: " + currentUid);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    showToastOnMainThread(context, "Database error: " + databaseError.getMessage());
                    Log.e("DeviceToken", "Database error checking user", databaseError.toException());
                }
            });
        } else {
            Log.w("DeviceToken", "User not logged in, cannot update token");
//            showToastOnMainThread(context, "User not logged in");
        }
    }

    public static void sendNotification(final Context context, final String title,
                                        final String message, String userId) {
        // 1. Validate all input parameters
        if (context == null) {
            Log.e("Notification", "Context is null");
            return;
        }

        if (TextUtils.isEmpty(userId)) {
            showErrorToast(context, "Invalid user ID");
            return;
        }

        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(message)) {
            showErrorToast(context, "Title or message cannot be empty");
            return;
        }

        // 2. Get Firebase database reference
        DatabaseReference userTokenRef = FirebaseDatabase.getInstance()
                .getReference("Tokens")
                .child(userId)
                .child("device_token");

        userTokenRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    // 3. Get and validate the device token
                    String deviceToken = dataSnapshot.getValue(String.class);

                    if (TextUtils.isEmpty(deviceToken)) {
                        showErrorToast(context, "Recipient device token not found");
                        return;
                    }

                    // 4. Build FCM payload
                    JSONObject payload = new JSONObject();
                    JSONObject notification = new JSONObject();
                    JSONObject data = new JSONObject();

                    try {
                        // Notification content
                        notification.put("title", title);
                        notification.put("body", message);
                        notification.put("sound", "default");

                        // Data payload
                        data.put("title", title);
                        data.put("message", message);
                        data.put("userId", userId);

                        // Complete payload
                        payload.put("to", deviceToken.trim());
                        payload.put("priority", "high");
                        payload.put("notification", notification);
                        payload.put("data", data);

                        // 5. Send with improved error handling
                        sendFcmRequest(context, payload);

                    } catch (JSONException e) {
                        showErrorToast(context, "Failed to create notification");
                        Log.e("Notification", "JSON error", e);
                    }
                } catch (Exception e) {
                    showErrorToast(context, "Failed to process notification");
                    Log.e("Notification", "Unexpected error", e);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                showErrorToast(context, "Failed to access user token");
                Log.e("Notification", "Database error", databaseError.toException());
            }
        });
    }

    private static void sendFcmRequest(Context context, JSONObject payload) {
        String fcmUrl = "https://fcm.googleapis.com/fcm/send";

        // 1. Validate payload
        if (payload == null) {
            showErrorToast(context, "Invalid FCM payload");
            return;
        }

        // 2. Create request queue
        RequestQueue requestQueue = Volley.newRequestQueue(context.getApplicationContext());

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                fcmUrl,
                payload,
                response -> {
                    Log.d("FCM", "Success: " + response);
                    try {
                        // Check for server errors in success response
                        if (response.has("failure") && response.getInt("failure") > 0) {
                            String errorMsg = "Server rejected notification";
                            if (response.has("results")) {
                                JSONArray results = response.getJSONArray("results");
                                if (results.length() > 0 && results.getJSONObject(0).has("error")) {
                                    errorMsg = results.getJSONObject(0).getString("error");
                                }
                            }
                            showErrorToast(context, errorMsg);
                        }
                    } catch (JSONException e) {
                        Log.e("FCM", "Error parsing success response", e);
                    }
                },
                error -> {
                    String errorMsg = "Failed to send notification";
                    if (error != null) {
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            String responseBody = new String(error.networkResponse.data);
                            // Handle HTML error responses
                            if (responseBody.contains("<html>")) {
                                errorMsg = "Server error (check API key)";
                            } else {
                                try {
                                    JSONObject errorJson = new JSONObject(responseBody);
                                    if (errorJson.has("error")) {
                                        errorMsg = errorJson.getString("error");
                                    }
                                } catch (JSONException e) {
                                    errorMsg = responseBody;
                                }
                            }
                        } else {
                            errorMsg = error.getMessage();
                        }
                    }
                    showErrorToast(context, errorMsg);
                    Log.e("FCM", "Error: " + errorMsg);
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "key=" + Constants.FIREBASE_KEY);
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        // 3. Set retry policy
        request.setRetryPolicy(new DefaultRetryPolicy(
                20000, // 20 seconds timeout
                2, // Max retries
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        // 4. Add to request queue
        requestQueue.add(request);
    }

    private static void showErrorToast(Context context, String message) {
        if (context != null) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
        Log.e("Notification", message);
    }

    private static boolean isAppInForeground(Context context) {
        if (context == null) return false;
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> processes = am.getRunningAppProcesses();
        if (processes == null) return false;

        String packageName = context.getPackageName();
        for (ActivityManager.RunningAppProcessInfo process : processes) {
            if (process.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                    && process.processName.equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    private static void handleError(Context context, String error) {
        Log.e("NotificationUtil", error);
        if (context != null) {
            Toast.makeText(context,
                    context.getString(R.string.failed_to_send_notification, error),
                    Toast.LENGTH_SHORT).show();
        }
    }

    public static void updateChatDetails(final Context context, final String currentUserId,
                                         final String chatUserId, final String lastMessage) {
        // Validate input parameters
        if (context == null || currentUserId == null || chatUserId == null || lastMessage == null) {
            Log.e("ChatUpdate", "Null parameter detected in updateChatDetails");
            return;
        }

        final DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        DatabaseReference chatRef = rootRef.child(NodeNames.CHATS).child(chatUserId).child(currentUserId);

        chatRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    int currentCount = 0;
                    if (dataSnapshot.hasChild(NodeNames.UNREAD_COUNT)) {
                        try {
                            Object countValue = dataSnapshot.child(NodeNames.UNREAD_COUNT).getValue();
                            if (countValue != null) {
                                currentCount = Integer.parseInt(countValue.toString());
                            }
                        } catch (NumberFormatException e) {
                            Log.e("ChatUpdate", "Error parsing unread count", e);
                        }
                    }

                    // Create typed maps
                    Map<String, Object> chatMap = new HashMap<>();
                    chatMap.put(NodeNames.TIME_STAMP, ServerValue.TIMESTAMP);
                    chatMap.put(NodeNames.UNREAD_COUNT, currentCount + 1);
                    chatMap.put(NodeNames.LAST_MESSAGE, lastMessage);
                    chatMap.put(NodeNames.LAST_MESSAGE_TIME, ServerValue.TIMESTAMP);

                    Map<String, Object> chatUserMap = new HashMap<>();
                    chatUserMap.put(NodeNames.CHATS + "/" + chatUserId + "/" + currentUserId, chatMap);

                    rootRef.updateChildren(chatUserMap, (databaseError, databaseReference) -> {
                        if (databaseError != null) {
                            Log.e("ChatUpdate", "Failed to update chat: " + databaseError.getMessage());
                            Toast.makeText(context,
                                    context.getString(R.string.something_went_wrong, databaseError.getMessage()),
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Log.d("ChatUpdate", "Chat details updated successfully");
                        }
                    });

                } catch (Exception e) {
                    Log.e("ChatUpdate", "Error updating chat details", e);
                    Toast.makeText(context,
                            context.getString(R.string.something_went_wrong, e.getMessage()),
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("ChatUpdate", "Chat update cancelled: " + databaseError.getMessage());
                Toast.makeText(context,
                        context.getString(R.string.something_went_wrong, databaseError.getMessage()),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static  String getTimeAgo(long time)
    {
        final  int SECOND_MILLIS = 1000;
        final  int MINUTE_MILLIS= 60 * SECOND_MILLIS;
        final  int HOUR_MILLIS = 60 * MINUTE_MILLIS;
        final  int DAY_MILLIS = 24 * HOUR_MILLIS;

        if (time < 1000000000000L) {
            time *= 1000;
        }

        long now = System.currentTimeMillis();

        if(time>now || time <=0)
        {
            return  "";
        }

        final  long diff = now-time;

        if(diff<MINUTE_MILLIS)
        {
            return  "just now";
        }
        else if(diff <2* MINUTE_MILLIS)
        {
            return  "a minute ago";
        }
        else if(diff <59*MINUTE_MILLIS)
        {
            return  diff/MINUTE_MILLIS + " minutes ago";
        }
        else  if(diff < 90 * MINUTE_MILLIS)
        {
            return "an hour ago";
        }
        else if(diff<24*HOUR_MILLIS){
            return  diff/HOUR_MILLIS + " hours ago";
        }
        else if( diff < 48 * HOUR_MILLIS)
        {
            return  "yesterday";
        }
        else
        {
            return  diff/DAY_MILLIS  + " days ago";
        }

    }

}
