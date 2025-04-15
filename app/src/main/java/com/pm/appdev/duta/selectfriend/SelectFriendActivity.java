package com.pm.appdev.duta.selectfriend;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.pm.appdev.duta.R;
import com.pm.appdev.duta.Common.Extras;
import com.pm.appdev.duta.Common.NodeNames;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class SelectFriendActivity extends AppCompatActivity {

    private RecyclerView rvSelectFriend;
    private SelectFriendAdapter selectFriendAdapter;
    private List<SelectFriendModel> selectFriendModels;
    private View progressBar;

    private DatabaseReference databaseReferenceUsers, databaseReferenceChats;
    private FirebaseUser currentUser;
    private ValueEventListener valueEventListener;
    private String selectedMessage, selectedMessageId, selectedMessageType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_friend);

        if(getIntent().hasExtra(Extras.MESSAGE)) {
            selectedMessage = getIntent().getStringExtra(Extras.MESSAGE);
            selectedMessageId = getIntent().getStringExtra(Extras.MESSAGE_ID);
            selectedMessageType = getIntent().getStringExtra(Extras.MESSAGE_TYPE);
        }

        rvSelectFriend = findViewById(R.id.rvSelectFriend);
        progressBar = findViewById(R.id.progressBar);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        rvSelectFriend.setLayoutManager(linearLayoutManager);

        selectFriendModels = new ArrayList<>();
        selectFriendAdapter = new SelectFriendAdapter(this, selectFriendModels);
        rvSelectFriend.setAdapter(selectFriendAdapter);

        progressBar.setVisibility(View.VISIBLE);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        databaseReferenceChats = FirebaseDatabase.getInstance().getReference().child(NodeNames.CHATS).child(currentUser.getUid());
        databaseReferenceUsers = FirebaseDatabase.getInstance().getReference().child(NodeNames.USERS);

        valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                selectFriendModels.clear();
                Set<String> processedUserIds = new HashSet<>();

                for (DataSnapshot chatSnapshot : dataSnapshot.getChildren()) {
                    // Check if this is a direct UID node
                    if (isValidUid(chatSnapshot.getKey())) {
                        processChatUid(chatSnapshot.getKey(), processedUserIds);
                    }
                    // Check for UIDs in chat data
                    else {
                        for (DataSnapshot chatData : chatSnapshot.getChildren()) {
                            if (isValidUid(chatData.getKey())) {
                                processChatUid(chatData.getKey(), processedUserIds);
                            }
                        }
                    }
                }

                if (processedUserIds.isEmpty()) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(SelectFriendActivity.this,
                            "No friends found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(SelectFriendActivity.this,
                        getString(R.string.failed_to_fetch_friend_list, databaseError.getMessage()),
                        Toast.LENGTH_SHORT).show();
            }
        };
        databaseReferenceChats.addValueEventListener(valueEventListener);
    }

    private boolean isValidUid(String uid) {
        return uid != null &&
                !uid.equals(currentUser.getUid()) &&
                !uid.isEmpty() &&
                !uid.equals("unread_count") &&
                !uid.equals("last_message") &&
                !uid.equals("last_message_time") &&
                !uid.equals("timestamp") &&
                !uid.equals("typing");
    }

    private void processChatUid(String uid, Set<String> processedUserIds) {
        if (!processedUserIds.contains(uid)) {
            processedUserIds.add(uid);
            fetchUserDetails(uid);
        }
    }

    private void fetchUserDetails(String targetUid) {
        if (targetUid == null) {
            Log.e("SelectFriendActivity", "UID is null");
            progressBar.setVisibility(View.GONE);
            return;
        }

        // Query users where "uid" field matches targetUid
        Query query = databaseReferenceUsers.orderByChild("uid").equalTo(targetUid);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                        String userName = userSnapshot.child(NodeNames.NAME).getValue(String.class);
                        String photoName = userSnapshot.child(NodeNames.PHOTO).getValue(String.class);
                        String userUid = userSnapshot.child("uid").getValue(String.class);

                        // Create friend model with the retrieved data
                        SelectFriendModel friendModel = new SelectFriendModel(
                                userUid != null ? userUid : targetUid,
                                userName != null ? userName : "Unknown",
                                photoName != null ? photoName : targetUid + ".jpg"
                        );

                        selectFriendModels.add(friendModel);
                        selectFriendAdapter.notifyDataSetChanged();
                        break; // Exit after first match (UIDs should be unique)
                    }
                } else {
                    Log.e("SelectFriendActivity", "User with UID not found: " + targetUid);
                    // Add with default values if user not found
                    SelectFriendModel friendModel = new SelectFriendModel(
                            targetUid,
                            "Unknown",
                            targetUid + ".jpg"
                    );
                    selectFriendModels.add(friendModel);
                    selectFriendAdapter.notifyDataSetChanged();
                }
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("SelectFriendActivity", "Query failed: " + databaseError.getMessage());
                progressBar.setVisibility(View.GONE);
                // Add with default values on error
                SelectFriendModel friendModel = new SelectFriendModel(
                        targetUid,
                        "Unknown",
                        targetUid + ".jpg"
                );
                selectFriendModels.add(friendModel);
                selectFriendAdapter.notifyDataSetChanged();
            }
        });
    }

    public void returnSelectedFriend(String userId, String userName, String photoName) {
        if (valueEventListener != null) {
            databaseReferenceChats.removeEventListener(valueEventListener);
        }
        Intent intent = new Intent();

        intent.putExtra(Extras.USER_KEY, userId);
        intent.putExtra(Extras.USER_NAME, userName);
        intent.putExtra(Extras.PHOTO_NAME, photoName);

        if (selectedMessage != null) {
            intent.putExtra(Extras.MESSAGE, selectedMessage);
            intent.putExtra(Extras.MESSAGE_ID, selectedMessageId);
            intent.putExtra(Extras.MESSAGE_TYPE, selectedMessageType);
        }

        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (valueEventListener != null) {
            databaseReferenceChats.removeEventListener(valueEventListener);
        }
    }
}

//package com.pm.appdev.duta.selectfriend;
//
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.recyclerview.widget.LinearLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//
//import android.annotation.SuppressLint;
//import android.app.Activity;
//import android.content.Intent;
//import android.os.Bundle;
//import android.view.View;
//import android.widget.Toast;
//
//import com.pm.appdev.duta.R;
//import com.pm.appdev.duta.Common.Extras;
//import com.pm.appdev.duta.Common.NodeNames;
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.auth.FirebaseUser;
//import com.google.firebase.database.DataSnapshot;
//import com.google.firebase.database.DatabaseError;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//import com.google.firebase.database.ValueEventListener;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Objects;
//
//public class SelectFriendActivity extends AppCompatActivity {
//
//    private RecyclerView rvSelectFriend;
//    private  SelectFriendAdapter selectFriendAdapter;
//    private List<SelectFriendModel> selectFriendModels;
//    private View progressBar;
//
//    private DatabaseReference databaseReferenceUsers, databaseReferenceChats;
//
//    private FirebaseUser currentUser;
//    private ValueEventListener valueEventListener;
//
//    private  String selectedMessage, selectedMessageId, selectedMessageType;
//
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_select_friend);
//
//        if(getIntent().hasExtra(Extras.MESSAGE))
//        {
//            selectedMessage = getIntent().getStringExtra(Extras.MESSAGE);
//            selectedMessageId = getIntent().getStringExtra(Extras.MESSAGE_ID);
//            selectedMessageType = getIntent().getStringExtra(Extras.MESSAGE_TYPE);
//        }
//
//        rvSelectFriend = findViewById(R.id.rvSelectFriend);
//        progressBar = findViewById(R.id.progressBar);
//
//        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
//        rvSelectFriend.setLayoutManager(linearLayoutManager);
//
//        selectFriendModels = new ArrayList<>();
//        selectFriendAdapter = new SelectFriendAdapter(this, selectFriendModels);
//        rvSelectFriend.setAdapter(selectFriendAdapter);
//
//        progressBar.setVisibility(View.VISIBLE);
//
//        currentUser = FirebaseAuth.getInstance().getCurrentUser();
//        databaseReferenceChats = FirebaseDatabase.getInstance().getReference().child(NodeNames.CHATS).child(currentUser.getUid());
//        databaseReferenceUsers = FirebaseDatabase.getInstance().getReference().child(NodeNames.USERS);
//
//        valueEventListener = new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                for (DataSnapshot ds : dataSnapshot.getChildren())
//                {
//                    final String userId = ds.getKey();
//                    databaseReferenceUsers.child(Objects.requireNonNull(userId)).addListenerForSingleValueEvent(new ValueEventListener() {
//                        @SuppressLint("NotifyDataSetChanged")
//                        @Override
//                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                            String userName = dataSnapshot.child(NodeNames.NAME).getValue()!=null?
//                                    Objects.requireNonNull(dataSnapshot.child(NodeNames.NAME).getValue()).toString():"";
//
//                            SelectFriendModel friendModel = new SelectFriendModel(userId, userName, userId + ".jpg");
//                            selectFriendModels.add(friendModel);
//                            selectFriendAdapter.notifyDataSetChanged();
//
//                            progressBar.setVisibility(View.GONE);
//                        }
//
//                        @Override
//                        public void onCancelled(@NonNull DatabaseError databaseError) {
//                            Toast.makeText(SelectFriendActivity.this,
//                                    getString(R.string.failed_to_fetch_friend_list, databaseError.getMessage()), Toast.LENGTH_SHORT).show();
//                        }
//                    });
//
//                }
//            }
//            @Override
//            public void onCancelled(@NonNull DatabaseError databaseError) {
//                Toast.makeText(SelectFriendActivity.this,
//                        getString(R.string.failed_to_fetch_friend_list, databaseError.getMessage()), Toast.LENGTH_SHORT).show();
//            }
//        };
//        databaseReferenceChats.addValueEventListener(valueEventListener);
//    }
//
//    public  void  returnSelectedFriend(String userId, String userName, String photoName)
//    {
//        databaseReferenceChats.removeEventListener(valueEventListener);
//        Intent intent = new Intent();
//
//        intent.putExtra(Extras.USER_KEY, userId);
//        intent.putExtra(Extras.USER_NAME, userName);
//        intent.putExtra(Extras.PHOTO_NAME, photoName);
//
//        intent.putExtra(Extras.MESSAGE, selectedMessage);
//        intent.putExtra(Extras.MESSAGE_ID, selectedMessageId);
//        intent.putExtra(Extras.MESSAGE_TYPE, selectedMessageType);
//
//        setResult(Activity.RESULT_OK, intent);
//        finish();
//    }
//}
