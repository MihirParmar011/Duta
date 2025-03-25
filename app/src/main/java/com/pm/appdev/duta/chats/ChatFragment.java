package com.pm.appdev.duta.chats;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import com.pm.appdev.duta.R;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class ChatFragment extends Fragment {

    private RecyclerView rvChatList;
    private View progressBar;
    private TextView tvEmptyChatList;
    private ChatListAdapter chatListAdapter;
    private List<ChatListModel> chatListModelList;
    private DatabaseReference databaseReferenceChats, databaseReferenceUsers;
    private FirebaseUser currentUser;

    public ChatFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvChatList = view.findViewById(R.id.rvChats);
        tvEmptyChatList = view.findViewById(R.id.tvEmptyChatList);
        progressBar = view.findViewById(R.id.progressBar);

        chatListModelList = new ArrayList<>();
        chatListAdapter = new ChatListAdapter(getActivity(), chatListModelList);
        rvChatList.setLayoutManager(new LinearLayoutManager(getActivity()));
        rvChatList.setAdapter(chatListAdapter);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        databaseReferenceUsers = FirebaseDatabase.getInstance().getReference("Users");
        databaseReferenceChats = FirebaseDatabase.getInstance().getReference("Chats").child(currentUser.getUid());

        databaseReferenceChats.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                chatListModelList.clear();

                for (DataSnapshot chatSnapshot : snapshot.getChildren()) {
                    final String userId = chatSnapshot.getKey();  // ✅ Make it final
                    Long timestampValue = chatSnapshot.child("timestamp").getValue(Long.class);
                    final long timestamp = (timestampValue != null) ? timestampValue : System.currentTimeMillis();  // ✅ Prevent NPE

                    Log.d("ChatFragment", "Fetching user details for UID: " + userId);

                    databaseReferenceUsers.orderByChild("uid").equalTo(userId)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @SuppressLint("NotifyDataSetChanged")
                                @Override
                                public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                                    if (userSnapshot.exists()) {
                                        for (DataSnapshot data : userSnapshot.getChildren()) {
                                            String name = data.child("name").getValue(String.class);
                                            String photo = data.child("photo").getValue(String.class);

                                            Log.d("ChatFragment", "User Found: " + name);

                                            chatListModelList.add(new ChatListModel(
                                                    userId, name, photo, "0", "Last message", timestamp)); // ✅ Use correct `timestamp`
                                            chatListAdapter.notifyDataSetChanged();
                                        }
                                    } else {
                                        Log.e("ChatFragment", "User data not found for UID: " + userId);
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Log.e("ChatFragment", "Database Error: " + error.getMessage());
                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("ChatFragment", "Database Error: " + error.getMessage());
            }
        });

    }
}