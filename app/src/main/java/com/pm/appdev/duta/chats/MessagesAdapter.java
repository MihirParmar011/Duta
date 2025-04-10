package com.pm.appdev.duta.chats;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.pm.appdev.duta.Common.Constants;
import com.pm.appdev.duta.Common.ImageRepository;
import com.pm.appdev.duta.R;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.MessageViewHolder> {

    private final Context context;
    private final List<MessageModel> messageList;
    private ActionMode actionMode;
    private ConstraintLayout selectedView;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    public MessagesAdapter(Context context, List<MessageModel> messageList) {
        this.context = context;
        this.messageList = messageList;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.message_layout, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        MessageModel message = messageList.get(position);

        // Handle date separator
        boolean showDate = shouldShowDate(position, message.getMessageTime());
        holder.llDateSeparator.setVisibility(showDate ? View.VISIBLE : View.GONE);
        holder.tvDateSeparator.setVisibility(showDate ? View.VISIBLE : View.GONE);

        if (showDate) {
            holder.tvDateSeparator.setText(getFormattedDate(message.getMessageTime()));
        }

        // Load message content
        loadMessageContent(holder, message);
    }

    private boolean shouldShowDate(int position, long timestamp) {
        if (position == 0) return true;

        long prevTimestamp = messageList.get(position - 1).getMessageTime();
        Calendar currentCal = Calendar.getInstance();
        Calendar prevCal = Calendar.getInstance();
        currentCal.setTimeInMillis(timestamp);
        prevCal.setTimeInMillis(prevTimestamp);

        return currentCal.get(Calendar.YEAR) != prevCal.get(Calendar.YEAR) ||
                currentCal.get(Calendar.DAY_OF_YEAR) != prevCal.get(Calendar.DAY_OF_YEAR);
    }

    private String getFormattedDate(long timestamp) {
        Calendar messageCal = Calendar.getInstance();
        messageCal.setTimeInMillis(timestamp);
        Calendar todayCal = Calendar.getInstance();
        Calendar yesterdayCal = Calendar.getInstance();
        yesterdayCal.add(Calendar.DATE, -1);

        if (messageCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                messageCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)) {
            return "Today";
        } else if (messageCal.get(Calendar.YEAR) == yesterdayCal.get(Calendar.YEAR) &&
                messageCal.get(Calendar.DAY_OF_YEAR) == yesterdayCal.get(Calendar.DAY_OF_YEAR)) {
            return "Yesterday";
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        }
    }

    @SuppressLint("SetTextI18n")
    private void loadMessageContent(MessageViewHolder holder, MessageModel message) {
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        String currentUserId = Objects.requireNonNull(firebaseAuth.getCurrentUser()).getUid();
        boolean isSent = message.getMessageFrom().equals(currentUserId);
        String messageTime = new SimpleDateFormat("h:mm a", Locale.getDefault())
                .format(new Date(message.getMessageTime()));

        resetMessageViews(holder);

        if (isSent) {
            bindSentMessage(holder, message, messageTime);
        } else {
            bindReceivedMessage(holder, message, messageTime);
        }

        setMessageMetadata(holder, message);
    }

    @SuppressLint("SetTextI18n")
    private void bindSentMessage(MessageViewHolder holder, MessageModel message, String messageTime) {
        switch (message.getMessageType()) {
            case Constants.MESSAGE_TYPE_TEXT:
                holder.llSent.setVisibility(View.VISIBLE);
                holder.tvSentMessage.setText(message.getMessage());
                holder.tvSentMessageTime.setText(messageTime);
                break;
            case Constants.MESSAGE_TYPE_IMAGE:
                holder.llSentImage.setVisibility(View.VISIBLE);
                holder.tvImageSentTime.setText(messageTime);
                loadImage(message.getMessage(), holder.ivSent);
                break;
            case Constants.MESSAGE_TYPE_VIDEO:
                holder.llSent.setVisibility(View.VISIBLE);
                holder.tvSentMessage.setText("[Video]");
                holder.tvSentMessageTime.setText(messageTime);
                break;
        }
    }

    @SuppressLint("SetTextI18n")
    private void bindReceivedMessage(MessageViewHolder holder, MessageModel message, String messageTime) {
        switch (message.getMessageType()) {
            case Constants.MESSAGE_TYPE_TEXT:
                holder.llReceived.setVisibility(View.VISIBLE);
                holder.tvReceivedMessage.setText(message.getMessage());
                holder.tvReceivedMessageTime.setText(messageTime);
                break;
            case Constants.MESSAGE_TYPE_IMAGE:
                holder.llReceivedImage.setVisibility(View.VISIBLE);
                holder.tvImageReceivedTime.setText(messageTime);
                loadImage(message.getMessage(), holder.ivReceived);
                break;
            case Constants.MESSAGE_TYPE_VIDEO:
                holder.llReceived.setVisibility(View.VISIBLE);
                holder.tvReceivedMessage.setText("[Video]");
                holder.tvReceivedMessageTime.setText(messageTime);
                break;
        }
    }

    private void loadImage(String imageData, ShapeableImageView imageView) {
        executor.execute(() -> {
            try {
                String hash = getSHA256Hash(imageData);
                String fileName = "img_" + hash + ".jpg";
                File imageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                File imageFile = new File(imageDir, fileName);

                assert imageDir != null;
                if (!imageDir.exists()) {
                    imageDir.mkdirs();
                }

                Bitmap bitmap;
                if (imageFile.exists()) {
                    bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
                } else {
                    byte[] decodedString = Base64.decode(imageData, Base64.DEFAULT);
                    bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    try (FileOutputStream out = new FileOutputStream(imageFile)) {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out);
                    }
                }

                ((Activity) context).runOnUiThread(() -> {
                    AlphaAnimation fadeIn = new AlphaAnimation(0.2f, 1.0f);
                    fadeIn.setDuration(300);
                    imageView.setImageBitmap(bitmap);
                    imageView.startAnimation(fadeIn);
                });
            } catch (Exception e) {
                ((Activity) context).runOnUiThread(() ->
                        imageView.setImageResource(R.drawable.ic_broken_image));
            }
        });
    }

    private String getSHA256Hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(input.hashCode());
        }
    }

    private void resetMessageViews(MessageViewHolder holder) {
        holder.llSent.setVisibility(View.GONE);
        holder.llSentImage.setVisibility(View.GONE);
        holder.llReceived.setVisibility(View.GONE);
        holder.llReceivedImage.setVisibility(View.GONE);
    }

    private void setMessageMetadata(MessageViewHolder holder, MessageModel message) {
        holder.clMessage.setTag(R.id.TAG_MESSAGE, message.getMessage());
        holder.clMessage.setTag(R.id.TAG_MESSAGE_ID, message.getMessageId());
        holder.clMessage.setTag(R.id.TAG_MESSAGE_TYPE, message.getMessageType());

        holder.clMessage.setOnClickListener(v -> handleMessageClick(
                v.getTag(R.id.TAG_MESSAGE_TYPE).toString(),
                v.getTag(R.id.TAG_MESSAGE).toString()
        ));

        holder.clMessage.setOnLongClickListener(v -> {
            handleMessageLongClick(holder, v);
            return true;
        });
    }

    private void handleMessageClick(String messageType, String messageContent) {
        if (messageType.equals(Constants.MESSAGE_TYPE_VIDEO)) {
            playVideo(messageContent);
        } else if (messageType.equals(Constants.MESSAGE_TYPE_IMAGE)) {
            loadAndDisplayFullImage(messageContent);
        }
    }

    private void loadAndDisplayFullImage(String messageContent) {
        executor.execute(() -> {
            try {
                String hash = getSHA256Hash(messageContent);
                String fileName = "full_img_" + hash + ".jpg";
                File imageFile = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), fileName);

                byte[] decodedString = Base64.decode(messageContent, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                try (FileOutputStream out = new FileOutputStream(imageFile)) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                }

                ImageRepository.setCurrentImage(bitmap);
                ImageRepository.setCurrentImagePath(imageFile.getAbsolutePath());

                ((Activity) context).runOnUiThread(() -> {
                    Intent intent = new Intent(context, FullScreenImageActivity.class);
                    intent.putExtra("image_path", imageFile.getAbsolutePath());
                    context.startActivity(intent);
                });
            } catch (Exception e) {
                ((Activity) context).runOnUiThread(() ->
                        Toast.makeText(context, "Failed to load image", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void handleMessageLongClick(MessageViewHolder holder, View view) {
        if (actionMode != null) return;
        selectedView = holder.clMessage;
        actionMode = ((AppCompatActivity) context).startSupportActionMode(actionModeCallBack);
        view.setBackgroundColor(ContextCompat.getColor(context, R.color.colorAccent));
    }

    private void playVideo(String videoBase64) {
        executor.execute(() -> {
            try {
                String hash = getSHA256Hash(videoBase64);
                String fileName = "video_" + hash + ".mp4";
                File videoFile = new File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), fileName);

                byte[] videoBytes = Base64.decode(videoBase64, Base64.DEFAULT);
                try (FileOutputStream fos = new FileOutputStream(videoFile)) {
                    fos.write(videoBytes);
                }

                ((Activity) context).runOnUiThread(() -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    Uri videoUri = FileProvider.getUriForFile(context,
                            context.getPackageName() + ".provider", videoFile);
                    intent.setDataAndType(videoUri, "video/mp4");
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    context.startActivity(intent);
                });
            } catch (Exception e) {
                ((Activity) context).runOnUiThread(() ->
                        Toast.makeText(context, "Couldn't play video", Toast.LENGTH_SHORT).show());
            }
        });
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateMessages(List<MessageModel> newMessages) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new MessageDiffCallback(messageList, newMessages));
        messageList.clear();
        messageList.addAll(newMessages);
        diffResult.dispatchUpdatesTo(this);
    }

    private static class MessageDiffCallback extends DiffUtil.Callback {
        private final List<MessageModel> oldList;
        private final List<MessageModel> newList;

        public MessageDiffCallback(List<MessageModel> oldList, List<MessageModel> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override public int getOldListSize() { return oldList.size(); }
        @Override public int getNewListSize() { return newList.size(); }

        @Override
        public boolean areItemsTheSame(int oldPos, int newPos) {
            return oldList.get(oldPos).getMessageId().equals(newList.get(newPos).getMessageId());
        }

        @Override
        public boolean areContentsTheSame(int oldPos, int newPos) {
            return oldList.get(oldPos).equals(newList.get(newPos));
        }
    }

    public void cleanup() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        public final ConstraintLayout clMessage;
        public final LinearLayout llDateSeparator;
        public final TextView tvDateSeparator;
        public final LinearLayout llSent, llReceived;
        public final LinearLayout llSentImage, llReceivedImage;
        public final TextView tvSentMessage, tvSentMessageTime;
        public final TextView tvReceivedMessage, tvReceivedMessageTime;
        public final ShapeableImageView ivSent, ivReceived;
        public final TextView tvImageSentTime, tvImageReceivedTime;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            clMessage = itemView.findViewById(R.id.clMessage);
            llDateSeparator = itemView.findViewById(R.id.llDateSeparator);
            tvDateSeparator = itemView.findViewById(R.id.tvChatDate);
            llSent = itemView.findViewById(R.id.llSent);
            llReceived = itemView.findViewById(R.id.llReceived);
            llSentImage = itemView.findViewById(R.id.llSentImage);
            llReceivedImage = itemView.findViewById(R.id.llReceivedImage);
            tvSentMessage = itemView.findViewById(R.id.tvSentMessage);
            tvSentMessageTime = itemView.findViewById(R.id.tvSentMessageTime);
            tvReceivedMessage = itemView.findViewById(R.id.tvReceivedMessage);
            tvReceivedMessageTime = itemView.findViewById(R.id.tvReceivedMessageTime);
            ivSent = itemView.findViewById(R.id.ivSent);
            ivReceived = itemView.findViewById(R.id.ivReceived);
            tvImageSentTime = itemView.findViewById(R.id.tvSentImageTime);
            tvImageReceivedTime = itemView.findViewById(R.id.tvReceivedImageTime);
        }
    }

    private final ActionMode.Callback actionModeCallBack = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.menu_chat_options, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            String messageType = String.valueOf(selectedView.getTag(R.id.TAG_MESSAGE_TYPE));
            menu.findItem(R.id.mnuDownload).setVisible(!messageType.equals(Constants.MESSAGE_TYPE_TEXT));
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            String messageId = String.valueOf(selectedView.getTag(R.id.TAG_MESSAGE_ID));
            String message = String.valueOf(selectedView.getTag(R.id.TAG_MESSAGE));
            String messageType = String.valueOf(selectedView.getTag(R.id.TAG_MESSAGE_TYPE));

            if (context instanceof ChatActivity) {
                ChatActivity activity = (ChatActivity) context;
                int itemId = item.getItemId();

                if (itemId == R.id.mnuDelete) {
                    activity.deleteMessage(messageId, messageType);
                } else if (itemId == R.id.mnuDownload) {
                    activity.downloadFile(messageId, messageType, false);
                } else if (itemId == R.id.mnuShare) {
                    if (messageType.equals(Constants.MESSAGE_TYPE_TEXT)) {
                        shareTextMessage(message);
                    } else {
                        activity.downloadFile(messageId, messageType, true);
                    }
                } else if (itemId == R.id.mnuForward) {
                    activity.forwardMessage(messageId, message, messageType);
                }
            }
            mode.finish();
            return true;
        }

        private void shareTextMessage(String message) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, message);
            context.startActivity(Intent.createChooser(intent, "Share Message"));
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            actionMode = null;
            selectedView.setBackgroundColor(ContextCompat.getColor(context, R.color.chat_background));
        }
    };
}

//    package com.pm.appdev.duta.chats;
//
//    import android.annotation.SuppressLint;
//    import android.app.Activity;
//    import android.content.Context;
//    import android.content.Intent;
//    import android.graphics.Bitmap;
//    import android.graphics.BitmapFactory;
//
//    import androidx.appcompat.view.ActionMode;
//
//    import android.util.Base64;
//    import android.view.LayoutInflater;
//    import android.view.Menu;
//    import android.view.MenuInflater;
//    import android.view.MenuItem;
//    import android.view.View;
//    import android.view.ViewGroup;
//    import android.view.animation.AlphaAnimation;
//    import android.widget.ImageView;
//    import android.widget.LinearLayout;
//    import android.widget.TextView;
//    import android.widget.Toast;
//
//    import androidx.annotation.NonNull;
//    import androidx.appcompat.app.AppCompatActivity;
//    import androidx.constraintlayout.widget.ConstraintLayout;
//    import androidx.core.content.ContextCompat;
//    import androidx.core.content.FileProvider;
//    import androidx.recyclerview.widget.RecyclerView;
//
//    import com.pm.appdev.duta.Common.ImageRepository;
//    import com.pm.appdev.duta.R;
//    import com.pm.appdev.duta.Common.Constants;
//    import com.google.firebase.auth.FirebaseAuth;
//
//    import java.io.File;
//    import java.io.FileOutputStream;
//    import java.text.SimpleDateFormat;
//    import java.util.Date;
//    import java.util.List;
//    import java.util.Locale;
//    import java.util.Objects;
//    import android.net.Uri;
//    import java.io.IOException;
//
//    public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.MessageViewHolder> {
//
//        private final Context context;
//        private final List<MessageModel> messageList;
//
//        private ActionMode actionMode;
//        private ConstraintLayout selectedView;
//
//        public MessagesAdapter(Context context, List<MessageModel> messageList) {
//            this.context = context;
//            this.messageList = messageList;
//        }
//
//        @NonNull
//        @Override
//        public MessagesAdapter.MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//            View view = LayoutInflater.from(context).inflate(R.layout.message_layout, parent, false);
//            return new MessageViewHolder(view);
//        }
//
//        @SuppressLint("SetTextI18n")
//        @Override
//        public void onBindViewHolder(@NonNull final MessagesAdapter.MessageViewHolder holder, int position) {
//            MessageModel message = messageList.get(position);
//            FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
//            String currentUserId = Objects.requireNonNull(firebaseAuth.getCurrentUser()).getUid();
//            String fromUserId = message.getMessageFrom();
//
//            // Format time in 12-hour format with AM/PM
//            SimpleDateFormat sfd = new SimpleDateFormat("hh:mm a", Locale.getDefault());
//            String messageTime = sfd.format(new Date(message.getMessageTime()));
//
//            // Reset all views first to avoid overlap
//            holder.llSent.setVisibility(View.GONE);
//            holder.llSentImage.setVisibility(View.GONE);
//            holder.llReceived.setVisibility(View.GONE);
//            holder.llReceivedImage.setVisibility(View.GONE);
//
//            if (fromUserId.equals(currentUserId)) {
//                // Current user's sent message
//                bindSentMessage(holder, message, messageTime);
//            } else {
//                // Received message
//                bindReceivedMessage(holder, message, messageTime);
//            }
//
//            // Set common click listeners
//            holder.clMessage.setTag(R.id.TAG_MESSAGE, message.getMessage());
//            holder.clMessage.setTag(R.id.TAG_MESSAGE_ID, message.getMessageId());
//            holder.clMessage.setTag(R.id.TAG_MESSAGE_TYPE, message.getMessageType());
//
//            holder.clMessage.setOnClickListener(this::handleMessageClick);
//            holder.clMessage.setOnLongClickListener(view -> handleMessageLongClick(holder, view));
//        }
//
//        @SuppressLint("SetTextI18n")
//        private void bindSentMessage(MessageViewHolder holder, MessageModel message, String messageTime) {
//            switch (message.getMessageType()) {
//                case Constants.MESSAGE_TYPE_TEXT:
//                    holder.llSent.setVisibility(View.VISIBLE);
//                    holder.tvSentMessage.setText(message.getMessage());
//                    holder.tvSentMessageTime.setText(messageTime);
//                    break;
//
//                case Constants.MESSAGE_TYPE_IMAGE:
//                    holder.llSentImage.setVisibility(View.VISIBLE);
//                    holder.tvImageSentTime.setText(messageTime);
//                    loadImage(message.getMessage(), holder.ivSent, true);
//                    break;
//
//                case Constants.MESSAGE_TYPE_VIDEO:
//                    holder.llSent.setVisibility(View.VISIBLE);
//                    holder.tvSentMessage.setText("[Video]");
//                    holder.tvSentMessageTime.setText(messageTime);
//                    break;
//            }
//        }
//
//        @SuppressLint("SetTextI18n")
//        private void bindReceivedMessage(MessageViewHolder holder, MessageModel message, String messageTime) {
//            switch (message.getMessageType()) {
//                case Constants.MESSAGE_TYPE_TEXT:
//                    holder.llReceived.setVisibility(View.VISIBLE);
//                    holder.tvReceivedMessage.setText(message.getMessage());
//                    holder.tvReceivedMessageTime.setText(messageTime);
//                    break;
//
//                case Constants.MESSAGE_TYPE_IMAGE:
//                    holder.llReceivedImage.setVisibility(View.VISIBLE);
//                    holder.tvImageReceivedTime.setText(messageTime);
//                    loadImage(message.getMessage(), holder.ivReceived, false);
//                    break;
//
//                case Constants.MESSAGE_TYPE_VIDEO:
//                    holder.llReceived.setVisibility(View.VISIBLE);
//                    holder.tvReceivedMessage.setText("[Video]");
//                    holder.tvReceivedMessageTime.setText(messageTime);
//                    break;
//            }
//        }
//
//        private void loadImage(String imageData, ImageView imageView, boolean isSent) {
//            imageView.setImageResource(R.drawable.ic_image_placeholder);
//
//            new Thread(() -> {
//                try {
//                    byte[] decodedString = Base64.decode(imageData, Base64.DEFAULT);
//                    final Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
//
//                    ((Activity) context).runOnUiThread(() -> {
//                        AlphaAnimation fadeIn = new AlphaAnimation(0.2f, 1.0f);
//                        fadeIn.setDuration(300);
//                        imageView.setImageBitmap(decodedBitmap);
//                        imageView.startAnimation(fadeIn);
//                    });
//                } catch (Exception e) {
//                    ((Activity) context).runOnUiThread(() -> {
//                        imageView.setImageResource(R.drawable.ic_broken_image);
//                    });
//                }
//            }).start();
//        }
//
//        // In MessagesAdapter.java
//        private void handleMessageClick(View view) {
//            String messageType = view.getTag(R.id.TAG_MESSAGE_TYPE).toString();
//            String messageContent = view.getTag(R.id.TAG_MESSAGE).toString();
//
//            if (messageType.equals(Constants.MESSAGE_TYPE_VIDEO)) {
//                playVideo(messageContent);
//            } else if (messageType.equals(Constants.MESSAGE_TYPE_IMAGE)) {
//                // Decode the Base64 image in background thread
//                new Thread(() -> {
//                    try {
//                        byte[] decodedString = Base64.decode(messageContent, Base64.DEFAULT);
//                        final Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
//
//                        // Run on UI thread to start activity
//                        ((Activity) context).runOnUiThread(() -> {
//                            // Store bitmap in repository
//                            ImageRepository.setCurrentImage(bitmap);
//
//                            // Start FullScreenImageActivity
//                            Intent intent = new Intent(context, FullScreenImageActivity.class);
//                            context.startActivity(intent);
//                        });
//                    } catch (Exception e) {
//                        ((Activity) context).runOnUiThread(() -> {
//                            Toast.makeText(context, "Failed to load image", Toast.LENGTH_SHORT).show();
//                        });
//                    }
//                }).start();
//            }
//        }
//
//        private boolean handleMessageLongClick(MessageViewHolder holder, View view) {
//            if (actionMode != null) {
//                return false;
//            }
//            selectedView = holder.clMessage;
//            actionMode = ((AppCompatActivity) context).startSupportActionMode(actionModeCallBack);
//            view.setBackgroundColor(ContextCompat.getColor(context, R.color.colorAccent));
//            return true;
//        }
//
//        private void playVideo(String videoBase64) {
//            try {
//                byte[] videoBytes = Base64.decode(videoBase64, Base64.DEFAULT);
//                File tempFile = File.createTempFile("video", ".mp4", context.getCacheDir());
//                try (FileOutputStream fos = new FileOutputStream(tempFile)) {
//                    fos.write(videoBytes);
//                }
//
//                Intent intent = new Intent(Intent.ACTION_VIEW);
//                intent.setDataAndType(FileProvider.getUriForFile(context,
//                        context.getPackageName() + ".provider", tempFile), "video/mp4");
//                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//                context.startActivity(intent);
//            } catch (Exception e) {
//                Toast.makeText(context, "Couldn't play video", Toast.LENGTH_SHORT).show();
//            }
//        }
//
//        @Override
//        public int getItemCount() {
//            return messageList.size();
//        }
//
//        public static class MessageViewHolder extends RecyclerView.ViewHolder {
//
//            private final LinearLayout llSent;
//            private final LinearLayout llReceived;
//            private final LinearLayout llSentImage;
//            private final LinearLayout llReceivedImage;
//            private final TextView tvSentMessage, tvSentMessageTime, tvReceivedMessage, tvReceivedMessageTime;
//            private final ImageView ivSent, ivReceived;
//            private final TextView tvImageSentTime, tvImageReceivedTime;
//            private final ConstraintLayout clMessage;
//
//            public MessageViewHolder(@NonNull View itemView) {
//                super(itemView);
//
//                llSent = itemView.findViewById(R.id.llSent);
//                llReceived = itemView.findViewById(R.id.llReceived);
//                tvSentMessage = itemView.findViewById(R.id.tvSentMessage);
//                tvSentMessageTime = itemView.findViewById(R.id.tvSentMessageTime);
//
//                tvReceivedMessage = itemView.findViewById(R.id.tvReceivedMessage);
//                tvReceivedMessageTime = itemView.findViewById(R.id.tvReceivedMessageTime);
//
//                clMessage = itemView.findViewById(R.id.clMessage);
//
//                llSentImage = itemView.findViewById(R.id.llSentImage);
//                llReceivedImage = itemView.findViewById(R.id.llReceivedImage);
//                ivSent = itemView.findViewById(R.id.ivSent);
//                ivReceived = itemView.findViewById(R.id.ivReceived);
//
//                tvImageSentTime = itemView.findViewById(R.id.tvSentImageTime);
//                tvImageReceivedTime = itemView.findViewById(R.id.tvReceivedImageTime);
//
//            }
//        }
//
//        public ActionMode.Callback actionModeCallBack = new ActionMode.Callback() {
//            @Override
//            public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
//                // Load the menu resource like an object
//                MenuInflater inflater = actionMode.getMenuInflater();
//                inflater.inflate(R.menu.menu_chat_options, menu);
//
//                String selectedMessageType = String.valueOf(selectedView.getTag(R.id.TAG_MESSAGE_TYPE));
//                if (selectedMessageType.equals(Constants.MESSAGE_TYPE_TEXT)) {
//                    MenuItem itemDownload = menu.findItem(R.id.mnuDownload);
//                    itemDownload.setVisible(false);
//                }
//                return true;
//            }
//
//            @Override
//            public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
//                return false;
//            }
//
//            @Override
//            public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
//                String selectedMessageId = String.valueOf(selectedView.getTag(R.id.TAG_MESSAGE_ID));
//                String selectedMessage = String.valueOf(selectedView.getTag(R.id.TAG_MESSAGE));
//                String selectedMessageType = String.valueOf(selectedView.getTag(R.id.TAG_MESSAGE_TYPE));
//
//                int itemId = menuItem.getItemId();
//
//                // Replace switch with if-else to avoid "Constant expression required" error
//                if (itemId == R.id.mnuDelete) {
//                    if (context instanceof ChatActivity) {
//                        ((ChatActivity) context).deleteMessage(selectedMessageId, selectedMessageType);
//                    }
//                    actionMode.finish();
//                } else if (itemId == R.id.mnuDownload) {
//                    if (context instanceof ChatActivity) {
//                        ((ChatActivity) context).downloadFile(selectedMessageId, selectedMessageType, false);
//                    }
//                    actionMode.finish();
//                } else if (itemId == R.id.mnuShare) {
//                    if (selectedMessageType.equals(Constants.MESSAGE_TYPE_TEXT)) {
//                        Intent intentShare = new Intent();
//                        intentShare.setAction(Intent.ACTION_SEND);
//                        intentShare.putExtra(Intent.EXTRA_TEXT, selectedMessage);
//                        intentShare.setType("text/plain");
//                        context.startActivity(intentShare);
//                    } else {
//                        if (context instanceof ChatActivity) {
//                            ((ChatActivity) context).downloadFile(selectedMessageId, selectedMessageType, true);
//                        }
//                    }
//                    actionMode.finish();
//                } else if (itemId == R.id.mnuForward) {
//                    if (context instanceof ChatActivity) {
//                        ((ChatActivity) context).forwardMessage(selectedMessageId, selectedMessage, selectedMessageType);
//                    }
//                    actionMode.finish();
//                }
//
//                return false;
//            }
//
//            @Override
//            public void onDestroyActionMode(ActionMode actionMode) {
//                actionMode = null;
//                selectedView.setBackgroundColor(context.getResources().getColor(R.color.chat_background));
//            }
//        };
//    }