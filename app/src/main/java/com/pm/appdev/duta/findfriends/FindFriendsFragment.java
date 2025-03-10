package com.pm.appdev.duta.findfriends;

import android.annotation.SuppressLint;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.pm.appdev.duta.Common.Constants;
import com.pm.appdev.duta.Common.NodeNames;
import com.pm.appdev.duta.R;
import com.pm.appdev.duta.findfriends.FindFriendModel;
import com.pm.appdev.duta.findfriends.FindFriendAdapter;

import java.util.ArrayList;
import java.util.List;

public class FindFriendsFragment extends Fragment {

    private RecyclerView rvFindFriends;
    private FindFriendAdapter findFriendAdapter;
    private List<FindFriendModel> findFriendModelList;
    private TextView tvEmptyFriendsList;
    private View progressBar;
    private SearchView searchView;
    private DatabaseReference usersRef, friendRequestsRef;
    private FirebaseUser currentUser;

    public FindFriendsFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_find_friends, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        searchView = view.findViewById(R.id.searchView);
        rvFindFriends = view.findViewById(R.id.rvFindFriends);
        progressBar = view.findViewById(R.id.progressBar);
        tvEmptyFriendsList = view.findViewById(R.id.tvEmptyFriendsList);

        rvFindFriends.setLayoutManager(new LinearLayoutManager(getActivity()));
        findFriendModelList = new ArrayList<>();
        findFriendAdapter = new FindFriendAdapter(getActivity(), findFriendModelList);
        rvFindFriends.setAdapter(findFriendAdapter);

        usersRef = FirebaseDatabase.getInstance().getReference().child(NodeNames.USERS);
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        friendRequestsRef = FirebaseDatabase.getInstance().getReference().child(NodeNames.FRIEND_REQUESTS);

        // 🔎 **Setup search bar listener**
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (!query.trim().isEmpty()) {
                    searchUserById(query.trim());
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (!newText.trim().isEmpty()) {
                    searchUserById(newText.trim());
                }
                return false;
            }
        });
    }

    private void searchUserById(String userId) {
        progressBar.setVisibility(View.VISIBLE);
        findFriendModelList.clear();

        DatabaseReference userRef = usersRef.child(userId);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                findFriendModelList.clear();
                if (!dataSnapshot.exists()) {
                    Toast.makeText(getContext(), "User not found", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    tvEmptyFriendsList.setVisibility(View.VISIBLE);
                    return;
                }

                String id = dataSnapshot.getKey();
                if (id.equals(currentUser.getUid())) return; // Skip the current user

                String fullName = dataSnapshot.child(NodeNames.NAME).getValue(String.class);
                String photoUrl = dataSnapshot.child(NodeNames.PHOTO).getValue(String.class);

                // ✅ Check if a friend request is sent
                friendRequestsRef.child(currentUser.getUid()).child(id).child(NodeNames.REQUEST_TYPE)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot requestSnapshot) {
                                boolean requestSent = requestSnapshot.exists() &&
                                        requestSnapshot.getValue(String.class).equals(Constants.REQUEST_STATUS_SENT);

                                findFriendModelList.add(new FindFriendModel(fullName, photoUrl, id, requestSent));
                                findFriendAdapter.notifyDataSetChanged();

                                progressBar.setVisibility(View.GONE);
                                tvEmptyFriendsList.setVisibility(findFriendModelList.isEmpty() ? View.VISIBLE : View.GONE);
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(getContext(), "Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Failed to search user: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
