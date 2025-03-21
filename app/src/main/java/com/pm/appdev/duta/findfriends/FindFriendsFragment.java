package com.pm.appdev.duta.findfriends;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

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
import com.pm.appdev.duta.R;

import java.util.ArrayList;
import java.util.List;

public class FindFriendsFragment extends Fragment {

    private RecyclerView rvFindFriends;
    private FindFriendAdapter findFriendAdapter;
    private List<FindFriendModel> findFriendModelList;
    private TextView tvEmptyFriendsList;
    private SearchView searchView;
    private View progressBar;

    private DatabaseReference usersDatabase, friendRequestDatabase;
    private FirebaseUser currentUser;

    public FindFriendsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_find_friends, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvFindFriends = view.findViewById(R.id.rvFindFriends);
        searchView = view.findViewById(R.id.searchView);
        progressBar = view.findViewById(R.id.progressBar);
        tvEmptyFriendsList = view.findViewById(R.id.tvEmptyFriendsList);

        rvFindFriends.setLayoutManager(new LinearLayoutManager(getActivity()));

        findFriendModelList = new ArrayList<>();
        usersDatabase = FirebaseDatabase.getInstance().getReference("Users"); // Initialize usersDatabase
        findFriendAdapter = new FindFriendAdapter(getActivity(), findFriendModelList, usersDatabase); // Pass usersDatabase
        rvFindFriends.setAdapter(findFriendAdapter);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        friendRequestDatabase = FirebaseDatabase.getInstance().getReference("FriendRequests").child(currentUser.getUid());

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (!TextUtils.isEmpty(query)) {
                    searchUser(query.trim());
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (TextUtils.isEmpty(newText)) {
                    findFriendModelList.clear();
                    findFriendAdapter.notifyDataSetChanged();
                    tvEmptyFriendsList.setVisibility(View.VISIBLE);
                }
                return false;
            }
        });
    }

    private void searchUser(String userId) {
        progressBar.setVisibility(View.VISIBLE);
        tvEmptyFriendsList.setVisibility(View.GONE);

        // Search for the user by userId
        usersDatabase.orderByKey().equalTo(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                findFriendModelList.clear();

                if (dataSnapshot.exists()) {
                    for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                        String fullName = userSnapshot.child("name").getValue(String.class);
                        String photoUrl = userSnapshot.child("photo").getValue(String.class);
                        String receiverUid = userSnapshot.child("uid").getValue(String.class);

                        // Check if a friend request already exists
                        friendRequestDatabase.orderByChild("receiverId").equalTo(receiverUid)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot requestSnapshot) {
                                        boolean requestSent = false;
                                        for (DataSnapshot snapshot : requestSnapshot.getChildren()) {
                                            String senderId = snapshot.child("senderId").getValue(String.class);
                                            if (senderId != null && senderId.equals(currentUser.getUid())) {
                                                requestSent = true;
                                                break;
                                            }
                                        }

                                        findFriendModelList.add(new FindFriendModel(fullName, photoUrl, userId, requestSent));
                                        findFriendAdapter.notifyDataSetChanged();
                                        progressBar.setVisibility(View.GONE);
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {
                                        progressBar.setVisibility(View.GONE);
                                    }
                                });
                    }
                } else {
                    progressBar.setVisibility(View.GONE);
                    tvEmptyFriendsList.setVisibility(View.VISIBLE);
                    Toast.makeText(getContext(), "User not found!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}

//package com.pm.appdev.duta.findfriends;
//
//import android.os.Bundle;
//import android.text.TextUtils;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.SearchView;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.fragment.app.Fragment;
//import androidx.recyclerview.widget.LinearLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.auth.FirebaseUser;
//import com.google.firebase.database.DataSnapshot;
//import com.google.firebase.database.DatabaseError;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//import com.google.firebase.database.ValueEventListener;
//import com.pm.appdev.duta.R;
//
//import java.util.List;
//
//import java.util.ArrayList;
//import java.util.List;
//
//public class FindFriendsFragment extends Fragment {
//
//    private RecyclerView rvFindFriends;
//    private FindFriendAdapter findFriendAdapter;
//    private List<FindFriendModel> findFriendModelList;
//    private TextView tvEmptyFriendsList;
//    private SearchView searchView;
//    private View progressBar;
//
//    private DatabaseReference usersDatabase, friendRequestDatabase;
//    private FirebaseUser currentUser;
//
//    public FindFriendsFragment() {
//        // Required empty public constructor
//    }
//
//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//        return inflater.inflate(R.layout.fragment_find_friends, container, false);
//    }
//
//    @Override
//    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
//        super.onViewCreated(view, savedInstanceState);
//
//        rvFindFriends = view.findViewById(R.id.rvFindFriends);
//        searchView = view.findViewById(R.id.searchView);
//        progressBar = view.findViewById(R.id.progressBar);
//        tvEmptyFriendsList = view.findViewById(R.id.tvEmptyFriendsList);
//
//        rvFindFriends.setLayoutManager(new LinearLayoutManager(getActivity()));
//
//        findFriendModelList = new ArrayList<>();
//        findFriendAdapter = new FindFriendAdapter(getActivity(), findFriendModelList);
//        rvFindFriends.setAdapter(findFriendAdapter);
//
//        usersDatabase = FirebaseDatabase.getInstance().getReference("Users");
//        currentUser = FirebaseAuth.getInstance().getCurrentUser();
//        friendRequestDatabase = FirebaseDatabase.getInstance().getReference("FriendRequests").child(currentUser.getUid());
//
//        DatabaseReference usersDatabase = FirebaseDatabase.getInstance().getReference("Users");
//        findFriendAdapter = new FindFriendAdapter(getActivity(), findFriendModelList, usersDatabase);
//
//        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
//            @Override
//            public boolean onQueryTextSubmit(String query) {
//                if (!TextUtils.isEmpty(query)) {
//                    searchUser(query.trim());
//                }
//                return false;
//            }
//
//            @Override
//            public boolean onQueryTextChange(String newText) {
//                if (TextUtils.isEmpty(newText)) {
//                    findFriendModelList.clear();
//                    findFriendAdapter.notifyDataSetChanged();
//                    tvEmptyFriendsList.setVisibility(View.VISIBLE);
//                }
//                return false;
//            }
//        });
//    }
//
//    private void searchUser(String userId) {
//        progressBar.setVisibility(View.VISIBLE);
//        tvEmptyFriendsList.setVisibility(View.GONE);
//
//        // Search for the user by userId
//        usersDatabase.orderByKey().equalTo(userId).addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                findFriendModelList.clear();
//
//                if (dataSnapshot.exists()) {
//                    for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
//                        String fullName = userSnapshot.child("name").getValue(String.class);
//                        String photoUrl = userSnapshot.child("photo").getValue(String.class);
//                        String receiverUid = userSnapshot.child("uid").getValue(String.class);
//
//                        // Check if a friend request already exists
//                        friendRequestDatabase.orderByChild("receiverId").equalTo(receiverUid)
//                                .addListenerForSingleValueEvent(new ValueEventListener() {
//                                    @Override
//                                    public void onDataChange(@NonNull DataSnapshot requestSnapshot) {
//                                        boolean requestSent = false;
//                                        for (DataSnapshot snapshot : requestSnapshot.getChildren()) {
//                                            String senderId = snapshot.child("senderId").getValue(String.class);
//                                            if (senderId != null && senderId.equals(currentUser.getUid())) {
//                                                requestSent = true;
//                                                break;
//                                            }
//                                        }
//
//                                        findFriendModelList.add(new FindFriendModel(fullName, photoUrl, userId, requestSent));
//                                        findFriendAdapter.notifyDataSetChanged();
//                                        progressBar.setVisibility(View.GONE);
//                                    }
//
//                                    @Override
//                                    public void onCancelled(@NonNull DatabaseError databaseError) {
//                                        progressBar.setVisibility(View.GONE);
//                                    }
//                                });
//                    }
//                } else {
//                    progressBar.setVisibility(View.GONE);
//                    tvEmptyFriendsList.setVisibility(View.VISIBLE);
//                    Toast.makeText(getContext(), "User not found!", Toast.LENGTH_SHORT).show();
//                }
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError databaseError) {
//                progressBar.setVisibility(View.GONE);
//                Toast.makeText(getContext(), "Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
////    private void searchUser(String userId) {
////        progressBar.setVisibility(View.VISIBLE);
////        tvEmptyFriendsList.setVisibility(View.GONE);
////
////        usersDatabase.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
////            @Override
////            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
////                findFriendModelList.clear();
////
////                if (dataSnapshot.exists()) {
////                    String fullName = dataSnapshot.child("name").getValue(String.class);
////                    String photoUrl = dataSnapshot.child("photo").getValue(String.class);
////
////                    friendRequestDatabase.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
////                        @Override
////                        public void onDataChange(@NonNull DataSnapshot requestSnapshot) {
////                            boolean requestSent = requestSnapshot.exists() &&
////                                    "sent".equals(requestSnapshot.child("requestType").getValue(String.class));
////
////                            findFriendModelList.add(new FindFriendModel(fullName, photoUrl, userId, requestSent));
////                            findFriendAdapter.notifyDataSetChanged();
////                            progressBar.setVisibility(View.GONE);
////                        }
////
////                        @Override
////                        public void onCancelled(@NonNull DatabaseError databaseError) {
////                            progressBar.setVisibility(View.GONE);
////                        }
////                    });
////
////                } else {
////                    progressBar.setVisibility(View.GONE);
////                    tvEmptyFriendsList.setVisibility(View.VISIBLE);
////                    Toast.makeText(getContext(), "User not found!", Toast.LENGTH_SHORT).show();
////                }
////            }
////
////            @Override
////            public void onCancelled(@NonNull DatabaseError databaseError) {
////                progressBar.setVisibility(View.GONE);
////                Toast.makeText(getContext(), "Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
////            }
////        });
////    }
//}
