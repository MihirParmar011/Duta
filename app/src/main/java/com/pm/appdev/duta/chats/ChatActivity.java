package com.pm.appdev.duta.chats;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.pm.appdev.duta.Common.Extras;
import com.pm.appdev.duta.Common.NodeNames;
import com.pm.appdev.duta.Common.Util;
import com.pm.appdev.duta.Common.Constants;
import com.pm.appdev.duta.R;
import com.pm.appdev.duta.selectfriend.SelectFriendActivity;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipDrawable;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.mlkit.nl.smartreply.SmartReply;
import com.google.mlkit.nl.smartreply.SmartReplyGenerator;
import com.google.mlkit.nl.smartreply.SmartReplySuggestion;
import com.google.mlkit.nl.smartreply.SmartReplySuggestionResult;
import com.google.mlkit.nl.smartreply.TextMessage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static android.provider.MediaStore.ACTION_IMAGE_CAPTURE;

public class ChatActivity extends AppCompatActivity implements View.OnClickListener {

    private ImageView ivSend, ivAttachment, ivProfile;
    private TextView tvUserName, tvUserStatus;
    private EditText etMessage;
    private DatabaseReference mRootRef;
    private String currentUserId, chatUserId;
    private RecyclerView rvMessages;
    private SwipeRefreshLayout srlMessages;
    private MessagesAdapter messagesAdapter;
    private List<MessageModel> messagesList;
    private int currentPage = 1;
    private static final int RECORD_PER_PAGE = 30;
    private static final int REQUEST_CODE_PICK_IMAGE = 101;
    private static final int REQUEST_CODE_PICK_VIDEO = 103;
    private static final int REQUEST_CODE_CAPTURE_IMAGE = 102;
    private static final int REQUEST_CODE_FORWARD_MESSAGE = 104;
    private ChildEventListener childEventListener;
    private BottomSheetDialog bottomSheetDialog;
    private LinearLayout llProgress;
    private String userName, photoName;
    private ChipGroup cgSmartReplies;
    private List<TextMessage> conversation;
    private ActivityResultLauncher<Intent> captureImageLauncher;
    private ActivityResultLauncher<Intent> pickImageLauncher;
    private ActivityResultLauncher<Intent> pickVideoLauncher;
    private ActivityResultLauncher<Intent> forwardMessageLauncher;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Check if User ID is null and close activity if it is
        chatUserId = getIntent().getStringExtra(Extras.USER_KEY);
        if (chatUserId == null) {
            Log.e("ChatActivity", "User ID is null, closing activity.");
            finish(); // Close activity to prevent crash
            return;
        }

        Log.d("ChatActivity", "chatUserId: " + chatUserId);

        // Set up Toolbar as ActionBar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Enable the back button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Initialize UI components
        initializeUI();

        // Fetch user details from Firebase
        fetchUserDetails(chatUserId);


        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                            encodeAndSendImage(bitmap, Constants.MESSAGE_TYPE_IMAGE);
                        } catch (IOException e) {
                            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        pickVideoLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        try {
                            byte[] videoBytes = readBytesFromUri(uri);
                            if (videoBytes != null) {
                                String videoBase64 = Base64.encodeToString(videoBytes, Base64.DEFAULT);
                                DatabaseReference messageRef = mRootRef.child(NodeNames.MESSAGES)
                                        .child(currentUserId).child(chatUserId).push();
                                String pushId = messageRef.getKey();
                                sendMessage(videoBase64, Constants.MESSAGE_TYPE_VIDEO, pushId);
                            }
                        } catch (IOException e) {
                            Toast.makeText(this, "Failed to load video", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        captureImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Bitmap bitmap = (Bitmap) Objects.requireNonNull(result.getData().getExtras()).get("data");
                        encodeAndSendImage(bitmap, Constants.MESSAGE_TYPE_IMAGE);
                    }
                });

        forwardMessageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        Intent intent = new Intent(this, ChatActivity.class);
                        intent.putExtra(Extras.USER_KEY, data.getStringExtra(Extras.USER_KEY));
                        intent.putExtra(Extras.USER_NAME, data.getStringExtra(Extras.USER_NAME));
                        intent.putExtra(Extras.PHOTO_NAME, data.getStringExtra(Extras.PHOTO_NAME));
                        intent.putExtra(Extras.MESSAGE, data.getStringExtra(Extras.MESSAGE));
                        intent.putExtra(Extras.MESSAGE_ID, data.getStringExtra(Extras.MESSAGE_ID));
                        intent.putExtra(Extras.MESSAGE_TYPE, data.getStringExtra(Extras.MESSAGE_TYPE));
                        startActivity(intent);
                        finish();
                    }
                });

    }

    private void fetchUserDetails(String targetUid) {
        if (targetUid == null) {
            Log.e("ChatActivity", "UID is null");
            return;
        }

        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("Users");

        // Query: Find the user where "uid" field matches targetUid
        Query query = usersRef.orderByChild("uid").equalTo(targetUid);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Loop through results (should be 1 match)
                    for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                        String userName = userSnapshot.child("name").getValue(String.class);
                        String photoName = userSnapshot.child("photo").getValue(String.class);

                        runOnUiThread(() -> {
                            // Update UI (same as your original code)
                            if (tvUserName != null) {
                                tvUserName.setText(userName != null ? userName : "Unknown User");
                            }
                            if (ivProfile != null) {
                                if (!TextUtils.isEmpty(photoName)) {
                                    try {
                                        byte[] decodedString = Base64.decode(photoName, Base64.DEFAULT);
                                        Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                                        ivProfile.setImageBitmap(decodedBitmap);
                                    } catch (Exception e) {
                                        ivProfile.setImageResource(R.drawable.default_profile);
                                    }
                                } else {
                                    ivProfile.setImageResource(R.drawable.default_profile);
                                }
                            }
                        });
                        break; // Exit after first match (UIDs should be unique)
                    }
                } else {
                    Log.e("ChatActivity", "User with UID not found: " + targetUid);
                    runOnUiThread(() -> {
                        if (tvUserName != null) tvUserName.setText("Unknown User");
                        if (ivProfile != null) ivProfile.setImageResource(R.drawable.default_profile);
                    });
                }
            }

            @SuppressLint("SetTextI18n")
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("ChatActivity", "Query failed: " + databaseError.getMessage());
                runOnUiThread(() -> {
                    if (tvUserName != null) tvUserName.setText("Unknown User");
                    if (ivProfile != null) ivProfile.setImageResource(R.drawable.default_profile);
                });
            }
        });
    }

    private void initializeUI() {
        // Initialize Toolbar and Action Bar
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
                getSupportActionBar().setDisplayShowTitleEnabled(false);
            }
        } else {
            Log.e("ChatActivity", "Toolbar is null");
        }

        // Inflate custom action bar layout
        @SuppressLint("InflateParams") ViewGroup actionBarLayout = (ViewGroup) getLayoutInflater().inflate(R.layout.custom_action_bar, null);
        if (toolbar != null) {
            toolbar.addView(actionBarLayout);
        }

        // Initialize views from the custom action bar layout
        ivProfile = findViewById(R.id.ivProfile);
        tvUserName = findViewById(R.id.tvUserName);
        tvUserStatus = actionBarLayout.findViewById(R.id.tvUserStatus);

        // Check if views are properly initialized
        if (ivProfile == null) {
            Log.e("ChatActivity", "ivProfile is null");
        }
        if (tvUserName == null) {
            Log.e("ChatActivity", "tvUserName is null");
        }
        if (tvUserStatus == null) {
            Log.e("ChatActivity", "tvUserStatus is null");
        }

        // Fetch and display user details (name and photo)
        if (chatUserId != null) {
            fetchUserDetails(chatUserId); // Pass the correct user ID
        } else {
            Log.e("ChatActivity", "chatUser Id is null");
        }

        // Initialize other UI components
        cgSmartReplies = findViewById(R.id.cgSmartReplies);
        if (cgSmartReplies == null) {
            Log.e("ChatActivity", "cgSmartReplies is null");
        }

        conversation = new ArrayList<>();

        ivSend = findViewById(R.id.ivSend);
        ivAttachment = findViewById(R.id.ivAttachment);
        etMessage = findViewById(R.id.etMessage);

        llProgress = findViewById(R.id.llProgress);

        if (ivSend == null || ivAttachment == null || etMessage == null || llProgress == null) {
            Log.e("ChatActivity", "One or more UI components are null");
        }

        ivSend.setOnClickListener(this);
        ivAttachment.setOnClickListener(this);

        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        mRootRef = FirebaseDatabase.getInstance().getReference();
        currentUserId = Objects.requireNonNull(firebaseAuth.getCurrentUser()).getUid();

        // Initialize RecyclerView and SwipeRefreshLayout
        rvMessages = findViewById(R.id.rvMessages);
        srlMessages = findViewById(R.id.srlMessages);

        if (rvMessages == null || srlMessages == null) {
            Log.e("ChatActivity", "RecyclerView or SwipeRefreshLayout is null");
        }

        // Initialize messagesList and messagesAdapter
        messagesList = new ArrayList<>();
        messagesAdapter = new MessagesAdapter(this, messagesList);

        // Set up RecyclerView
        rvMessages.setLayoutManager(new LinearLayoutManager(this));
        rvMessages.setAdapter(messagesAdapter);

        // Load messages and set up other Firebase listeners
        loadMessages();

        mRootRef.child("Chats").child(currentUserId).child(chatUserId).child("unread_count").setValue(0);

        rvMessages.scrollToPosition(messagesList.size() - 1);

        srlMessages.setOnRefreshListener(() -> {
            currentPage++;
            loadMessages();
        });

        bottomSheetDialog = new BottomSheetDialog(this);
        @SuppressLint("InflateParams") View view = getLayoutInflater().inflate(R.layout.chat_file_options, null);
        view.findViewById(R.id.llCamera).setOnClickListener(this);
        view.findViewById(R.id.llGallery).setOnClickListener(this);
        view.findViewById(R.id.llVideo).setOnClickListener(this);
        view.findViewById(R.id.ivClose).setOnClickListener(this);
        bottomSheetDialog.setContentView(view);

        // Replace the file forwarding section with Base64 approach
        if (getIntent().hasExtra(Extras.MESSAGE) && getIntent().hasExtra(Extras.MESSAGE_ID) && getIntent().hasExtra(Extras.MESSAGE_TYPE)) {
            String message = getIntent().getStringExtra(Extras.MESSAGE);
            String messageId = getIntent().getStringExtra(Extras.MESSAGE_ID);
            final String messageType = getIntent().getStringExtra(Extras.MESSAGE_TYPE);

            DatabaseReference messageRef = mRootRef.child("Messages").child(currentUserId).child(chatUserId).push();
            final String newMessageId = messageRef.getKey();

            // For forwarded messages, just send them directly (they should already be in Base64 format)
            sendMessage(message, messageType, newMessageId);
        }

        // Set up user status listener with null checks
        DatabaseReference databaseReferenceUsers = mRootRef.child("Users").child(chatUserId);
        databaseReferenceUsers.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String status = "";
                if (dataSnapshot.child("online").getValue() != null)
                    status = Objects.requireNonNull(dataSnapshot.child("online").getValue()).toString();

                if (tvUserStatus != null) {
                    if (status.equals("true"))
                        tvUserStatus.setText(Constants.STATUS_ONLINE);
                    else
                        tvUserStatus.setText(Constants.STATUS_OFFLINE);
                } else {
                    Log.e("ChatActivity", "tvUserStatus is null in onDataChange");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("ChatActivity", "Failed to fetch user status: " + databaseError.getMessage());
            }
        });

        // Set up typing status listener with null checks
        DatabaseReference chatUserRef = mRootRef.child("Chats").child(chatUserId).child(currentUserId);
        chatUserRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.child("typing").getValue() != null) {
                    String typingStatus = Objects.requireNonNull(dataSnapshot.child("typing").getValue()).toString();

                    if (tvUserStatus != null) {
                        if (typingStatus.equals(Constants.TYPING_STARTED))
                            tvUserStatus.setText(Constants.STATUS_TYPING);
                        else
                            tvUserStatus.setText(Constants.STATUS_ONLINE);
                    } else {
                        Log.e("ChatActivity", "tvUserStatus is null in onDataChange");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("ChatActivity", "Failed to fetch typing status: " + databaseError.getMessage());
            }
        });

        // Set up text change listener for typing indicator
        etMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                DatabaseReference currentUserRef = mRootRef.child("Chats").child(currentUserId).child(chatUserId);
                if (editable.toString().matches("")) {
                    currentUserRef.child("typing").setValue(Constants.TYPING_STOPPED);
                } else {
                    currentUserRef.child("typing").setValue(Constants.TYPING_STARTED);
                }
            }
        });
    }

    private void encodeAndSendImage(Bitmap bitmap, String messageType) {
        // Show progress
        llProgress.setVisibility(View.VISIBLE);

        new Thread(() -> {
            try {
                // Compress and encode image in background thread
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                byte[] imageBytes = baos.toByteArray();
                String imageBase64 = Base64.encodeToString(imageBytes, Base64.DEFAULT);

                // Create message reference
                DatabaseReference messageRef = mRootRef.child(NodeNames.MESSAGES)
                        .child(currentUserId).child(chatUserId).push();
                String pushId = messageRef.getKey();

                // Send message on UI thread
                runOnUiThread(() -> {
                    sendMessage(imageBase64, messageType, pushId);
                    llProgress.setVisibility(View.GONE);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Failed to send image", Toast.LENGTH_SHORT).show();
                    llProgress.setVisibility(View.GONE);
                });
            }
        }).start();
    }

    private byte[] readBytesFromUri(Uri uri) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();

        if (inputStream != null) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len);
            }
            inputStream.close();
            return byteBuffer.toByteArray();
        }
        return null;
    }

    @SuppressLint("NotifyDataSetChanged")
    private void sendMessage(final String msg, final String msgType, String pushId) {
        try {
            if (!TextUtils.isEmpty(msg)) {
                // Clear input immediately
                etMessage.setText("");

                // Create temporary message for local display
                MessageModel tempMessage = new MessageModel();
                tempMessage.setMessageId(pushId);
                tempMessage.setMessage(msg);
                tempMessage.setMessageType(msgType);
                tempMessage.setMessageFrom(currentUserId);
                tempMessage.setMessageTime(System.currentTimeMillis());

                // Add to pending messages map
                if (childEventListener instanceof ChildEventListenerWithPending) {
                    ((ChildEventListenerWithPending) childEventListener).addPendingMessage(pushId, tempMessage);
                }

                // Add to UI immediately (will be replaced when Firebase confirms)
                messagesList.add(tempMessage);
                messagesAdapter.notifyItemInserted(messagesList.size() - 1);
                rvMessages.scrollToPosition(messagesList.size() - 1);

                // Create message data for Firebase
                HashMap<String, Object> messageMap = new HashMap<>();
                messageMap.put(NodeNames.MESSAGE_ID, pushId);
                messageMap.put(NodeNames.MESSAGE, msg);
                messageMap.put(NodeNames.MESSAGE_TYPE, msgType);
                messageMap.put(NodeNames.MESSAGE_FROM, currentUserId);
                messageMap.put(NodeNames.MESSAGE_TIME, ServerValue.TIMESTAMP);

                String currentUserRef = NodeNames.MESSAGES + "/" + currentUserId + "/" + chatUserId;
                String chatUserRef = NodeNames.MESSAGES + "/" + chatUserId + "/" + currentUserId;

                HashMap<String, Object> messageUserMap = new HashMap<>();
                messageUserMap.put(currentUserRef + "/" + pushId, messageMap);
                messageUserMap.put(chatUserRef + "/" + pushId, messageMap);

                // Update Firebase
                mRootRef.updateChildren(messageUserMap, (databaseError, databaseReference) -> {
                    if (databaseError != null) {
                        // Remove failed message from UI
                        messagesList.removeIf(m -> m.getMessageId().equals(pushId));
                        messagesAdapter.notifyDataSetChanged();

                        Toast.makeText(ChatActivity.this,
                                getString(R.string.failed_to_send_message, databaseError.getMessage()),
                                Toast.LENGTH_SHORT).show();
                    } else {
                        // Success - the real message will appear via onChildAdded
                        // Send notification
                        String notificationTitle = "New Message";
                        if (msgType.equals(Constants.MESSAGE_TYPE_IMAGE)) {
                            notificationTitle = "New Image";
                        } else if (msgType.equals(Constants.MESSAGE_TYPE_VIDEO)) {
                            notificationTitle = "New Video";
                        }

                        Util.sendNotification(ChatActivity.this, notificationTitle,
                                msgType.equals(Constants.MESSAGE_TYPE_TEXT) ? msg : notificationTitle,
                                chatUserId);

                        // Update last message in chat list
                        String lastMessage = msgType.equals(Constants.MESSAGE_TYPE_TEXT) ? msg : notificationTitle;
                        Util.updateChatDetails(ChatActivity.this, currentUserId, chatUserId, lastMessage);
                    }
                });
            }
        } catch (Exception ex) {
            Toast.makeText(ChatActivity.this,
                    getString(R.string.failed_to_send_message, ex.getMessage()),
                    Toast.LENGTH_SHORT).show();
        }
    }

    // Helper interface for pending messages
    private interface ChildEventListenerWithPending extends ChildEventListener {
        void addPendingMessage(String messageId, MessageModel message);
    }

    private void loadMessages() {
        messagesList.clear();
        conversation.clear();
        cgSmartReplies.removeAllViews();

        DatabaseReference databaseReferenceMessages = mRootRef.child(NodeNames.MESSAGES)
                .child(currentUserId)
                .child(chatUserId);

        // Order by timestamp to ensure correct ordering
        Query messageQuery = databaseReferenceMessages.orderByChild(NodeNames.MESSAGE_TIME)
                .limitToLast(currentPage * RECORD_PER_PAGE);

        if (childEventListener != null) {
            messageQuery.removeEventListener(childEventListener);
        }

        childEventListener = new ChildEventListener() {
            private final Map<String, MessageModel> pendingMessages = new HashMap<>();

            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                MessageModel message = dataSnapshot.getValue(MessageModel.class);
                if (message != null) {
                    // Check if this is a pending message we sent
                    pendingMessages.remove(message.getMessageId());

                    // Add to UI only if not already present
                    if (!containsMessage(messagesList, message.getMessageId())) {
                        messagesList.add(message);
                        // Sort by timestamp (just in case)
                        messagesList.sort(Comparator.comparingLong(MessageModel::getMessageTime));

                        messagesAdapter.notifyDataSetChanged();
                        rvMessages.scrollToPosition(messagesList.size() - 1);
                        srlMessages.setRefreshing(false);

                        // Show smart replies only for received text messages
                        if (message.getMessageFrom().equals(chatUserId) &&
                                message.getMessageType().equals(Constants.MESSAGE_TYPE_TEXT)) {
                            showSmartReplies(message);
                        }
                    }
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                // Handle message updates if needed
            }

            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                // Handle message deletion
                MessageModel message = dataSnapshot.getValue(MessageModel.class);
                if (message != null) {
                    messagesList.removeIf(m -> m.getMessageId().equals(message.getMessageId()));
                    messagesAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                // Not needed for basic chat
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                srlMessages.setRefreshing(false);
                Toast.makeText(ChatActivity.this,
                        "Failed to load messages: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }

            // Helper to track pending messages
            public void addPendingMessage(String messageId, MessageModel message) {
                pendingMessages.put(messageId, message);
            }
        };

        messageQuery.addChildEventListener(childEventListener);
    }

    private boolean containsMessage(List<MessageModel> messages, String messageId) {
        for (MessageModel message : messages) {
            if (message.getMessageId().equals(messageId)) {
                return true;
            }
        }
        return false;
    }

    private boolean isMessageExists(String messageId) {
        for (MessageModel m : messagesList) {
            if (m.getMessageId().equals(messageId)) {
                return true;
            }
        }
        return false;
    }

    private void removeMessageFromList(String messageId) {
        for (int i = 0; i < messagesList.size(); i++) {
            if (messagesList.get(i).getMessageId().equals(messageId)) {
                messagesList.remove(i);
                messagesAdapter.notifyItemRemoved(i);
                break;
            }
        }
    }
//    private void sendMessage(final String msg, final String msgType, String pushId) {
//        try {
//            if (TextUtils.isEmpty(msg)){
//                Toast.makeText(this, "Message cannot be empty", Toast.LENGTH_SHORT).show();
//                return;
//            }
//
//            HashMap<String, Object> messageMap = new HashMap<>();
//            messageMap.put(NodeNames.MESSAGE_ID, pushId);
//            messageMap.put(NodeNames.MESSAGE, msg);
//            messageMap.put(NodeNames.MESSAGE_TYPE, msgType);
//            messageMap.put(NodeNames.MESSAGE_FROM, currentUserId);
//            messageMap.put(NodeNames.MESSAGE_TIME, ServerValue.TIMESTAMP);  // Server will set the time
//
//            String currentUserRef = NodeNames.MESSAGES + "/" + currentUserId + "/" + chatUserId;
//            String chatUserRef = NodeNames.MESSAGES + "/" + chatUserId + "/" + currentUserId;
//
//            HashMap<String, Object> messageUserMap = new HashMap<>();
//            messageUserMap.put(currentUserRef + "/" + pushId, messageMap);
//            messageUserMap.put(chatUserRef + "/" + pushId, messageMap);
//
//            mRootRef.updateChildren(messageUserMap).addOnCompleteListener(task -> {
//                if (task.isSuccessful()) {
//                    etMessage.setText("");
//                    if (msgType.equals(Constants.MESSAGE_TYPE_TEXT)) {
//                        conversation.add(TextMessage.createForLocalUser(msg, System.currentTimeMillis()));
//                    }
//                } else {
//                    Toast.makeText(ChatActivity.this,
//                            "Failed to send message: " + task.getException(),
//                            Toast.LENGTH_SHORT).show();
//                }
//            });
//        } catch (Exception e) {
//            Toast.makeText(this, "Error sending message: " + e.getMessage(), Toast.LENGTH_SHORT).show();
//        }
//    }
//    private void sendMessage(final String msg, final String msgType, String pushId) {
//        try {
//            if (TextUtils.isEmpty(msg)){
//                Toast.makeText(this, "Message cannot be empty", Toast.LENGTH_SHORT).show();
//                return;
//            }
//
//            HashMap<String, Object> messageMap = new HashMap<>();
//            messageMap.put(NodeNames.MESSAGE_ID, pushId);
//            messageMap.put(NodeNames.MESSAGE, msg);
//            messageMap.put(NodeNames.MESSAGE_TYPE, msgType);
//            messageMap.put(NodeNames.MESSAGE_FROM, currentUserId);
//            messageMap.put(NodeNames.MESSAGE_TIME, ServerValue.TIMESTAMP);
//
//            String currentUserRef = NodeNames.MESSAGES + "/" + currentUserId + "/" + chatUserId;
//            String chatUserRef = NodeNames.MESSAGES + "/" + chatUserId + "/" + currentUserId;
//
//            HashMap<String, Object> messageUserMap = new HashMap<>();
//            messageUserMap.put(currentUserRef + "/" + pushId, messageMap);
//            messageUserMap.put(chatUserRef + "/" + pushId, messageMap);
//
//            mRootRef.updateChildren(messageUserMap).addOnCompleteListener(task -> {
//                if (task.isSuccessful()) {
//                    etMessage.setText("");
//                    if (msgType.equals(Constants.MESSAGE_TYPE_TEXT)) {
//                        conversation.add(TextMessage.createForLocalUser(msg, System.currentTimeMillis()));
//                    }
//                } else {
//                    Toast.makeText(ChatActivity.this,
//                            "Failed to send message: " + task.getException(),
//                            Toast.LENGTH_SHORT).show();
//                }
//            });
//        } catch (Exception e) {
//            Toast.makeText(this, "Error sending message: " + e.getMessage(), Toast.LENGTH_SHORT).show();
//        }
//    }

//    private void loadMessages() {
//        messagesList.clear();
//        conversation.clear();
//        cgSmartReplies.removeAllViews();
//
//        DatabaseReference databaseReferenceMessages = mRootRef.child(NodeNames.MESSAGES)
//                .child(currentUserId)
//                .child(chatUserId);
//
//        Query messageQuery = databaseReferenceMessages.limitToLast(currentPage * RECORD_PER_PAGE);
//
//        if (childEventListener != null) {
//            messageQuery.removeEventListener(childEventListener);
//        }
//
//        childEventListener = new ChildEventListener() {
//            @Override
//            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
//                try {
//                    MessageModel message = dataSnapshot.getValue(MessageModel.class);
//                    if (message != null) {
//                        // Check if this message already exists to prevent duplicates
//                        boolean exists = false;
//                        for (MessageModel m : messagesList) {
//                            if (m.getMessageId().equals(message.getMessageId())) {
//                                exists = true;
//                                break;
//                            }
//                        }
//
//                        if (!exists) {
//                            messagesList.add(message);
//                            messagesAdapter.notifyItemInserted(messagesList.size() - 1);
//                            rvMessages.scrollToPosition(messagesList.size() - 1);
//
//                            // Only show smart replies for text messages
//                            if (message.getMessageType().equals(Constants.MESSAGE_TYPE_TEXT)) {
//                                showSmartReplies(message);
//                            }
//                        }
//                    }
//                } catch (Exception e) {
//                    Log.e("ChatActivity", "Error parsing message: " + e.getMessage());
//                }
//                srlMessages.setRefreshing(false);
//            }
//
//            @Override
//            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {}
//
//            @Override
//            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
//                loadMessages(); // Refresh the list if a message is removed
//            }
//
//            @Override
//            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {}
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError databaseError) {
//                Log.e("ChatActivity", "Messages loading cancelled: " + databaseError.getMessage());
//                srlMessages.setRefreshing(false);
//            }
//        };
//
//        messageQuery.addChildEventListener(childEventListener);
//    }

    @Override
    public void onClick(View view) {
        if (view == null) {
            // Handle null view case
            return;
        }

        int viewId = view.getId();

        if (viewId == R.id.ivSend) {
            // Handle send button click
            if (Util.connectionAvailable(this)) {
                DatabaseReference userMessagePush = mRootRef.child(NodeNames.MESSAGES)
                        .child(currentUserId).child(chatUserId).push();
                String pushId = userMessagePush.getKey();
                String messageText = etMessage.getText().toString().trim();
                if (!TextUtils.isEmpty(messageText)) {
                    sendMessage(messageText, Constants.MESSAGE_TYPE_TEXT, pushId);
                } else {
                    Toast.makeText(this, "Message cannot be empty", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, R.string.no_internet, Toast.LENGTH_SHORT).show();
            }
        } else if (viewId == R.id.ivAttachment) {
            // Handle attachment button click
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                if (bottomSheetDialog != null) {
                    bottomSheetDialog.show();
                }
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            }

            // Hide the keyboard
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (inputMethodManager != null && view.getWindowToken() != null) {
                inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        } else if (viewId == R.id.llCamera) {
            // Handle camera option click
            if (bottomSheetDialog != null) {
                bottomSheetDialog.dismiss();
            }
            Intent intentCamera = new Intent(ACTION_IMAGE_CAPTURE);
            if (viewId == R.id.llCamera) {
                if (bottomSheetDialog != null) {
                    bottomSheetDialog.dismiss();
                }
//                Intent intentCamera = new Intent(ACTION_IMAGE_CAPTURE);
                if (intentCamera.resolveActivity(getPackageManager()) != null) {
                    captureImageLauncher.launch(intentCamera);
                } else {
                    Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show();
            }
        } else if (viewId == R.id.llGallery) {
            // Handle gallery option click
            if (bottomSheetDialog != null) {
                bottomSheetDialog.dismiss();
            }
            Intent intentImage = new Intent(Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            if (viewId == R.id.llGallery) {
                if (bottomSheetDialog != null) {
                    bottomSheetDialog.dismiss();
                }
//                Intent intentImage = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                if (intentImage.resolveActivity(getPackageManager()) != null) {
                    pickImageLauncher.launch(intentImage);
                } else {
                    Toast.makeText(this, "No gallery app found", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "No gallery app found", Toast.LENGTH_SHORT).show();
            }
        } else if (viewId == R.id.llVideo) {
            // Handle video option click
            if (bottomSheetDialog != null) {
                bottomSheetDialog.dismiss();
            }
            Intent intentVideo = new Intent(Intent.ACTION_PICK,
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
            if (viewId == R.id.llVideo) {
                if (bottomSheetDialog != null) {
                    bottomSheetDialog.dismiss();
                }
//                Intent intentVideo = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                if (intentVideo.resolveActivity(getPackageManager()) != null) {
                    pickVideoLauncher.launch(intentVideo);
                } else {
                    Toast.makeText(this, "No video app found", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "No video app found", Toast.LENGTH_SHORT).show();
            }
        } else if (viewId == R.id.ivClose) {
            // Handle close button click
            if (bottomSheetDialog != null) {
                bottomSheetDialog.dismiss();
            }
        } else {
            // Handle unknown view ID
            Toast.makeText(this, "Unknown view clicked", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CODE_CAPTURE_IMAGE) { // Camera
                Bitmap bitmap = (Bitmap) Objects.requireNonNull(Objects.requireNonNull(Objects.requireNonNull(data).getExtras())).get("data");
                encodeAndSendImage(bitmap, Constants.MESSAGE_TYPE_IMAGE);
            }
            else if (requestCode == REQUEST_CODE_PICK_IMAGE) { // Gallery
                Uri uri = Objects.requireNonNull(data).getData();
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                    encodeAndSendImage(bitmap, Constants.MESSAGE_TYPE_IMAGE);
                } catch (IOException e) {
                    Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                }
            }
            else if (requestCode == REQUEST_CODE_PICK_VIDEO) { // Video
                Uri uri = Objects.requireNonNull(data).getData();
                try {
                    byte[] videoBytes = readBytesFromUri(uri);
                    if (videoBytes != null) {
                        String videoBase64 = Base64.encodeToString(videoBytes, Base64.DEFAULT);
                        DatabaseReference messageRef = mRootRef.child(NodeNames.MESSAGES)
                                .child(currentUserId).child(chatUserId).push();
                        String pushId = messageRef.getKey();
                        sendMessage(videoBase64, Constants.MESSAGE_TYPE_VIDEO, pushId);
                    }
                } catch (IOException e) {
                    Toast.makeText(this, "Failed to load video", Toast.LENGTH_SHORT).show();
                }
            }
            else if (requestCode == REQUEST_CODE_FORWARD_MESSAGE) {
                Intent intent = new Intent(this, ChatActivity.class);
                intent.putExtra(Extras.USER_KEY, Objects.requireNonNull(data).getStringExtra(Extras.USER_KEY));
                intent.putExtra(Extras.USER_NAME, data.getStringExtra(Extras.USER_NAME));
                intent.putExtra(Extras.PHOTO_NAME, data.getStringExtra(Extras.PHOTO_NAME));
                intent.putExtra(Extras.MESSAGE, data.getStringExtra(Extras.MESSAGE));
                intent.putExtra(Extras.MESSAGE_ID, data.getStringExtra(Extras.MESSAGE_ID));
                intent.putExtra(Extras.MESSAGE_TYPE, data.getStringExtra(Extras.MESSAGE_TYPE));
                startActivity(intent);
                finish();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (bottomSheetDialog != null)
                    bottomSheetDialog.show();
            } else {
                Toast.makeText(this, "Permission required to access files", Toast.LENGTH_SHORT).show();
            }

        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed(); // Handle back button click
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void deleteMessage(final String messageId, final String messageType){

        DatabaseReference databaseReference = mRootRef.child(NodeNames.MESSAGES)
                .child(currentUserId).child(chatUserId).child(messageId);

        databaseReference.removeValue().addOnCompleteListener(task -> {

            if(task.isSuccessful())
            {
                DatabaseReference databaseReferenceChatUser = mRootRef.child(NodeNames.MESSAGES)
                        .child(chatUserId).child(currentUserId).child(messageId);

                databaseReferenceChatUser.removeValue().addOnCompleteListener(task1 -> {

                    if(task1.isSuccessful())
                    {
                        Toast.makeText(ChatActivity.this, R.string.message_deleted_successfully, Toast.LENGTH_SHORT).show();
                        if(!messageType.equals(Constants.MESSAGE_TYPE_TEXT))
                        {
                            StorageReference rootRef = FirebaseStorage.getInstance().getReference();
                            String folder = messageType.equals(Constants.MESSAGE_TYPE_VIDEO)?Constants.MESSAGE_VIDEOS:Constants.MESSAGE_IMAGES;
                            String fileName = messageType.equals(Constants.MESSAGE_TYPE_VIDEO)?messageId +".mp4": messageId+".jpg";
                            StorageReference fileRef = rootRef.child(folder).child(fileName);

                            fileRef.delete().addOnCompleteListener(task2 -> {
                                if (!task2.isSuccessful()) {
                                    Toast.makeText(ChatActivity.this,
                                            getString(R.string.failed_to_delete_file, task2.getException()), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                    else
                    {
                        Toast.makeText(ChatActivity.this, getString( R.string.failed_to_delete_message, task1.getException()),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
            else
            {
                Toast.makeText(ChatActivity.this, getString( R.string.failed_to_delete_message, task.getException()),
                        Toast.LENGTH_SHORT).show();
            }
        });




    }

    public void downloadFile(String messageId, final String messageType, final boolean isShare) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
        } else {
            DatabaseReference messageRef = mRootRef.child(NodeNames.MESSAGES)
                    .child(currentUserId).child(chatUserId).child(messageId);

            messageRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    MessageModel message = dataSnapshot.getValue(MessageModel.class);
                    if (message != null) {
                        String base64Data = message.getMessage();
                        byte[] fileBytes = Base64.decode(base64Data, Base64.DEFAULT);

                        String fileName = messageId +
                                (messageType.equals(Constants.MESSAGE_TYPE_VIDEO) ? ".mp4" : ".jpg");
                        String localFilePath = Objects.requireNonNull(getExternalFilesDir(null))
                                .getAbsolutePath() + "/" + fileName;

                        try {
                            FileOutputStream fos = new FileOutputStream(localFilePath);
                            fos.write(fileBytes);
                            fos.close();

                            if (isShare) {
                                shareFile(localFilePath, messageType);
                            } else {
                                viewFile(localFilePath, messageType);
                            }
                        } catch (IOException e) {
                            Toast.makeText(ChatActivity.this,
                                    "Failed to save file", Toast.LENGTH_SHORT).show();
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Toast.makeText(ChatActivity.this,
                            "Failed to load message", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void shareFile(String filePath, String messageType) {
        Intent intentShare = new Intent();
        intentShare.setAction(Intent.ACTION_SEND);
        intentShare.putExtra(Intent.EXTRA_STREAM, Uri.parse(filePath));
        intentShare.setType(messageType.equals(Constants.MESSAGE_TYPE_VIDEO) ?
                "video/mp4" : "image/jpg");
        startActivity(Intent.createChooser(intentShare, getString(R.string.share_with)));
    }

    private void viewFile(String filePath, String messageType) {
        Snackbar snackbar = Snackbar.make(llProgress,
                getString(R.string.file_downloaded_successfully),
                Snackbar.LENGTH_INDEFINITE);

        snackbar.setAction(R.string.view, view1 -> {
            Uri uri = Uri.parse(filePath);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.setDataAndType(uri,
                    messageType.equals(Constants.MESSAGE_TYPE_VIDEO) ?
                            "video/mp4" : "image/jpg");
            startActivity(intent);
        });

        snackbar.show();
    }

    public void forwardMessage(String selectedMessageId, String selectedMessage, String selectedMessageType) {
        Intent intent = new Intent(this, SelectFriendActivity.class);
        intent.putExtra(Extras.MESSAGE, selectedMessage);
        intent.putExtra(Extras.MESSAGE_ID, selectedMessageId);
        intent.putExtra(Extras.MESSAGE_TYPE, selectedMessageType);
        forwardMessageLauncher.launch(intent);
    }

    @Override
    public void onBackPressed() {
        mRootRef.child(NodeNames.CHATS).child(currentUserId).child(chatUserId).child(NodeNames.UNREAD_COUNT).setValue(0);
        super.onBackPressed();

    }

    private void showSmartReplies(final MessageModel messageModel){

        conversation.clear();
        cgSmartReplies.removeAllViews();

        DatabaseReference databaseReference = mRootRef.child(NodeNames.MESSAGES).child(currentUserId).child(chatUserId);
        Query lastQuery = databaseReference.orderByKey().limitToLast(1);

        lastQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot data : dataSnapshot.getChildren()){
                    MessageModel message = data.getValue(MessageModel.class);

                    assert message != null;
                    if(message.getMessageFrom().equals(chatUserId) && messageModel.getMessageId().equals(message.getMessageId())
                            && message.getMessageType().equals(Constants.MESSAGE_TYPE_TEXT)){
                        conversation.add(TextMessage.createForRemoteUser(message.getMessage(), System.currentTimeMillis(), chatUserId));

                        if(!conversation.isEmpty()){
                            // generating the smart replies using the smart reply generator
                            SmartReplyGenerator smartReply = SmartReply.getClient();
                            smartReply.suggestReplies(conversation)
                                    .addOnSuccessListener(result -> {
                                        if (result.getStatus() == SmartReplySuggestionResult.STATUS_NOT_SUPPORTED_LANGUAGE) {
                                            Toast.makeText(ChatActivity.this, "Language Not supported", Toast.LENGTH_SHORT).show();
                                        } else if (result.getStatus() == SmartReplySuggestionResult.STATUS_SUCCESS) {
                                            for (SmartReplySuggestion suggestion : result.getSuggestions()) {
                                                String replyText = suggestion.getText();

                                                Chip chip = new Chip(ChatActivity.this);
                                                ChipDrawable drawable = ChipDrawable.createFromAttributes(ChatActivity.this,
                                                        null, 0, R.style.Base_Theme_Duta);
// R.style.Widget_MaterialComponents_Chip_Action
                                                chip.setChipDrawable(drawable);
                                                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                                        LinearLayout.LayoutParams.WRAP_CONTENT,
                                                        LinearLayout.LayoutParams.WRAP_CONTENT);
                                                params.setMargins(16, 16, 16, 16);
                                                chip.setLayoutParams(params);
                                                chip.setText(replyText);
                                                chip.setTag(replyText);

                                                // apply onclick listener to the chip so that whenever user clicks on the CHIP, it will
                                                // send as the "Reply Message"
                                                chip.setOnClickListener(v -> {
                                                    DatabaseReference messageRef = mRootRef.child(NodeNames.MESSAGES)
                                                            .child(currentUserId).child(chatUserId).push();

                                                    String newMessageId = messageRef.getKey();
                                                    sendMessage(v.getTag().toString(), Constants.MESSAGE_TYPE_TEXT, newMessageId);
                                                });
                                                cgSmartReplies.addView(chip);
                                            }
                                        }
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(ChatActivity.this, "Something went wrong : " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


    }
}

//package com.pm.appdev.duta.chats;
//
//import android.Manifest;
//import android.annotation.SuppressLint;
//import android.content.Intent;
//import android.content.pm.PackageManager;
//import android.graphics.Bitmap;
//import android.graphics.BitmapFactory;
//import android.net.Uri;
//import android.os.Bundle;
//import android.provider.MediaStore;
//import android.text.Editable;
//import android.text.TextUtils;
//import android.text.TextWatcher;
//import android.util.Base64;
//import android.util.Log;
//import android.view.MenuItem;
//import android.view.View;
//import android.view.ViewGroup;
//import android.view.inputmethod.InputMethodManager;
//import android.widget.EditText;
//import android.widget.ImageView;
//import android.widget.LinearLayout;
//import android.widget.ProgressBar;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.appcompat.app.ActionBar;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.core.app.ActivityCompat;
//import androidx.recyclerview.widget.LinearLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
//
//import com.pm.appdev.duta.Common.Extras;
//import com.pm.appdev.duta.Common.NodeNames;
//import com.pm.appdev.duta.Common.Util;
//import com.pm.appdev.duta.Common.Constants;
//import com.pm.appdev.duta.R;
//import com.pm.appdev.duta.selectfriend.SelectFriendActivity;
//
//import com.bumptech.glide.Glide;
//import com.google.android.gms.tasks.OnCompleteListener;
//import com.google.android.gms.tasks.OnFailureListener;
//import com.google.android.gms.tasks.OnSuccessListener;
//import com.google.android.gms.tasks.Task;
//import com.google.android.material.bottomsheet.BottomSheetDialog;
//import com.google.android.material.chip.Chip;
//import com.google.android.material.chip.ChipDrawable;
//import com.google.android.material.chip.ChipGroup;
//import com.google.android.material.snackbar.Snackbar;
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.database.ChildEventListener;
//import com.google.firebase.database.DataSnapshot;
//import com.google.firebase.database.DatabaseError;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//import com.google.firebase.database.Query;
//import com.google.firebase.database.ServerValue;
//import com.google.firebase.database.ValueEventListener;
//import com.google.firebase.storage.FileDownloadTask;
//import com.google.firebase.storage.FirebaseStorage;
//import com.google.firebase.storage.OnProgressListener;
//import com.google.firebase.storage.StorageReference;
//import com.google.firebase.storage.UploadTask;
//import com.google.mlkit.nl.smartreply.SmartReply;
//import com.google.mlkit.nl.smartreply.SmartReplyGenerator;
//import com.google.mlkit.nl.smartreply.SmartReplySuggestion;
//import com.google.mlkit.nl.smartreply.SmartReplySuggestionResult;
//import com.google.mlkit.nl.smartreply.TextMessage;
//
//import java.io.ByteArrayOutputStream;
//import java.io.File;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//
////import static android.os.Build.VERSION_CODES.R;
//import static android.provider.MediaStore.ACTION_IMAGE_CAPTURE;
//
//public class ChatActivity extends AppCompatActivity implements View.OnClickListener {
//
//    private ImageView ivSend, ivAttachment, ivProfile;
//    private TextView tvUserName, tvUserStatus;
//
//    private EditText etMessage;
//    private DatabaseReference mRootRef;
//    private FirebaseAuth firebaseAuth;
//    private String currentUserId, chatUserId;
//
//    private RecyclerView rvMessages;
//    private SwipeRefreshLayout srlMessages;
//    private MessagesAdapter messagesAdapter;
//    private List<MessageModel> messagesList;
//
//    private int currentPage = 1;
//    private static final int RECORD_PER_PAGE = 30;
//
//    private static final int REQUEST_CODE_PICK_IMAGE = 101;
//    private static final int REQUEST_CODE_PICK_VIDEO = 103;
//    private static final int REQUEST_CODE_CAPTURE_IMAGE = 102;
//
//    private static final int REQUEST_CODE_FORWARD_MESSAGE = 104;
//
//
//    private ChildEventListener childEventListener;
//
//    private BottomSheetDialog bottomSheetDialog;
//
//    private LinearLayout llProgress;
//    private String userName, photoName;
//
//    private ChipGroup cgSmartReplies;
//
//
//    private List<TextMessage> conversation;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_chat);
//
//        // Check if User ID is null and close activity if it is
//        String userId = getIntent().getStringExtra(Extras.USER_KEY);
//        if (userId == null) {
//            Log.e("ChatActivity", "User ID is null, closing activity.");
//            finish(); // Close activity to prevent crash
//            return;
//        }
//
//        // Initialize UI components
//        initializeUI();
//
//        // Fetch user details from Firebase
//        fetchUserDetails(userId);
//    }
//
//    private void fetchUserDetails(String userId) {
//        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference()
//                .child(NodeNames.USERS).child(userId);
//
//        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                if (dataSnapshot.exists()) {
//                    userName = dataSnapshot.child(NodeNames.NAME).getValue(String.class);
//                    String photoBase64 = dataSnapshot.child(NodeNames.PHOTO).getValue(String.class);
//
//                    // Update UI with fetched data
//                    tvUserName.setText(userName);
//
//                    if (!TextUtils.isEmpty(photoBase64)) {
//                        try {
//                            byte[] decodedString = Base64.decode(photoBase64, Base64.DEFAULT);
//                            Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
//                            ivProfile.setImageBitmap(decodedBitmap);
//                        } catch (IllegalArgumentException e) {
//                            Log.e("ChatActivity", "Failed to decode Base64 photo: " + e.getMessage());
//                            ivProfile.setImageResource(R.drawable.default_profile);
//                        }
//                    } else {
//                        ivProfile.setImageResource(R.drawable.default_profile);
//                    }
//                } else {
//                    Log.e("ChatActivity", "User data not found in Firebase.");
//                    finish(); // Close activity if user data is not found
//                }
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError databaseError) {
//                Log.e("ChatActivity", "Failed to fetch user data: " + databaseError.getMessage());
//            }
//        });
//    }
//
//    private void initializeUI() {
//        // Initialize ActionBar
//        ActionBar actionBar = getSupportActionBar();
//        if (actionBar != null) {
//            actionBar.setTitle("");
//            ViewGroup actionBarLayout = (ViewGroup) getLayoutInflater().inflate(R.layout.custom_action_bar, null);
//
//            actionBar.setDisplayHomeAsUpEnabled(true);
//            actionBar.setHomeButtonEnabled(true);
//            actionBar.setElevation(0);
//
//            // Initialize views from the custom action bar layout
//            ivProfile = actionBarLayout.findViewById(R.id.ivProfile);
//            tvUserName = actionBarLayout.findViewById(R.id.tvUserName);
//            tvUserStatus = actionBarLayout.findViewById(R.id.tvUserStatus);
//
//            actionBar.setCustomView(actionBarLayout);
//            actionBar.setDisplayOptions(actionBar.getDisplayOptions() | ActionBar.DISPLAY_SHOW_CUSTOM);
//        }
//
//        // Initialize other UI components
//        cgSmartReplies = findViewById(R.id.cgSmartReplies);
//        conversation = new ArrayList<>();
//
//        ivSend = findViewById(R.id.ivSend);
//        ivAttachment = findViewById(R.id.ivAttachment);
//        etMessage = findViewById(R.id.etMessage);
//
//        llProgress = findViewById(R.id.llProgress);
//
//        ivSend.setOnClickListener(this);
//        ivAttachment.setOnClickListener(this);
//
//        firebaseAuth = FirebaseAuth.getInstance();
//        mRootRef = FirebaseDatabase.getInstance().getReference();
//        currentUserId = firebaseAuth.getCurrentUser().getUid();
//
//        // Initialize RecyclerView and SwipeRefreshLayout
//        rvMessages = findViewById(R.id.rvMessages);
//        srlMessages = findViewById(R.id.srlMessages);
//
//        // Initialize messagesList and messagesAdapter
//        messagesList = new ArrayList<>(); // Initialize messagesList here
//        messagesAdapter = new MessagesAdapter(this, messagesList); // Initialize messagesAdapter here
//
//        // Set up RecyclerView
//        rvMessages.setLayoutManager(new LinearLayoutManager(this));
//        rvMessages.setAdapter(messagesAdapter);
//
//        // Load messages and set up other Firebase listeners
//        loadMessages(); // Call the correct method
//
//        mRootRef.child(NodeNames.CHATS).child(currentUserId).child(chatUserId).child(NodeNames.UNREAD_COUNT).setValue(0);
//
//        rvMessages.scrollToPosition(messagesList.size() - 1);
//
//        srlMessages.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
//            @Override
//            public void onRefresh() {
//                currentPage++;
//                loadMessages(); // Call the correct method
//            }
//        });
//
//        bottomSheetDialog = new BottomSheetDialog(this);
//        View view = getLayoutInflater().inflate(R.layout.chat_file_options, null);
//        view.findViewById(R.id.llCamera).setOnClickListener(this);
//        view.findViewById(R.id.llGallery).setOnClickListener(this);
//        view.findViewById(R.id.llVideo).setOnClickListener(this);
//        view.findViewById(R.id.ivClose).setOnClickListener(this);
//        bottomSheetDialog.setContentView(view);
//
//        if (getIntent().hasExtra(Extras.MESSAGE) && getIntent().hasExtra(Extras.MESSAGE_ID) && getIntent().hasExtra(Extras.MESSAGE_TYPE)) {
//            String message = getIntent().getStringExtra(Extras.MESSAGE);
//            String messageId = getIntent().getStringExtra(Extras.MESSAGE_ID);
//            final String messageType = getIntent().getStringExtra(Extras.MESSAGE_TYPE);
//
//            DatabaseReference messageRef = mRootRef.child(NodeNames.MESSAGES).child(currentUserId).child(chatUserId).push();
//            final String newMessageId = messageRef.getKey();
//
//            if (messageType.equals(Constants.MESSAGE_TYPE_TEXT)) {
//                sendMessage(message, messageType, newMessageId);
//            } else {
//                StorageReference rootRef = FirebaseStorage.getInstance().getReference();
//                String folder = messageType.equals(Constants.MESSAGE_TYPE_VIDEO) ? Constants.MESSAGE_VIDEOS : Constants.MESSAGE_IMAGES;
//                String oldFileName = messageType.equals(Constants.MESSAGE_TYPE_VIDEO) ? messageId + ".mp4" : messageId + ".jpg";
//                String newFileName = messageType.equals(Constants.MESSAGE_TYPE_VIDEO) ? newMessageId + ".mp4" : newMessageId + ".jpg";
//
//                final String localFilePath = getExternalFilesDir(null).getAbsolutePath() + "/" + oldFileName;
//                final File localFile = new File(localFilePath);
//
//                final StorageReference newFileRef = rootRef.child(folder).child(newFileName);
//                rootRef.child(folder).child(oldFileName).getFile(localFile)
//                        .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
//                            @Override
//                            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
//                                UploadTask uploadTask = newFileRef.putFile(Uri.fromFile(localFile));
//                                uploadProgress(uploadTask, newFileRef, newMessageId, messageType);
//                            }
//                        });
//            }
//        }
//
//        DatabaseReference databaseReferenceUsers = mRootRef.child(NodeNames.USERS).child(chatUserId);
//        databaseReferenceUsers.addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                String status = "";
//                if (dataSnapshot.child(NodeNames.ONLINE).getValue() != null)
//                    status = dataSnapshot.child(NodeNames.ONLINE).getValue().toString();
//
//                if (status.equals("true"))
//                    tvUserStatus.setText(Constants.STATUS_ONLINE);
//                else
//                    tvUserStatus.setText(Constants.STATUS_OFFLINE);
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError databaseError) {
//                Log.e("ChatActivity", "Failed to fetch user status: " + databaseError.getMessage());
//            }
//        });
//
//        etMessage.addTextChangedListener(new TextWatcher() {
//            @Override
//            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//            }
//
//            @Override
//            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//            }
//
//            @Override
//            public void afterTextChanged(Editable editable) {
//                DatabaseReference currentUserRef = mRootRef.child(NodeNames.CHATS).child(currentUserId).child(chatUserId);
//                if (editable.toString().matches("")) {
//                    currentUserRef.child(NodeNames.TYPING).setValue(Constants.TYPING_STOPPED);
//                } else {
//                    currentUserRef.child(NodeNames.TYPING).setValue(Constants.TYPING_STARTED);
//                }
//            }
//        });
//
//        DatabaseReference chatUserRef = mRootRef.child(NodeNames.CHATS).child(chatUserId).child(currentUserId);
//        chatUserRef.addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                if (dataSnapshot.child(NodeNames.TYPING).getValue() != null) {
//                    String typingStatus = dataSnapshot.child(NodeNames.TYPING).getValue().toString();
//
//                    if (typingStatus.equals(Constants.TYPING_STARTED))
//                        tvUserStatus.setText(Constants.STATUS_TYPING);
//                    else
//                        tvUserStatus.setText(Constants.STATUS_ONLINE);
//                }
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError databaseError) {
//                Log.e("ChatActivity", "Failed to fetch typing status: " + databaseError.getMessage());
//            }
//        });
//    }
//
////    @Override
////    protected void onCreate(Bundle savedInstanceState) {
////        super.onCreate(savedInstanceState);
////        setContentView(R.layout.activity_chat);
////
////        // Check if User ID is null and close activity if it is
////        String userId = getIntent().getStringExtra(Extras.USER_KEY);
////        if (userId == null) {
////            Log.e("ChatActivity", "User ID is null, closing activity.");
////            finish(); // Close activity to prevent crash
////            return;
////        }
////
////        // Initialize UI components
////        initializeUI();
////
////        // Fetch user details from Firebase
////        fetchUserDetails(userId);
////    }
////
////    private void fetchUserDetails(String userId) {
////        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference()
////                .child(NodeNames.USERS).child(userId);
////
////        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
////            @Override
////            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
////                if (dataSnapshot.exists()) {
////                    userName = dataSnapshot.child(NodeNames.NAME).getValue(String.class);
////                    String photoBase64 = dataSnapshot.child(NodeNames.PHOTO).getValue(String.class);
////
////                    // Update UI with fetched data
////                    tvUserName.setText(userName);
////
////                    if (!TextUtils.isEmpty(photoBase64)) {
////                        try {
////                            byte[] decodedString = Base64.decode(photoBase64, Base64.DEFAULT);
////                            Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
////                            ivProfile.setImageBitmap(decodedBitmap);
////                        } catch (IllegalArgumentException e) {
////                            Log.e("ChatActivity", "Failed to decode Base64 photo: " + e.getMessage());
////                            ivProfile.setImageResource(R.drawable.default_profile);
////                        }
////                    } else {
////                        ivProfile.setImageResource(R.drawable.default_profile);
////                    }
////                } else {
////                    Log.e("ChatActivity", "User data not found in Firebase.");
////                    finish(); // Close activity if user data is not found
////                }
////            }
////
////            @Override
////            public void onCancelled(@NonNull DatabaseError databaseError) {
////                Log.e("ChatActivity", "Failed to fetch user data: " + databaseError.getMessage());
////            }
////        });
////    }
////    private void initializeUI() {
////        // Initialize ActionBar
////        ActionBar actionBar = getSupportActionBar();
////        if (actionBar != null) {
////            actionBar.setTitle("");
////            ViewGroup actionBarLayout = (ViewGroup) getLayoutInflater().inflate(R.layout.custom_action_bar, null);
////
////            actionBar.setDisplayHomeAsUpEnabled(true);
////            actionBar.setHomeButtonEnabled(true);
////            actionBar.setElevation(0);
////
////            // Initialize views from the custom action bar layout
////            ivProfile = actionBarLayout.findViewById(R.id.ivProfile);
////            tvUserName = actionBarLayout.findViewById(R.id.tvUserName);
////            tvUserStatus = actionBarLayout.findViewById(R.id.tvUserStatus);
////
////            actionBar.setCustomView(actionBarLayout);
////            actionBar.setDisplayOptions(actionBar.getDisplayOptions() | ActionBar.DISPLAY_SHOW_CUSTOM);
////        }
////
////        // Initialize other UI components
////        cgSmartReplies = findViewById(R.id.cgSmartReplies);
////        conversation = new ArrayList<>();
////
////        ivSend = findViewById(R.id.ivSend);
////        ivAttachment = findViewById(R.id.ivAttachment);
////        etMessage = findViewById(R.id.etMessage);
////
////        llProgress = findViewById(R.id.llProgress);
////
////        ivSend.setOnClickListener(this);
////        ivAttachment.setOnClickListener(this);
////
////        firebaseAuth = FirebaseAuth.getInstance();
////        mRootRef = FirebaseDatabase.getInstance().getReference();
////        currentUserId = firebaseAuth.getCurrentUser().getUid();
////
////        // Initialize RecyclerView and SwipeRefreshLayout
////        rvMessages = findViewById(R.id.rvMessages);
////        srlMessages = findViewById(R.id.srlMessages);
////
////        // Initialize messagesList and messagesAdapter
////        messagesList = new ArrayList<>(); // Initialize messagesList here
////        messagesAdapter = new MessagesAdapter(this, messagesList); // Initialize messagesAdapter here
////
////        // Set up RecyclerView
////        rvMessages.setLayoutManager(new LinearLayoutManager(this));
////        rvMessages.setAdapter(messagesAdapter);
////
////        // Load messages and set up other Firebase listeners
////        loadMessages();
////
////        mRootRef.child(NodeNames.CHATS).child(currentUserId).child(chatUserId).child(NodeNames.UNREAD_COUNT).setValue(0);
////
////        rvMessages.scrollToPosition(messagesList.size() - 1);
////
////        srlMessages.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
////            @Override
////            public void onRefresh() {
////                currentPage++;
////                loadMessages();
////            }
////        });
////
////        bottomSheetDialog = new BottomSheetDialog(this);
////        View view = getLayoutInflater().inflate(R.layout.chat_file_options, null);
////        view.findViewById(R.id.llCamera).setOnClickListener(this);
////        view.findViewById(R.id.llGallery).setOnClickListener(this);
////        view.findViewById(R.id.llVideo).setOnClickListener(this);
////        view.findViewById(R.id.ivClose).setOnClickListener(this);
////        bottomSheetDialog.setContentView(view);
////
////        if (getIntent().hasExtra(Extras.MESSAGE) && getIntent().hasExtra(Extras.MESSAGE_ID) && getIntent().hasExtra(Extras.MESSAGE_TYPE)) {
////            String message = getIntent().getStringExtra(Extras.MESSAGE);
////            String messageId = getIntent().getStringExtra(Extras.MESSAGE_ID);
////            final String messageType = getIntent().getStringExtra(Extras.MESSAGE_TYPE);
////
////            DatabaseReference messageRef = mRootRef.child(NodeNames.MESSAGES).child(currentUserId).child(chatUserId).push();
////            final String newMessageId = messageRef.getKey();
////
////            if (messageType.equals(Constants.MESSAGE_TYPE_TEXT)) {
////                sendMessage(message, messageType, newMessageId);
////            } else {
////                StorageReference rootRef = FirebaseStorage.getInstance().getReference();
////                String folder = messageType.equals(Constants.MESSAGE_TYPE_VIDEO) ? Constants.MESSAGE_VIDEOS : Constants.MESSAGE_IMAGES;
////                String oldFileName = messageType.equals(Constants.MESSAGE_TYPE_VIDEO) ? messageId + ".mp4" : messageId + ".jpg";
////                String newFileName = messageType.equals(Constants.MESSAGE_TYPE_VIDEO) ? newMessageId + ".mp4" : newMessageId + ".jpg";
////
////                final String localFilePath = getExternalFilesDir(null).getAbsolutePath() + "/" + oldFileName;
////                final File localFile = new File(localFilePath);
////
////                final StorageReference newFileRef = rootRef.child(folder).child(newFileName);
////                rootRef.child(folder).child(oldFileName).getFile(localFile)
////                        .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
////                            @Override
////                            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
////                                UploadTask uploadTask = newFileRef.putFile(Uri.fromFile(localFile));
////                                uploadProgress(uploadTask, newFileRef, newMessageId, messageType);
////                            }
////                        });
////            }
////        }
////
////        DatabaseReference databaseReferenceUsers = mRootRef.child(NodeNames.USERS).child(chatUserId);
////        databaseReferenceUsers.addValueEventListener(new ValueEventListener() {
////            @Override
////            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
////                String status = "";
////                if (dataSnapshot.child(NodeNames.ONLINE).getValue() != null)
////                    status = dataSnapshot.child(NodeNames.ONLINE).getValue().toString();
////
////                if (status.equals("true"))
////                    tvUserStatus.setText(Constants.STATUS_ONLINE);
////                else
////                    tvUserStatus.setText(Constants.STATUS_OFFLINE);
////            }
////
////            @Override
////            public void onCancelled(@NonNull DatabaseError databaseError) {
////                Log.e("ChatActivity", "Failed to fetch user status: " + databaseError.getMessage());
////            }
////        });
////
////        etMessage.addTextChangedListener(new TextWatcher() {
////            @Override
////            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
////            }
////
////            @Override
////            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
////            }
////
////            @Override
////            public void afterTextChanged(Editable editable) {
////                DatabaseReference currentUserRef = mRootRef.child(NodeNames.CHATS).child(currentUserId).child(chatUserId);
////                if (editable.toString().matches("")) {
////                    currentUserRef.child(NodeNames.TYPING).setValue(Constants.TYPING_STOPPED);
////                } else {
////                    currentUserRef.child(NodeNames.TYPING).setValue(Constants.TYPING_STARTED);
////                }
////            }
////        });
////
////        DatabaseReference chatUserRef = mRootRef.child(NodeNames.CHATS).child(chatUserId).child(currentUserId);
////        chatUserRef.addValueEventListener(new ValueEventListener() {
////            @Override
////            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
////                if (dataSnapshot.child(NodeNames.TYPING).getValue() != null) {
////                    String typingStatus = dataSnapshot.child(NodeNames.TYPING).getValue().toString();
////
////                    if (typingStatus.equals(Constants.TYPING_STARTED))
////                        tvUserStatus.setText(Constants.STATUS_TYPING);
////                    else
////                        tvUserStatus.setText(Constants.STATUS_ONLINE);
////                }
////            }
////
////            @Override
////            public void onCancelled(@NonNull DatabaseError databaseError) {
////                Log.e("ChatActivity", "Failed to fetch typing status: " + databaseError.getMessage());
////            }
////        });
////    }
////
////    private void loadMessages() {
////        if (messagesList == null) {
////            messagesList = new ArrayList<>(); // Ensure messagesList is initialized
////        }
////        messagesList.clear();
////
////        if (conversation == null) {
////            conversation = new ArrayList<>(); // Ensure conversation is initialized
////        }
////        conversation.clear();
////
////        if (cgSmartReplies != null) {
////            cgSmartReplies.removeAllViews();
////        }
////
////        DatabaseReference databaseReferenceMessages = mRootRef.child(NodeNames.MESSAGES).child(currentUserId).child(chatUserId);
////
////        Query messageQuery = databaseReferenceMessages.limitToLast(currentPage * RECORD_PER_PAGE);
////
////        if (childEventListener != null)
////            messageQuery.removeEventListener(childEventListener);
////
////        childEventListener = new ChildEventListener() {
////            @SuppressLint("NotifyDataSetChanged")
////            @Override
////            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
////                MessageModel message = dataSnapshot.getValue(MessageModel.class);
////
////                if (messagesList != null && messagesAdapter != null) {
////                    messagesList.add(message);
////                    messagesAdapter.notifyDataSetChanged();
////                    rvMessages.scrollToPosition(messagesList.size() - 1);
////                    srlMessages.setRefreshing(false);
////
////                    showSmartReplies(message);
////                }
////            }
////
////            @Override
////            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
////            }
////
////            @Override
////            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
////                loadMessages();
////            }
////
////            @Override
////            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
////            }
////
////            @Override
////            public void onCancelled(@NonNull DatabaseError databaseError) {
////                if (srlMessages != null) {
////                    srlMessages.setRefreshing(false);
////                }
////            }
////        };
////
////        messageQuery.addChildEventListener(childEventListener);
////    }
////    @Override
////    protected void onCreate(Bundle savedInstanceState) {
////        super.onCreate(savedInstanceState);
////        setContentView(R.layout.activity_chat);
////
////        ActionBar actionBar = getSupportActionBar();
////        if (actionBar != null) {
////            actionBar.setTitle("");
////            ViewGroup actionBarLayout = (ViewGroup) getLayoutInflater().inflate(R.layout.custom_action_bar, null);
////
////            actionBar.setDisplayHomeAsUpEnabled(true);
////            actionBar.setHomeButtonEnabled(true);
////            actionBar.setElevation(0);
////
////            // Initialize views from the custom action bar layout
////            ivProfile = actionBarLayout.findViewById(R.id.ivProfile);
////            tvUserName = actionBarLayout.findViewById(R.id.tvUserName);
////            tvUserStatus = actionBarLayout.findViewById(R.id.tvUserStatus);
////            if (ivProfile == null) {
////                Log.e("ChatActivity", "ivProfile is null");
////            }
////            if (tvUserName == null) {
////                Log.e("ChatActivity", "tvUserName is null");
////            }
////            if (tvUserStatus == null) {
////                Log.e("ChatActivity", "tvUserStatus is null");
////            }
////
////            actionBar.setCustomView(actionBarLayout);
////            actionBar.setDisplayOptions(actionBar.getDisplayOptions() | ActionBar.DISPLAY_SHOW_CUSTOM);
////        }
////
////        // initialization of variables inside onCreate Method
////        cgSmartReplies = findViewById(R.id.cgSmartReplies);
////        conversation = new ArrayList<>();
////
////        ivSend = findViewById(R.id.ivSend);
////        ivAttachment = findViewById(R.id.ivAttachment);
////        etMessage = findViewById(R.id.etMessage);
////
////        llProgress = findViewById(R.id.llProgress);
////
////        ivSend.setOnClickListener(this);
////        ivAttachment.setOnClickListener(this);
////
////        firebaseAuth = FirebaseAuth.getInstance();
////        mRootRef = FirebaseDatabase.getInstance().getReference();
////        currentUserId = firebaseAuth.getCurrentUser().getUid();
////
////        if (getIntent().hasExtra(Extras.USER_KEY)) {
////            chatUserId = getIntent().getStringExtra(Extras.USER_KEY);
////            photoName = chatUserId + ".jpg";
////        }
////        if (getIntent().hasExtra(Extras.USER_NAME))
////            userName = getIntent().getStringExtra(Extras.USER_NAME);
////
////        if (getIntent().hasExtra(Extras.PHOTO_NAME))
////            getIntent().getStringExtra(Extras.PHOTO_NAME);
////
////
////        tvUserName.setText(userName);
////
////
////        if(!TextUtils.isEmpty(photoName) && photoName!=null) {
////            StorageReference photoRef = FirebaseStorage.getInstance().getReference().child(Constants.IMAGES_FOLDER + "/" + photoName);
////
////            photoRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
////                @Override
////                public void onSuccess(Uri uri) {
////                    Glide.with(ChatActivity.this)
////                            .load(uri)
////                            .placeholder(R.drawable.default_profile)
////                            .error(R.drawable.default_profile)
////                            .into(ivProfile);
////                }
////            });
////        }
////
////
////        rvMessages = findViewById(R.id.rvMessages);
////        srlMessages = findViewById(R.id.srlMessages);
////
////        messagesList = new ArrayList<>();
////        messagesAdapter = new MessagesAdapter(this, messagesList);
////
////        rvMessages.setLayoutManager(new LinearLayoutManager(this));
////        rvMessages.setAdapter(messagesAdapter);
////
////        loadMessages();
////
////        mRootRef.child(NodeNames.CHATS).child(currentUserId).child(chatUserId).child(NodeNames.UNREAD_COUNT).setValue(0);
////
////        rvMessages.scrollToPosition(messagesList.size() - 1);
////
////        srlMessages.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
////            @Override
////            public void onRefresh() {
////                currentPage++;
////                loadMessages();
////            }
////        });
////
////        bottomSheetDialog = new BottomSheetDialog(this);
////        View view = getLayoutInflater().inflate(R.layout.chat_file_options, null);
////        view.findViewById(R.id.llCamera).setOnClickListener(this);
////        view.findViewById(R.id.llGallery).setOnClickListener(this);
////        view.findViewById(R.id.llVideo).setOnClickListener(this);
////        view.findViewById(R.id.ivClose).setOnClickListener(this);
////        bottomSheetDialog.setContentView(view);
////
////        if(getIntent().hasExtra(Extras.MESSAGE) && getIntent().hasExtra(Extras.MESSAGE_ID) && getIntent().hasExtra(Extras.MESSAGE_TYPE) )
////        {
////            String message = getIntent().getStringExtra(Extras.MESSAGE);
////            String messageId = getIntent().getStringExtra(Extras.MESSAGE_ID);
////            final String messageType = getIntent().getStringExtra(Extras.MESSAGE_TYPE);
////
////            DatabaseReference messageRef = mRootRef.child(NodeNames.MESSAGES).child(currentUserId).child(chatUserId).push();
////            final String newMessageId = messageRef.getKey();
////
////            if(messageType.equals(Constants.MESSAGE_TYPE_TEXT)) {
////                sendMessage(message, messageType, newMessageId);
////            }
////            else{
////                StorageReference rootRef = FirebaseStorage.getInstance().getReference();
////                String folder = messageType.equals( Constants.MESSAGE_TYPE_VIDEO)? Constants.MESSAGE_VIDEOS:Constants.MESSAGE_IMAGES;
////                String oldFileName = messageType.equals( Constants.MESSAGE_TYPE_VIDEO)?messageId + ".mp4": messageId+".jpg";
////                String newFileName = messageType.equals( Constants.MESSAGE_TYPE_VIDEO)?newMessageId + ".mp4": newMessageId+".jpg";
////
////                final String localFilePath = getExternalFilesDir(null).getAbsolutePath() + "/" + oldFileName;
////                final File localFile = new File(localFilePath);
////
////                final StorageReference newFileRef = rootRef.child(folder).child(newFileName);
////                        rootRef.child(folder).child(oldFileName).getFile(localFile)
////                                .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
////                            @Override
////                            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
////                                UploadTask uploadTask = newFileRef.putFile(Uri.fromFile(localFile));
////                                uploadProgress(uploadTask, newFileRef, newMessageId, messageType);
////                            }
////                        });
////                    }
////
////            }
////
////
////
////        DatabaseReference databaseReferenceUsers = mRootRef.child(NodeNames.USERS).child(chatUserId);
////        databaseReferenceUsers.addValueEventListener(new ValueEventListener() {
////            @Override
////            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
////
////                String status="";
////                if(dataSnapshot.child(NodeNames.ONLINE).getValue()!=null)
////                    status = dataSnapshot.child(NodeNames.ONLINE).getValue().toString();
////
////                if(status.equals("true"))
////                    tvUserStatus.setText(Constants.STATUS_ONLINE);
////                else
////                    tvUserStatus.setText(Constants.STATUS_OFFLINE);
////            }
////
////            @Override
////            public void onCancelled(@NonNull DatabaseError databaseError) {
////
////            }
////        });
////
////
////        etMessage.addTextChangedListener(new TextWatcher() {
////            @Override
////            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
////
////            }
////
////            @Override
////            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
////
////            }
////
////            @Override
////            public void afterTextChanged(Editable editable) {
////
////                DatabaseReference currentUserRef = mRootRef.child(NodeNames.CHATS).child(currentUserId).child(chatUserId);
////                if(editable.toString().matches(""))
////                {
////                    currentUserRef.child(NodeNames.TYPING).setValue(Constants.TYPING_STOPPED);
////                }
////                else
////                {
////                    currentUserRef.child(NodeNames.TYPING).setValue(Constants.TYPING_STARTED);
////                }
////
////            }
////        });
////
////
////        DatabaseReference chatUserRef = mRootRef.child(NodeNames.CHATS).child(chatUserId).child(currentUserId);
////        chatUserRef.addValueEventListener(new ValueEventListener() {
////            @Override
////            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
////                if(dataSnapshot.child(NodeNames.TYPING).getValue()!=null)
////                {
////                    String typingStatus = dataSnapshot.child(NodeNames.TYPING).getValue().toString();
////
////                    if(typingStatus.equals(Constants.TYPING_STARTED))
////                        tvUserStatus.setText(Constants.STATUS_TYPING);
////                    else
////                        tvUserStatus.setText(Constants.STATUS_ONLINE);
////                }
////            }
////
////            @Override
////            public void onCancelled(@NonNull DatabaseError databaseError) {
////
////            }
////        });
////
////
////        }
//
//    private void sendMessage(final String msg, final String msgType, String pushId) {
//        try {
//            if (!msg.equals("")) {
//
//                HashMap messageMap = new HashMap();
//                messageMap.put(NodeNames.MESSAGE_ID, pushId);
//                messageMap.put(NodeNames.MESSAGE, msg);
//                messageMap.put(NodeNames.MESSAGE_TYPE, msgType);
//                messageMap.put(NodeNames.MESSAGE_FROM, currentUserId);
//                messageMap.put(NodeNames.MESSAGE_TIME, ServerValue.TIMESTAMP);
//
//                String currentUserRef = NodeNames.MESSAGES + "/" + currentUserId + "/" + chatUserId;
//                String chatUserRef = NodeNames.MESSAGES + "/" + chatUserId + "/" + currentUserId;
//
//                HashMap messageUserMap = new HashMap();
//                messageUserMap.put(currentUserRef + "/" + pushId, messageMap);
//                messageUserMap.put(chatUserRef + "/" + pushId, messageMap);
//
//                etMessage.setText("");
//                if(msgType.equals(Constants.MESSAGE_TYPE_TEXT))
//                {
//                    conversation.add(TextMessage.createForLocalUser(msg, System.currentTimeMillis()));
//                }
//
//                mRootRef.updateChildren(messageUserMap, new DatabaseReference.CompletionListener() {
//                    @Override
//                    public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
//                        if (databaseError != null) {
//                            Toast.makeText(ChatActivity.this, getString(R.string.failed_to_send_message, databaseError.getMessage())
//                                    , Toast.LENGTH_SHORT).show();
//                        }
//                        {
//                            Toast.makeText(ChatActivity.this, R.string.message_sent_successfully, Toast.LENGTH_SHORT).show();
//                            String title="";
//
//                            if(msgType.equals(Constants.MESSAGE_TYPE_TEXT))
//                                title = "New Message";
//                            else if(msgType.equals(Constants.MESSAGE_TYPE_IMAGE))
//                                title = "New Image";
//                            else if(msgType.equals(Constants.MESSAGE_TYPE_VIDEO))
//                                title = "New Video";
//
//                            Util.sendNotification(ChatActivity.this, title, msg, chatUserId);
//
//                            String lastMessage= !title.equals("New Message")?title:msg;
//
//                            Util.updateChatDetails(ChatActivity.this, currentUserId, chatUserId, lastMessage);
//
//
//                        }
//                    }
//                });
//            }
//        } catch (Exception ex) {
//            Toast.makeText(ChatActivity.this, getString(R.string.failed_to_send_message, ex.getMessage())
//                    , Toast.LENGTH_SHORT).show();
//        }
//    }
//
//    private void loadMessages() {
//        messagesList.clear();
//
//        conversation.clear();
//        cgSmartReplies.removeAllViews();
//        DatabaseReference databaseReferenceMessages = mRootRef.child(NodeNames.MESSAGES).child(currentUserId).child(chatUserId);
//
//        Query messageQuery = databaseReferenceMessages.limitToLast(currentPage * RECORD_PER_PAGE);
//
//        if (childEventListener != null)
//            messageQuery.removeEventListener(childEventListener);
//
//        childEventListener = new ChildEventListener() {
//            @SuppressLint("NotifyDataSetChanged")
//            @Override
//            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
//                MessageModel message = dataSnapshot.getValue(MessageModel.class);
//
//                messagesList.add(message);
//                messagesAdapter.notifyDataSetChanged();
//                rvMessages.scrollToPosition(messagesList.size() - 1);
//                srlMessages.setRefreshing(false);
//
//                showSmartReplies(message);
//            }
//
//            @Override
//            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
//
//            }
//
//            @Override
//            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
//                    loadMessages();
//            }
//
//            @Override
//            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
//
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError databaseError) {
//                srlMessages.setRefreshing(false);
//            }
//        };
//
//        messageQuery.addChildEventListener(childEventListener);
//    }
//
//    @Override
//    public void onClick(View view) {
//        if (view == null) {
//            // Handle null view case
//            return;
//        }
//
//        int viewId = view.getId();
//
//        if (viewId == R.id.ivSend) {
//            // Handle send button click
//            if (Util.connectionAvailable(this)) {
//                DatabaseReference userMessagePush = mRootRef.child(NodeNames.MESSAGES)
//                        .child(currentUserId).child(chatUserId).push();
//                String pushId = userMessagePush.getKey();
//                String messageText = etMessage.getText().toString().trim();
//                if (!TextUtils.isEmpty(messageText)) {
//                    sendMessage(messageText, Constants.MESSAGE_TYPE_TEXT, pushId);
//                } else {
//                    Toast.makeText(this, "Message cannot be empty", Toast.LENGTH_SHORT).show();
//                }
//            } else {
//                Toast.makeText(this, R.string.no_internet, Toast.LENGTH_SHORT).show();
//            }
//        } else if (viewId == R.id.ivAttachment) {
//            // Handle attachment button click
//            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
//                    == PackageManager.PERMISSION_GRANTED) {
//                if (bottomSheetDialog != null) {
//                    bottomSheetDialog.show();
//                }
//            } else {
//                ActivityCompat.requestPermissions(this,
//                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
//            }
//
//            // Hide the keyboard
//            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
//            if (inputMethodManager != null && view.getWindowToken() != null) {
//                inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
//            }
//        } else if (viewId == R.id.llCamera) {
//            // Handle camera option click
//            if (bottomSheetDialog != null) {
//                bottomSheetDialog.dismiss();
//            }
//            Intent intentCamera = new Intent(ACTION_IMAGE_CAPTURE);
//            if (intentCamera.resolveActivity(getPackageManager()) != null) {
//                startActivityForResult(intentCamera, REQUEST_CODE_CAPTURE_IMAGE);
//            } else {
//                Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show();
//            }
//        } else if (viewId == R.id.llGallery) {
//            // Handle gallery option click
//            if (bottomSheetDialog != null) {
//                bottomSheetDialog.dismiss();
//            }
//            Intent intentImage = new Intent(Intent.ACTION_PICK,
//                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
//            if (intentImage.resolveActivity(getPackageManager()) != null) {
//                startActivityForResult(intentImage, REQUEST_CODE_PICK_IMAGE);
//            } else {
//                Toast.makeText(this, "No gallery app found", Toast.LENGTH_SHORT).show();
//            }
//        } else if (viewId == R.id.llVideo) {
//            // Handle video option click
//            if (bottomSheetDialog != null) {
//                bottomSheetDialog.dismiss();
//            }
//            Intent intentVideo = new Intent(Intent.ACTION_PICK,
//                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
//            if (intentVideo.resolveActivity(getPackageManager()) != null) {
//                startActivityForResult(intentVideo, REQUEST_CODE_PICK_VIDEO);
//            } else {
//                Toast.makeText(this, "No video app found", Toast.LENGTH_SHORT).show();
//            }
//        } else if (viewId == R.id.ivClose) {
//            // Handle close button click
//            if (bottomSheetDialog != null) {
//                bottomSheetDialog.dismiss();
//            }
//        } else {
//            // Handle unknown view ID
//            Toast.makeText(this, "Unknown view clicked", Toast.LENGTH_SHORT).show();
//        }
//    }
//
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//
//        if (resultCode == RESULT_OK) {
//            if (requestCode == REQUEST_CODE_CAPTURE_IMAGE)//Camera
//            {
//                Bitmap bitmap = (Bitmap) data.getExtras().get("data");
//
//                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
//                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
//
//                uploadBytes(bytes, Constants.MESSAGE_TYPE_IMAGE);
//
//            } else if (requestCode == REQUEST_CODE_PICK_IMAGE) { //Gallery
//                Uri uri = data.getData();
//                uploadFile(uri, Constants.MESSAGE_TYPE_IMAGE);
//            } else if (requestCode == REQUEST_CODE_PICK_VIDEO)//Video
//            {
//                Uri uri = data.getData();
//                uploadFile(uri, Constants.MESSAGE_TYPE_VIDEO);
//            }
//            else  if(requestCode==REQUEST_CODE_FORWARD_MESSAGE){
//
//                Intent intent = new Intent( this, ChatActivity.class);
//                intent.putExtra(Extras.USER_KEY, data.getStringExtra(Extras.USER_KEY));
//                intent.putExtra(Extras.USER_NAME, data.getStringExtra(Extras.USER_NAME));
//                intent.putExtra(Extras.PHOTO_NAME, data.getStringExtra(Extras.PHOTO_NAME));
//
//                intent.putExtra(Extras.MESSAGE, data.getStringExtra(Extras.MESSAGE));
//                intent.putExtra(Extras.MESSAGE_ID, data.getStringExtra(Extras.MESSAGE_ID));
//                intent.putExtra(Extras.MESSAGE_TYPE, data.getStringExtra(Extras.MESSAGE_TYPE));
//
//                startActivity(intent);
//                finish();
//            }
//        }
//
//    }
//
//    private void uploadFile(Uri uri, String messageType) {
//        DatabaseReference databaseReference = mRootRef.child(NodeNames.MESSAGES).child(currentUserId).child(chatUserId).push();
//        String pushId = databaseReference.getKey();
//
//        StorageReference storageReference = FirebaseStorage.getInstance().getReference();
//        String folderName = messageType.equals(Constants.MESSAGE_TYPE_VIDEO) ? Constants.MESSAGE_VIDEOS : Constants.MESSAGE_IMAGES;
//        String fileName = messageType.equals(Constants.MESSAGE_TYPE_VIDEO) ? pushId + ".mp4" : pushId + ".jpg";
//
//        StorageReference fileRef = storageReference.child(folderName).child(fileName);
//        UploadTask uploadTask = fileRef.putFile(uri);
//
//        uploadProgress(uploadTask, fileRef, pushId, messageType);
//
//
//    }
//
//    private void uploadBytes(ByteArrayOutputStream bytes, String messageType) {
//        DatabaseReference databaseReference = mRootRef.child(NodeNames.MESSAGES).child(currentUserId).child(chatUserId).push();
//        String pushId = databaseReference.getKey();
//
//        StorageReference storageReference = FirebaseStorage.getInstance().getReference();
//        String folderName = messageType.equals(Constants.MESSAGE_TYPE_VIDEO) ? Constants.MESSAGE_VIDEOS : Constants.MESSAGE_IMAGES;
//        String fileName = messageType.equals(Constants.MESSAGE_TYPE_VIDEO) ? pushId + ".mp4" : pushId + ".jpg";
//
//        StorageReference fileRef = storageReference.child(folderName).child(fileName);
//        UploadTask uploadTask = fileRef.putBytes(bytes.toByteArray());
//        uploadProgress(uploadTask, fileRef, pushId, messageType);
//    }
//
//
//    private void uploadProgress(final UploadTask task, final StorageReference filePath, final String pushId, final String messageType) {
//
//        final View view = getLayoutInflater().inflate(R.layout.file_progress, null);
//        final ProgressBar pbProgress = view.findViewById(R.id.pbProgress);
//        final TextView tvProgress = view.findViewById(R.id.tvFileProgress);
//        final ImageView ivPlay = view.findViewById(R.id.ivPlay);
//        final ImageView ivPause = view.findViewById(R.id.ivPause);
//        ImageView ivCancel = view.findViewById(R.id.ivCancel);
//
//        ivPause.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                task.pause();
//                ivPlay.setVisibility(View.VISIBLE);
//                ivPause.setVisibility(View.GONE);
//            }
//        });
//
//        ivPlay.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                task.resume();
//                ivPause.setVisibility(View.VISIBLE);
//                ivPlay.setVisibility(View.GONE);
//            }
//        });
//
//        ivCancel.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                task.cancel();
//            }
//        });
//
//        llProgress.addView(view);
//        tvProgress.setText(getString(R.string.upload_progress, messageType, "0"));
//
//        task.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
//            @Override
//            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
//                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
//
//                pbProgress.setProgress((int) progress);
//                tvProgress.setText(getString(R.string.upload_progress, messageType, String.valueOf(pbProgress.getProgress())));
//
//            }
//        });
//
//        task.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
//            @Override
//            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
//                llProgress.removeView(view);
//                if (task.isSuccessful()) {
//                    filePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
//                        @Override
//                        public void onSuccess(Uri uri) {
//                            String downloadUrl = uri.toString();
//                            sendMessage(downloadUrl, messageType, pushId);
//                        }
//                    });
//                }
//            }
//        });
//
//        task.addOnFailureListener(new OnFailureListener() {
//            @Override
//            public void onFailure(@NonNull Exception e) {
//                llProgress.removeView(view);
//                Toast.makeText(ChatActivity.this, getString(R.string.failed_to_upload, e.getMessage()), Toast.LENGTH_SHORT).show();
//            }
//        });
//
//    }
//
//
//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        if (requestCode == 1) {
//            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                if (bottomSheetDialog != null)
//                    bottomSheetDialog.show();
//            } else {
//                Toast.makeText(this, "Permission required to access files", Toast.LENGTH_SHORT).show();
//            }
//
//        }
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
//        int itemId = item.getItemId();
//        switch (itemId) {
//            case android.R.id.home:
//                finish();
//                break;
//
//            default:
//                break;
//
//        }
//        return super.onOptionsItemSelected(item);
//    }
//
//    public void deleteMessage(final String messageId, final String messageType){
//
//            DatabaseReference databaseReference = mRootRef.child(NodeNames.MESSAGES)
//                    .child(currentUserId).child(chatUserId).child(messageId);
//
//            databaseReference.removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
//                @Override
//                public void onComplete(@NonNull Task<Void> task) {
//
//                    if(task.isSuccessful())
//                    {
//                        DatabaseReference databaseReferenceChatUser = mRootRef.child(NodeNames.MESSAGES)
//                                .child(chatUserId).child(currentUserId).child(messageId);
//
//                        databaseReferenceChatUser.removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
//                            @Override
//                            public void onComplete(@NonNull Task<Void> task) {
//
//                                if(task.isSuccessful())
//                                {
//                                    Toast.makeText(ChatActivity.this, R.string.message_deleted_successfully, Toast.LENGTH_SHORT).show();
//                                    if(!messageType.equals(Constants.MESSAGE_TYPE_TEXT))
//                                    {
//                                        StorageReference rootRef = FirebaseStorage.getInstance().getReference();
//                                        String folder = messageType.equals(Constants.MESSAGE_TYPE_VIDEO)?Constants.MESSAGE_VIDEOS:Constants.MESSAGE_IMAGES;
//                                        String fileName = messageType.equals(Constants.MESSAGE_TYPE_VIDEO)?messageId +".mp4": messageId+".jpg";
//                                        StorageReference fileRef = rootRef.child(folder).child(fileName);
//
//                                        fileRef.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
//                                            @Override
//                                            public void onComplete(@NonNull Task<Void> task) {
//                                           if(!task.isSuccessful())
//                                           {
//                                               Toast.makeText(ChatActivity.this,
//                                                       getString(R.string.failed_to_delete_file, task.getException()), Toast.LENGTH_SHORT).show();
//                                           }
//                                            }
//                                        });
//                                    }
//                                }
//                                else
//                                {
//                                    Toast.makeText(ChatActivity.this, getString( R.string.failed_to_delete_message, task.getException()),
//                                            Toast.LENGTH_SHORT).show();
//                                }
//                            }
//                        });
//                    }
//                    else
//                    {
//                        Toast.makeText(ChatActivity.this, getString( R.string.failed_to_delete_message, task.getException()),
//                                Toast.LENGTH_SHORT).show();
//                    }
//                }
//            });
//
//
//
//
//    }
//
//    public  void  downloadFile(String messageId, final String messageType, final boolean isShare){
//        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)==PackageManager.PERMISSION_DENIED)
//        {
//            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
//        }
//        else
//        {
//                String folderName = messageType.equals(Constants.MESSAGE_TYPE_VIDEO)?Constants.MESSAGE_VIDEOS : Constants.MESSAGE_IMAGES;
//                String fileName = messageType.equals(Constants.MESSAGE_TYPE_VIDEO)?messageId + ".mp4": messageId + ".jpg";
//
//                StorageReference fileRef= FirebaseStorage.getInstance().getReference().child(folderName).child(fileName);
//                final String localFilePath = getExternalFilesDir(null).getAbsolutePath() + "/" + fileName;
//
//                File localFile = new File(localFilePath);
//
//                try {
//                    if(localFile.exists() || localFile.createNewFile())
//                    {
//                        final FileDownloadTask downloadTask =  fileRef.getFile(localFile);
//
//                        final View view = getLayoutInflater().inflate(R.layout.file_progress, null);
//                        final ProgressBar pbProgress = view.findViewById(R.id.pbProgress);
//                        final TextView tvProgress = view.findViewById(R.id.tvFileProgress);
//                        final ImageView ivPlay = view.findViewById(R.id.ivPlay);
//                        final ImageView ivPause = view.findViewById(R.id.ivPause);
//                        ImageView ivCancel = view.findViewById(R.id.ivCancel);
//
//                        ivPause.setOnClickListener(new View.OnClickListener() {
//                            @Override
//                            public void onClick(View view) {
//                                downloadTask.pause();
//                                ivPlay.setVisibility(View.VISIBLE);
//                                ivPause.setVisibility(View.GONE);
//                            }
//                        });
//
//                        ivPlay.setOnClickListener(new View.OnClickListener() {
//                            @Override
//                            public void onClick(View view) {
//                                downloadTask.resume();
//                                ivPause.setVisibility(View.VISIBLE);
//                                ivPlay.setVisibility(View.GONE);
//                            }
//                        });
//
//                        ivCancel.setOnClickListener(new View.OnClickListener() {
//                            @Override
//                            public void onClick(View view) {
//                                downloadTask.cancel();
//                            }
//                        });
//
//                        llProgress.addView(view);
//                        tvProgress.setText(getString(R.string.download_progress, messageType, "0"));
//
//                        downloadTask.addOnProgressListener(new OnProgressListener<FileDownloadTask.TaskSnapshot>() {
//                            @Override
//                            public void onProgress(FileDownloadTask.TaskSnapshot taskSnapshot) {
//                                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
//
//                                pbProgress.setProgress((int) progress);
//                                tvProgress.setText(getString(R.string.download_progress, messageType, String.valueOf(pbProgress.getProgress())));
//                            }
//                        });
//
//                        downloadTask.addOnCompleteListener(new OnCompleteListener<FileDownloadTask.TaskSnapshot>() {
//                            @Override
//                            public void onComplete(@NonNull Task<FileDownloadTask.TaskSnapshot> task) {
//                                llProgress.removeView(view);
//                                if (task.isSuccessful()) {
//
//                                    if(isShare){
//                                        Intent intentShare = new Intent();
//                                        intentShare.setAction(Intent.ACTION_SEND);
//                                        intentShare.putExtra(Intent.EXTRA_STREAM, Uri.parse(localFilePath));
//                                        if(messageType.equals(Constants.MESSAGE_TYPE_VIDEO))
//                                            intentShare.setType("video/mp4");
//                                        if(messageType.equals(Constants.MESSAGE_TYPE_IMAGE))
//                                            intentShare.setType("image/jpg");
//                                        startActivity(Intent.createChooser(intentShare, getString(R.string.share_with)));
//
//                                    }
//                                    else {
//                                        Snackbar snackbar = Snackbar.make(llProgress, getString(R.string.file_downloaded_successfully)
//                                                , Snackbar.LENGTH_INDEFINITE);
//
//                                        snackbar.setAction(R.string.view, new View.OnClickListener() {
//                                            @Override
//                                            public void onClick(View view) {
//                                                Uri uri = Uri.parse(localFilePath);
//                                                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
//                                                if (messageType.equals(Constants.MESSAGE_TYPE_VIDEO))
//                                                    intent.setDataAndType(uri, "video/mp4");
//                                                else if (messageType.equals(Constants.MESSAGE_TYPE_IMAGE))
//                                                    intent.setDataAndType(uri, "image/jpg");
//
//                                                startActivity(intent);
//                                            }
//                                        });
//
//
//                                        snackbar.show();
//                                    }
//
//                                }
//                            }
//                        });
//
//                        downloadTask.addOnFailureListener(new OnFailureListener() {
//                            @Override
//                            public void onFailure(@NonNull Exception e) {
//                                llProgress.removeView(view);
//                                Toast.makeText(ChatActivity.this, getString(R.string.failed_to_download, e.getMessage()), Toast.LENGTH_SHORT).show();
//                            }
//                        });
//
//                    }
//                    else
//                    {
//                        Toast.makeText(this, R.string.failed_to_store_file, Toast.LENGTH_SHORT).show();
//                    }
//                }
//                catch(Exception ex){
//                    Toast.makeText(ChatActivity.this, getString(R.string.failed_to_download, ex.getMessage()), Toast.LENGTH_SHORT).show();
//                }
//
//        }
//    }
//
//    public void forwardMessage(String selectedMessageId, String selectedMessage, String selectedMessageType) {
//        Intent intent = new Intent(this, SelectFriendActivity.class);
//        intent.putExtra(Extras.MESSAGE, selectedMessage);
//        intent.putExtra(Extras.MESSAGE_ID, selectedMessageId);
//        intent.putExtra(Extras.MESSAGE_TYPE, selectedMessageType);
//        startActivityForResult(intent , REQUEST_CODE_FORWARD_MESSAGE);
//    }
//
//    @Override
//    public void onBackPressed() {
//        mRootRef.child(NodeNames.CHATS).child(currentUserId).child(chatUserId).child(NodeNames.UNREAD_COUNT).setValue(0);
//        super.onBackPressed();
//
//    }
//
//
//    private void showSmartReplies(final MessageModel messageModel){
//
//        conversation.clear();
//        cgSmartReplies.removeAllViews();
//
//        DatabaseReference databaseReference = mRootRef.child(NodeNames.MESSAGES).child(currentUserId).child(chatUserId);
//        Query lastQuery = databaseReference.orderByKey().limitToLast(1);
//
//        lastQuery.addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                for(DataSnapshot data : dataSnapshot.getChildren()){
//                    MessageModel message = data.getValue(MessageModel.class);
//
//                    if(message.getMessageFrom().equals(chatUserId) && messageModel.getMessageId().equals(message.getMessageId())
//                            && message.getMessageType().equals(Constants.MESSAGE_TYPE_TEXT)){
//                        conversation.add(TextMessage.createForRemoteUser(message.getMessage(), System.currentTimeMillis(), chatUserId));
//
//                        if(!conversation.isEmpty()){
//                            // generating the smart replies using the smart reply generator
//                            SmartReplyGenerator smartReply = SmartReply.getClient();
//                            smartReply.suggestReplies(conversation)
//                                    .addOnSuccessListener(new OnSuccessListener<SmartReplySuggestionResult>() {
//                                        @Override
//                                        public void onSuccess(SmartReplySuggestionResult result) {
//                                            if(result.getStatus() == SmartReplySuggestionResult.STATUS_NOT_SUPPORTED_LANGUAGE){
//                                                Toast.makeText(ChatActivity.this, "Language Not supported", Toast.LENGTH_SHORT).show();
//                                            }
//                                            else if(result.getStatus() == SmartReplySuggestionResult.STATUS_SUCCESS){
//                                                for(SmartReplySuggestion suggestion : result.getSuggestions()){
//                                                    String replyText = suggestion.getText();
//
//                                                    Chip chip = new Chip(ChatActivity.this);
//                                                    ChipDrawable drawable = ChipDrawable.createFromAttributes(ChatActivity.this,
//                                                            null, 0, R.style.Base_Theme_Duta);
//// R.style.Widget_MaterialComponents_Chip_Action
//                                                    chip.setChipDrawable(drawable);
//                                                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
//                                                            LinearLayout.LayoutParams.WRAP_CONTENT,
//                                                            LinearLayout.LayoutParams.WRAP_CONTENT);
//                                                    params.setMargins(16,16,16,16);
//                                                    chip.setLayoutParams(params);
//                                                    chip.setText(replyText);
//                                                    chip.setTag(replyText);
//
//                                                    // apply onclick listener to the chip so that whenever user clicks on the CHIP, it will
//                                                    // send as the "Reply Message"
//                                                    chip.setOnClickListener(new View.OnClickListener() {
//                                                        @Override
//                                                        public void onClick(View v) {
//                                                            DatabaseReference messageRef = mRootRef.child(NodeNames.MESSAGES)
//                                                                    .child(currentUserId).child(chatUserId).push();
//
//                                                            String newMessageId = messageRef.getKey();
//                                                            sendMessage(v.getTag().toString(), Constants.MESSAGE_TYPE_TEXT, newMessageId);
//                                                        }
//                                                    });
//                                                    cgSmartReplies.addView(chip);
//                                                }
//                                            }
//                                        }
//                                    })
//                                    .addOnFailureListener(new OnFailureListener() {
//                                        @Override
//                                        public void onFailure(@NonNull Exception e) {
//                                            Toast.makeText(ChatActivity.this, "Something went wrong : " + e.getMessage(), Toast.LENGTH_SHORT).show();
//                                        }
//                                    });
//                        }
//                    }
//                }
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError databaseError) {
//
//            }
//        });
//
//
//    }
//}