package com.pm.appdev.duta.notifications;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.pm.appdev.duta.Common.NodeNames;
import com.pm.appdev.duta.Common.Util;

import java.util.concurrent.atomic.AtomicInteger;

public class NotificationCheckManager {

    public interface Listener {
        void onNotificationSnapshot(@NonNull NotificationSnapshot snapshot);

        void onCheckError(@NonNull String reason, boolean retryable);

        void onAuthRequired();
    }

    private static final String TAG = "NotificationChecker";
    private static final long DEFAULT_POLL_MS = 2 * 60 * 1000;

    private final Context appContext;
    private final Listener listener;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
    private final long pollMs;

    private boolean running;

    private final Runnable periodicCheck = new Runnable() {
        @Override
        public void run() {
            performCheck(false);
            if (running) {
                handler.postDelayed(this, pollMs);
            }
        }
    };

    public NotificationCheckManager(@NonNull Context context, @NonNull Listener listener) {
        this(context, listener, DEFAULT_POLL_MS);
    }

    public NotificationCheckManager(@NonNull Context context, @NonNull Listener listener, long pollMs) {
        this.appContext = context.getApplicationContext();
        this.listener = listener;
        this.pollMs = Math.max(30000, pollMs);
    }

    public void start() {
        if (running) {
            return;
        }
        running = true;
        forceCheck();
        handler.postDelayed(periodicCheck, pollMs);
    }

    public void stop() {
        running = false;
        handler.removeCallbacks(periodicCheck);
    }

    public void forceCheck() {
        performCheck(true);
    }

    private void performCheck(boolean manualTrigger) {
        if (!Util.connectionAvailable(appContext)) {
            listener.onCheckError("No network connection for notification check", true);
            listener.onNotificationSnapshot(new NotificationSnapshot(0, 0, true));
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            listener.onAuthRequired();
            return;
        }

        ensureSession(user, new SessionCallback() {
            @Override
            public void onReady(@NonNull String uid) {
                fetchNotificationCounts(uid, manualTrigger);
            }

            @Override
            public void onError(@NonNull String reason, boolean retryable) {
                listener.onCheckError(reason, retryable);
            }
        });
    }

    private void ensureSession(@NonNull FirebaseUser user, @NonNull SessionCallback callback) {
        user.getIdToken(false).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                callback.onReady(user.getUid());
                return;
            }

            user.getIdToken(true).addOnCompleteListener(refreshTask -> {
                if (refreshTask.isSuccessful()) {
                    callback.onReady(user.getUid());
                } else {
                    Exception exception = refreshTask.getException();
                    callback.onError("Authentication refresh failed: " +
                            (exception != null ? exception.getMessage() : "unknown"), false);
                }
            });
        });
    }

    private void fetchNotificationCounts(@NonNull String uid, boolean manualTrigger) {
        AtomicInteger remaining = new AtomicInteger(2);
        final int[] unreadChatCount = {0};
        final int[] pendingRequestCount = {0};

        rootRef.child(NodeNames.CHATS)
                .child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        int unread = 0;
                        for (DataSnapshot chatSnapshot : snapshot.getChildren()) {
                            Long unreadCount = chatSnapshot.child(NodeNames.UNREAD_COUNT).getValue(Long.class);
                            if (unreadCount != null) {
                                unread += Math.max(0, unreadCount.intValue());
                            }
                        }
                        unreadChatCount[0] = unread;
                        maybeDispatch(remaining, unreadChatCount[0], pendingRequestCount[0], false, manualTrigger);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Chat count fetch cancelled", error.toException());
                        listener.onCheckError("Failed to fetch unread chats: " + error.getMessage(), true);
                        maybeDispatch(remaining, unreadChatCount[0], pendingRequestCount[0], true, manualTrigger);
                    }
                });

        rootRef.child(NodeNames.FRIEND_REQUESTS)
                .orderByChild("receiverUid")
                .equalTo(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        int pending = 0;
                        for (DataSnapshot requestSnapshot : snapshot.getChildren()) {
                            String status = requestSnapshot.child("status").getValue(String.class);
                            if ("Requested".equalsIgnoreCase(status)) {
                                pending++;
                            }
                        }
                        pendingRequestCount[0] = pending;
                        maybeDispatch(remaining, unreadChatCount[0], pendingRequestCount[0], false, manualTrigger);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Request count fetch cancelled", error.toException());
                        listener.onCheckError("Failed to fetch friend requests: " + error.getMessage(), true);
                        maybeDispatch(remaining, unreadChatCount[0], pendingRequestCount[0], true, manualTrigger);
                    }
                });
    }

    private void maybeDispatch(AtomicInteger remaining,
                               int unreadChatCount,
                               int pendingRequestCount,
                               boolean stale,
                               boolean manualTrigger) {
        if (remaining.decrementAndGet() != 0) {
            return;
        }

        NotificationSnapshot snapshot = new NotificationSnapshot(unreadChatCount, pendingRequestCount, stale);
        listener.onNotificationSnapshot(snapshot);

        if (manualTrigger) {
            Log.d(TAG, "Manual notification check result: " + snapshot);
        }
    }

    private interface SessionCallback {
        void onReady(@NonNull String uid);

        void onError(@NonNull String reason, boolean retryable);
    }
}
