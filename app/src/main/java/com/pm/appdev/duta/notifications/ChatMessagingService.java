package com.pm.appdev.duta.notifications;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.pm.appdev.duta.R;
import com.pm.appdev.duta.Common.Constants;
import com.pm.appdev.duta.Login.LoginActivity;
import com.pm.appdev.duta.Common.Util;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class ChatMessagingService extends FirebaseMessagingService {
    private static final String TAG = "ChatMessagingService";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Refreshed token: " + token);
        Util.updateDeviceToken(this, token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "Message received: " + remoteMessage.getData());

        // Extract data from message
        String title = remoteMessage.getData().get(Constants.NOTIFICATION_TITLE);
        String message = remoteMessage.getData().get(Constants.NOTIFICATION_MESSAGE);

        // Validate notification data
        if (title == null || message == null) {
            Log.e(TAG, "Notification data is null. Title: " + title + ", Message: " + message);
            return;
        }

        // Create intent for notification click
        Intent intentChat = new Intent(this, LoginActivity.class);
        intentChat.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intentChat,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        // Create notification builder
        NotificationCompat.Builder notificationBuilder = createNotificationBuilder();
        notificationBuilder.setSmallIcon(R.drawable.ic_chat)
                .setColor(getResources().getColor(R.color.colorPrimary))
                .setContentTitle(title)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        try {
            if (message.startsWith("https://firebasestorage.")) {
                handleImageNotification(notificationManager, notificationBuilder, message);
            } else {
                notificationBuilder.setContentText(message);
                notificationManager.notify(getNotificationId(), notificationBuilder.build());
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to show notification", e);
            // Fallback to simple text notification if image loading fails
            notificationBuilder.setContentText("New message received");
            notificationManager.notify(getNotificationId(), notificationBuilder.build());
        }
    }

    private NotificationCompat.Builder createNotificationBuilder() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    Constants.CHANNEL_ID,
                    Constants.CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(Constants.CHANNEL_DESC);
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
            return new NotificationCompat.Builder(this, Constants.CHANNEL_ID);
        } else {
            return new NotificationCompat.Builder(this);
        }
    }

    @SuppressLint("SuspiciousIndentation")
    private void handleImageNotification(NotificationManager notificationManager,
                                         NotificationCompat.Builder notificationBuilder,
                                         String imageUrl) {
        try {
            final NotificationCompat.BigPictureStyle bigPictureStyle = new NotificationCompat.BigPictureStyle();
            bigPictureStyle.setSummaryText("New image received");

            Glide.with(this)
                    .asBitmap()
                    .load(imageUrl)
                    .into(new CustomTarget<Bitmap>(200, 100) {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            bigPictureStyle.bigPicture(resource);
                            notificationBuilder.setStyle(bigPictureStyle);
                            notificationManager.notify(getNotificationId(), notificationBuilder.build());
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                            // Fallback if image loading is cleared
                            notificationBuilder.setContentText("New image received");
                            notificationManager.notify(getNotificationId(), notificationBuilder.build());
                        }

                        @Override
                        public void onLoadFailed(@Nullable Drawable errorDrawable) {
                            super.onLoadFailed(errorDrawable);
                            notificationBuilder.setContentText("New image received");
                            notificationManager.notify(getNotificationId(), notificationBuilder.build());
                        }
                    });
        } catch (Exception ex) {
            Log.e(TAG, "Image notification error", ex);
            notificationBuilder.setContentText("New image received");
            notificationManager.notify(getNotificationId(), notificationBuilder.build());
        }
    }

    private int getNotificationId() {
        return (int) System.currentTimeMillis();
    }
}