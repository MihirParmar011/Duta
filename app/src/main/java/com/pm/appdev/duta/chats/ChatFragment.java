package com.pm.appdev.duta.chats;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.pm.appdev.duta.Common.NodeNames;
import com.pm.appdev.duta.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChatFragment extends Fragment {

    private static final String TAG = "ChatFragment";

    private RecyclerView rvChatList;
    private View progressBar;
    private TextView tvEmptyChatList;
    private TextView tvChatError;
    private EditText etChatSearch;

    private ChatListAdapter chatListAdapter;
    private final List<ChatListModel> allChats = new ArrayList<>();

    private DatabaseReference databaseReferenceChats;
    private DatabaseReference databaseReferenceUsers;
    private ValueEventListener chatsListener;
    private FirebaseUser currentUser;

    public ChatFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvChatList = view.findViewById(R.id.rvChats);
        tvEmptyChatList = view.findViewById(R.id.tvEmptyChatList);
        tvChatError = view.findViewById(R.id.tvChatError);
        progressBar = view.findViewById(R.id.progressBar);
        etChatSearch = view.findViewById(R.id.etChatSearch);

        chatListAdapter = new ChatListAdapter(requireContext(), new ArrayList<>());
        rvChatList.setLayoutManager(new LinearLayoutManager(getActivity()));
        rvChatList.setHasFixedSize(true);
        rvChatList.setAdapter(chatListAdapter);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            showErrorState(getString(R.string.error_authentication_required));
            return;
        }

        databaseReferenceUsers = FirebaseDatabase.getInstance().getReference(NodeNames.USERS);
        databaseReferenceChats = FirebaseDatabase.getInstance().getReference(NodeNames.CHATS)
                .child(currentUser.getUid());

        bindSearch();
        subscribeToChatUpdates();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (databaseReferenceChats != null && chatsListener != null) {
            databaseReferenceChats.removeEventListener(chatsListener);
        }
    }

    private void bindSearch() {
        etChatSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                applyFilter(editable == null ? "" : editable.toString());
            }
        });
    }

    private void subscribeToChatUpdates() {
        setLoading(true);
        chatsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot chatsSnapshot) {
                if (!isAdded()) {
                    return;
                }

                if (!chatsSnapshot.exists()) {
                    allChats.clear();
                    chatListAdapter.submitList(new ArrayList<>());
                    showEmptyState();
                    return;
                }

                Map<String, ChatListModel> intermediateChats = new HashMap<>();
                for (DataSnapshot chatSnapshot : chatsSnapshot.getChildren()) {
                    String userId = chatSnapshot.getKey();
                    if (userId == null) {
                        continue;
                    }

                    long lastMessageTime = getLong(chatSnapshot, NodeNames.TIME_STAMP, System.currentTimeMillis());
                    String unreadCount = String.valueOf(getLong(chatSnapshot, NodeNames.UNREAD_COUNT, 0));
                    String lastMessage = chatSnapshot.child(NodeNames.LAST_MESSAGE).getValue(String.class);
                    if (lastMessage == null || lastMessage.trim().isEmpty()) {
                        lastMessage = getString(R.string.default_last_message);
                    }

                    intermediateChats.put(userId,
                            new ChatListModel(userId, getString(R.string.loading_user), "", unreadCount,
                                    lastMessage, lastMessageTime));
                }

                hydrateUsersAndRender(intermediateChats);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Chat list listener cancelled", error.toException());
                showErrorState(getString(R.string.error_failed_load_chats));
            }
        };

        databaseReferenceChats.addValueEventListener(chatsListener);
    }

    private void hydrateUsersAndRender(Map<String, ChatListModel> intermediateChats) {
        databaseReferenceUsers.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot usersSnapshot) {
                for (DataSnapshot userNode : usersSnapshot.getChildren()) {
                    String uid = userNode.child("uid").getValue(String.class);
                    if (uid == null || !intermediateChats.containsKey(uid)) {
                        continue;
                    }

                    ChatListModel model = intermediateChats.get(uid);
                    if (model == null) {
                        continue;
                    }

                    model.setUserName(valueOrDefault(userNode.child(NodeNames.NAME).getValue(String.class),
                            getString(R.string.unknown_user)));
                    model.setPhotoName(valueOrDefault(userNode.child(NodeNames.PHOTO).getValue(String.class), ""));
                }

                List<ChatListModel> sortedChats = new ArrayList<>(intermediateChats.values());
                Collections.sort(sortedChats,
                        Comparator.comparingLong(ChatListModel::getLastMessageTime).reversed());

                allChats.clear();
                allChats.addAll(sortedChats);
                applyFilter(etChatSearch.getText() == null ? "" : etChatSearch.getText().toString());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to hydrate users", error.toException());
                showErrorState(getString(R.string.error_failed_load_users));
            }
        });
    }

    private void applyFilter(String query) {
        if (!isAdded()) {
            return;
        }

        String normalizedQuery = query.trim().toLowerCase(Locale.getDefault());
        List<ChatListModel> filteredItems = new ArrayList<>();

        for (ChatListModel model : allChats) {
            if (normalizedQuery.isEmpty()) {
                filteredItems.add(model);
                continue;
            }

            String username = model.getUserName() == null ? "" : model.getUserName().toLowerCase(Locale.getDefault());
            String lastMessage = model.getLastMessage() == null ? "" : model.getLastMessage().toLowerCase(Locale.getDefault());
            if (username.contains(normalizedQuery) || lastMessage.contains(normalizedQuery)) {
                filteredItems.add(model);
            }
        }

        chatListAdapter.submitList(filteredItems);

        boolean isEmpty = filteredItems.isEmpty();
        tvEmptyChatList.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        tvEmptyChatList.setText(normalizedQuery.isEmpty()
                ? getString(R.string.empty_chat_list)
                : getString(R.string.no_chat_search_results));

        tvChatError.setVisibility(View.GONE);
        setLoading(false);
    }

    private long getLong(DataSnapshot node, String key, long defaultValue) {
        Long value = node.child(key).getValue(Long.class);
        return value == null ? defaultValue : value;
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value == null ? defaultValue : value;
    }

    private void showEmptyState() {
        setLoading(false);
        chatListAdapter.submitList(new ArrayList<>());
        tvChatError.setVisibility(View.GONE);
        tvEmptyChatList.setVisibility(View.VISIBLE);
        tvEmptyChatList.setText(R.string.empty_chat_list);
    }

    private void showErrorState(String message) {
        setLoading(false);
        tvEmptyChatList.setVisibility(View.GONE);
        tvChatError.setVisibility(View.VISIBLE);
        tvChatError.setText(message);
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}
