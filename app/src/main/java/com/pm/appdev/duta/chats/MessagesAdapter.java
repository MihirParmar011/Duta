package com.pm.appdev.duta.chats;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.appcompat.view.ActionMode;

import android.util.Base64;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.pm.appdev.duta.Common.ImageRepository;
import com.pm.appdev.duta.R;
import com.pm.appdev.duta.Common.Constants;
import com.google.firebase.auth.FirebaseAuth;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import android.net.Uri;
import java.io.IOException;

public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.MessageViewHolder> {

    private final Context context;
    private final List<MessageModel> messageList;

    private ActionMode actionMode;
    private ConstraintLayout selectedView;

    public MessagesAdapter(Context context, List<MessageModel> messageList) {
        this.context = context;
        this.messageList = messageList;
    }

    @NonNull
    @Override
    public MessagesAdapter.MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.message_layout, parent, false);
        return new MessageViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull final MessagesAdapter.MessageViewHolder holder, int position) {
        MessageModel message = messageList.get(position);
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        String currentUserId = Objects.requireNonNull(firebaseAuth.getCurrentUser()).getUid();
        String fromUserId = message.getMessageFrom();

        // Format time in 12-hour format with AM/PM
        SimpleDateFormat sfd = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        String messageTime = sfd.format(new Date(message.getMessageTime()));

        // Reset all views first to avoid overlap
        holder.llSent.setVisibility(View.GONE);
        holder.llSentImage.setVisibility(View.GONE);
        holder.llReceived.setVisibility(View.GONE);
        holder.llReceivedImage.setVisibility(View.GONE);

        if (fromUserId.equals(currentUserId)) {
            // Current user's sent message
            bindSentMessage(holder, message, messageTime);
        } else {
            // Received message
            bindReceivedMessage(holder, message, messageTime);
        }

        // Set common click listeners
        holder.clMessage.setTag(R.id.TAG_MESSAGE, message.getMessage());
        holder.clMessage.setTag(R.id.TAG_MESSAGE_ID, message.getMessageId());
        holder.clMessage.setTag(R.id.TAG_MESSAGE_TYPE, message.getMessageType());

        holder.clMessage.setOnClickListener(this::handleMessageClick);
        holder.clMessage.setOnLongClickListener(view -> handleMessageLongClick(holder, view));
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
                loadImage(message.getMessage(), holder.ivSent, true);
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
                loadImage(message.getMessage(), holder.ivReceived, false);
                break;

            case Constants.MESSAGE_TYPE_VIDEO:
                holder.llReceived.setVisibility(View.VISIBLE);
                holder.tvReceivedMessage.setText("[Video]");
                holder.tvReceivedMessageTime.setText(messageTime);
                break;
        }
    }

    private void loadImage(String imageData, ImageView imageView, boolean isSent) {
        imageView.setImageResource(R.drawable.ic_image_placeholder);

        new Thread(() -> {
            try {
                byte[] decodedString = Base64.decode(imageData, Base64.DEFAULT);
                final Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                ((Activity) context).runOnUiThread(() -> {
                    AlphaAnimation fadeIn = new AlphaAnimation(0.2f, 1.0f);
                    fadeIn.setDuration(300);
                    imageView.setImageBitmap(decodedBitmap);
                    imageView.startAnimation(fadeIn);
                });
            } catch (Exception e) {
                ((Activity) context).runOnUiThread(() -> {
                    imageView.setImageResource(R.drawable.ic_broken_image);
                });
            }
        }).start();
    }

    // In MessagesAdapter.java
    private void handleMessageClick(View view) {
        String messageType = view.getTag(R.id.TAG_MESSAGE_TYPE).toString();
        String messageContent = view.getTag(R.id.TAG_MESSAGE).toString();

        if (messageType.equals(Constants.MESSAGE_TYPE_VIDEO)) {
            playVideo(messageContent);
        } else if (messageType.equals(Constants.MESSAGE_TYPE_IMAGE)) {
            // Decode the Base64 image in background thread
            new Thread(() -> {
                try {
                    byte[] decodedString = Base64.decode(messageContent, Base64.DEFAULT);
                    final Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                    // Run on UI thread to start activity
                    ((Activity) context).runOnUiThread(() -> {
                        // Store bitmap in repository
                        ImageRepository.setCurrentImage(bitmap);

                        // Start FullScreenImageActivity
                        Intent intent = new Intent(context, FullScreenImageActivity.class);
                        context.startActivity(intent);
                    });
                } catch (Exception e) {
                    ((Activity) context).runOnUiThread(() -> {
                        Toast.makeText(context, "Failed to load image", Toast.LENGTH_SHORT).show();
                    });
                }
            }).start();
        }
    }

    private boolean handleMessageLongClick(MessageViewHolder holder, View view) {
        if (actionMode != null) {
            return false;
        }
        selectedView = holder.clMessage;
        actionMode = ((AppCompatActivity) context).startSupportActionMode(actionModeCallBack);
        view.setBackgroundColor(ContextCompat.getColor(context, R.color.colorAccent));
        return true;
    }

    private void playVideo(String videoBase64) {
        try {
            byte[] videoBytes = Base64.decode(videoBase64, Base64.DEFAULT);
            File tempFile = File.createTempFile("video", ".mp4", context.getCacheDir());
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(videoBytes);
            }

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(FileProvider.getUriForFile(context,
                    context.getPackageName() + ".provider", tempFile), "video/mp4");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "Couldn't play video", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {

        private final LinearLayout llSent;
        private final LinearLayout llReceived;
        private final LinearLayout llSentImage;
        private final LinearLayout llReceivedImage;
        private final TextView tvSentMessage, tvSentMessageTime, tvReceivedMessage, tvReceivedMessageTime;
        private final ImageView ivSent, ivReceived;
        private final TextView tvImageSentTime, tvImageReceivedTime;
        private final ConstraintLayout clMessage;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);

            llSent = itemView.findViewById(R.id.llSent);
            llReceived = itemView.findViewById(R.id.llReceived);
            tvSentMessage = itemView.findViewById(R.id.tvSentMessage);
            tvSentMessageTime = itemView.findViewById(R.id.tvSentMessageTime);

            tvReceivedMessage = itemView.findViewById(R.id.tvReceivedMessage);
            tvReceivedMessageTime = itemView.findViewById(R.id.tvReceivedMessageTime);

            clMessage = itemView.findViewById(R.id.clMessage);

            llSentImage = itemView.findViewById(R.id.llSentImage);
            llReceivedImage = itemView.findViewById(R.id.llReceivedImage);
            ivSent = itemView.findViewById(R.id.ivSent);
            ivReceived = itemView.findViewById(R.id.ivReceived);

            tvImageSentTime = itemView.findViewById(R.id.tvSentImageTime);
            tvImageReceivedTime = itemView.findViewById(R.id.tvReceivedImageTime);

        }
    }

    public ActionMode.Callback actionModeCallBack = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            // Load the menu resource like an object
            MenuInflater inflater = actionMode.getMenuInflater();
            inflater.inflate(R.menu.menu_chat_options, menu);

            String selectedMessageType = String.valueOf(selectedView.getTag(R.id.TAG_MESSAGE_TYPE));
            if (selectedMessageType.equals(Constants.MESSAGE_TYPE_TEXT)) {
                MenuItem itemDownload = menu.findItem(R.id.mnuDownload);
                itemDownload.setVisible(false);
            }
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
            String selectedMessageId = String.valueOf(selectedView.getTag(R.id.TAG_MESSAGE_ID));
            String selectedMessage = String.valueOf(selectedView.getTag(R.id.TAG_MESSAGE));
            String selectedMessageType = String.valueOf(selectedView.getTag(R.id.TAG_MESSAGE_TYPE));

            int itemId = menuItem.getItemId();

            // Replace switch with if-else to avoid "Constant expression required" error
            if (itemId == R.id.mnuDelete) {
                if (context instanceof ChatActivity) {
                    ((ChatActivity) context).deleteMessage(selectedMessageId, selectedMessageType);
                }
                actionMode.finish();
            } else if (itemId == R.id.mnuDownload) {
                if (context instanceof ChatActivity) {
                    ((ChatActivity) context).downloadFile(selectedMessageId, selectedMessageType, false);
                }
                actionMode.finish();
            } else if (itemId == R.id.mnuShare) {
                if (selectedMessageType.equals(Constants.MESSAGE_TYPE_TEXT)) {
                    Intent intentShare = new Intent();
                    intentShare.setAction(Intent.ACTION_SEND);
                    intentShare.putExtra(Intent.EXTRA_TEXT, selectedMessage);
                    intentShare.setType("text/plain");
                    context.startActivity(intentShare);
                } else {
                    if (context instanceof ChatActivity) {
                        ((ChatActivity) context).downloadFile(selectedMessageId, selectedMessageType, true);
                    }
                }
                actionMode.finish();
            } else if (itemId == R.id.mnuForward) {
                if (context instanceof ChatActivity) {
                    ((ChatActivity) context).forwardMessage(selectedMessageId, selectedMessage, selectedMessageType);
                }
                actionMode.finish();
            }

            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            actionMode = null;
            selectedView.setBackgroundColor(context.getResources().getColor(R.color.chat_background));
        }
    };
}