package com.pm.appdev.duta.requests;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

public class RequestsFragment extends Fragment {

    private RecyclerView rvRequests;
    private RequestAdapter adapter;
    private List<RequestModel> requestModelList;
    private TextView tvEmptyRequestsList;
    private View progressBar;

    private DatabaseReference friendRequestsDatabase, usersDatabase;
    private FirebaseUser currentUser;
    private String currentUserId;

    public RequestsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_requests, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvRequests = view.findViewById(R.id.rvRequests);
        tvEmptyRequestsList = view.findViewById(R.id.tvEmptyRequestsList);
        progressBar = view.findViewById(R.id.progressBar);

        rvRequests.setLayoutManager(new LinearLayoutManager(getActivity()));
        requestModelList = new ArrayList<>();
        adapter = new RequestAdapter(getActivity(), requestModelList);
        rvRequests.setAdapter(adapter);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getDisplayName();
            if (currentUserId == null || currentUserId.isEmpty()) {
                currentUserId = currentUser.getUid();  // Fallback to UID if display name is null
            }
        } else {
            Toast.makeText(getActivity(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        usersDatabase = FirebaseDatabase.getInstance().getReference("Users");
        friendRequestsDatabase = FirebaseDatabase.getInstance().getReference("FriendRequests").child(currentUserId);

        loadFriendRequests();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadFriendRequests();  // Refresh data when fragment is reopened
    }

    private void loadFriendRequests() {
        progressBar.setVisibility(View.VISIBLE);
        Log.d("RequestsFragment", "Fetching requests for user: " + currentUserId);

        friendRequestsDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                progressBar.setVisibility(View.GONE);
                requestModelList.clear();

                if (!dataSnapshot.exists()) {
                    Log.d("RequestsFragment", "No requests found for user: " + currentUserId);
                    tvEmptyRequestsList.setVisibility(View.VISIBLE);
                    adapter.notifyDataSetChanged();
                    return;
                }

                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    String requestType = ds.child("requestType").getValue(String.class);
                    if (requestType == null) {
                        requestType = ds.child("request_type").getValue(String.class);
                    }

                    if ("received".equals(requestType)) {
                        final String senderUserId = ds.getKey();
                        Log.d("RequestsFragment", "Request received from: " + senderUserId);

                        usersDatabase.child(senderUserId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                                if (!userSnapshot.exists()) {
                                    Log.e("RequestsFragment", "User details not found for: " + senderUserId);
                                    return;
                                }

                                String userName = userSnapshot.child("name").getValue(String.class);
                                String photoUrl = userSnapshot.child("photo").exists() ?
                                        userSnapshot.child("photo").getValue(String.class) : "";

                                Log.d("RequestsFragment", "Adding request from: " + userName);
                                requestModelList.add(new RequestModel(senderUserId, userName, photoUrl));
                                adapter.notifyDataSetChanged();
                                tvEmptyRequestsList.setVisibility(View.GONE);
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                Log.e("RequestsFragment", "Error fetching user details: " + databaseError.getMessage());
                                Toast.makeText(getActivity(), "Error fetching user details", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }

                if (requestModelList.isEmpty()) {
                    tvEmptyRequestsList.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                progressBar.setVisibility(View.GONE);
                Log.e("RequestsFragment", "Error loading requests: " + databaseError.getMessage());
                Toast.makeText(getActivity(), "Error loading requests", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
