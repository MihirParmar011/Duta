package com.pm.appdev.duta.findfriends;

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

import java.util.ArrayList;
import java.util.List;

public class FindFriendsFragment extends Fragment {

    private RecyclerView rvFindFriends;
    private FindFriendAdapter findFriendAdapter;
    private List<FindFriendModel> findFriendModelList;
    private TextView tvEmptyFriendsList;
    private SearchView searchView;
    private DatabaseReference databaseReference, databaseReferenceFriendRequests;
    private FirebaseUser currentUser;
    private View progressBar;

    public FindFriendsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_find_friends, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvFindFriends = view.findViewById(R.id.rvFindFriends);
        progressBar = view.findViewById(R.id.progressBar);
        tvEmptyFriendsList = view.findViewById(R.id.tvEmptyFriendsList);
        searchView = view.findViewById(R.id.searchView);

        rvFindFriends.setLayoutManager(new LinearLayoutManager(getActivity()));

        findFriendModelList = new ArrayList<>();
        findFriendAdapter = new FindFriendAdapter(getActivity(), findFriendModelList);
        rvFindFriends.setAdapter(findFriendAdapter);

        databaseReference = FirebaseDatabase.getInstance().getReference().child(NodeNames.USERS);
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        databaseReferenceFriendRequests = FirebaseDatabase.getInstance().getReference().child(NodeNames.FRIEND_REQUESTS).child(currentUser.getUid());

        tvEmptyFriendsList.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.VISIBLE);

        // Set up SearchView listener
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchUserById(query); // Search for user by userId
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {
                    findFriendModelList.clear();
                    findFriendAdapter.notifyDataSetChanged();
                    tvEmptyFriendsList.setVisibility(View.VISIBLE);
                }
                return false;
            }
        });
    }

    private void searchUserById(String userId) {
        progressBar.setVisibility(View.VISIBLE);
        findFriendModelList.clear(); // Clear the list before searching

        // Query Firebase for the specific userId
        Query query = databaseReference.orderByChild(NodeNames.USER_ID).equalTo(userId);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                findFriendModelList.clear();
                if (dataSnapshot.exists()) {
                    for (DataSnapshot ds : dataSnapshot.getChildren()) {
                        // Get the userId from the child node
                        String id = ds.child(NodeNames.USER_ID).getValue(String.class);
                        if (id == null || id.equals(currentUser.getUid())) continue; // Skip the current user

                        String fullName = ds.child(NodeNames.NAME).getValue(String.class);
                        String photoName = ds.child(NodeNames.PHOTO).getValue(String.class);

                        // Check if a friend request has already been sent
                        databaseReferenceFriendRequests.child(id).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                boolean requestSent = dataSnapshot.exists() && dataSnapshot.child(NodeNames.REQUEST_TYPE).getValue(String.class).equals(Constants.REQUEST_STATUS_SENT);
                                findFriendModelList.add(new FindFriendModel(fullName, photoName, id, requestSent));
                                findFriendAdapter.notifyDataSetChanged();
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                progressBar.setVisibility(View.GONE);
                            }
                        });
                    }
                } else {
                    Toast.makeText(getContext(), "User not found", Toast.LENGTH_SHORT).show();
                }
                progressBar.setVisibility(View.GONE);
                tvEmptyFriendsList.setVisibility(findFriendModelList.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Failed to search user: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}

//package com.pm.appdev.duta.findfriends;
//
//import android.os.Bundle;
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.fragment.app.Fragment;
//import androidx.recyclerview.widget.LinearLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.SearchView;
//import android.widget.TextView;
//import android.widget.Toast;
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.auth.FirebaseUser;
//import com.google.firebase.database.DataSnapshot;
//import com.google.firebase.database.DatabaseError;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//import com.google.firebase.database.Query;
//import com.google.firebase.database.ValueEventListener;
//import com.pm.appdev.duta.Common.Constants;
//import com.pm.appdev.duta.Common.NodeNames;
//import com.pm.appdev.duta.R;
//import java.util.ArrayList;
//import java.util.List;
//import android.widget.SearchView;
//
//public class FindFriendsFragment extends Fragment {
//    private RecyclerView rvFindFriends;
//    private FindFriendAdapter findFriendAdapter;
//    private List<FindFriendModel> findFriendModelList;
//    private List<FindFriendModel> filteredFriendModelList; // For search results
//    private TextView tvEmptyFriendsList;
//    private SearchView searchView;
//    private DatabaseReference databaseReference, databaseReferenceFriendRequests;
//    private FirebaseUser currentUser;
//    private View progressBar;
//
//    public FindFriendsFragment() {
//        // Required empty public constructor
//    }
//
//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container,
//                             Bundle savedInstanceState) {
//        return inflater.inflate(R.layout.fragment_find_friends, container, false);
//    }
//
//    @Override
//    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
//        super.onViewCreated(view, savedInstanceState);
//
//        rvFindFriends = view.findViewById(R.id.rvFindFriends);
//        progressBar = view.findViewById(R.id.progressBar);
//        tvEmptyFriendsList = view.findViewById(R.id.tvEmptyFriendsList);
//        searchView = view.findViewById(R.id.searchView);
//
//        rvFindFriends.setLayoutManager(new LinearLayoutManager(getActivity()));
//
//        findFriendModelList = new ArrayList<>();
//        filteredFriendModelList = new ArrayList<>(); // Initialize the filtered list
//        findFriendAdapter = new FindFriendAdapter(getActivity(), filteredFriendModelList); // Use filtered list for adapter
//        rvFindFriends.setAdapter(findFriendAdapter);
//
//        databaseReference = FirebaseDatabase.getInstance().getReference().child(NodeNames.USERS);
//        currentUser = FirebaseAuth.getInstance().getCurrentUser();
//        databaseReferenceFriendRequests = FirebaseDatabase.getInstance().getReference().child(NodeNames.FRIEND_REQUESTS).child(currentUser.getUid());
//
//        tvEmptyFriendsList.setVisibility(View.VISIBLE);
//        progressBar.setVisibility(View.VISIBLE);
//
//        // Set up SearchView listener
//        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
//            @Override
//            public boolean onQueryTextSubmit(String query) {
//                filterUsers(query); // Filter users when search is submitted
//                return false;
//            }
//
//            @Override
//            public boolean onQueryTextChange(String newText) {
//                filterUsers(newText); // Filter users as the text changes
//                return false;
//            }
//        });
//
//        // Load all users initially
//        loadAllUsers();
//    }
//
//    private void loadAllUsers() {
//        Query query = databaseReference.orderByChild(NodeNames.NAME);
//        query.addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                findFriendModelList.clear();
//                for (DataSnapshot ds : dataSnapshot.getChildren()) {
//                    String userId = ds.getKey();
//                    if (userId.equals(currentUser.getUid())) continue; // Skip the current user
//
//                    String fullName = ds.child(NodeNames.NAME).getValue(String.class);
//                    String photoName = ds.child(NodeNames.PHOTO).getValue(String.class);
//
//                    databaseReferenceFriendRequests.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
//                        @Override
//                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                            boolean requestSent = dataSnapshot.exists() && dataSnapshot.child(NodeNames.REQUEST_TYPE).getValue(String.class).equals(Constants.REQUEST_STATUS_SENT);
//                            findFriendModelList.add(new FindFriendModel(fullName, photoName, userId, requestSent));
//                            filterUsers(searchView.getQuery().toString()); // Re-filter the list after adding new data
//                        }
//
//                        @Override
//                        public void onCancelled(@NonNull DatabaseError databaseError) {
//                            progressBar.setVisibility(View.GONE);
//                        }
//                    });
//                }
//                progressBar.setVisibility(View.GONE);
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError databaseError) {
//                progressBar.setVisibility(View.GONE);
//                Toast.makeText(getContext(), "Failed to load users: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
//
//    private void filterUsers(String query) {
//        filteredFriendModelList.clear(); // Clear the filtered list
//        if (query.isEmpty()) {
//            // If the query is empty, show all users
//            filteredFriendModelList.addAll(findFriendModelList);
//        } else {
//            // Filter the list based on the query
//            String lowerCaseQuery = query.toLowerCase();
//            for (FindFriendModel user : findFriendModelList) {
//                if (user.getUserName().toLowerCase().contains(lowerCaseQuery)) {
//                    filteredFriendModelList.add(user);
//                }
//            }
//        }
//        findFriendAdapter.notifyDataSetChanged(); // Notify the adapter of data changes
//        tvEmptyFriendsList.setVisibility(filteredFriendModelList.isEmpty() ? View.VISIBLE : View.GONE); // Show empty state if no results
//    }
//}

//package com.pm.appdev.duta.findfriends;
//
//import android.os.Bundle;
//
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.fragment.app.Fragment;
//import androidx.recyclerview.widget.LinearLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.TextView;
//import android.widget.Toast;
//
//
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.auth.FirebaseUser;
//import com.google.firebase.database.DataSnapshot;
//import com.google.firebase.database.DatabaseError;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//import com.google.firebase.database.Query;
//import com.google.firebase.database.ValueEventListener;
//import com.pm.appdev.duta.Common.Constants;
//import com.pm.appdev.duta.Common.NodeNames;
//import com.pm.appdev.duta.R;
//
//import java.util.ArrayList;
//import java.util.List;
//
//
///**
// * A simple {@link Fragment} subclass.
// */
//public class FindFriendsFragment extends Fragment {
//
//    private RecyclerView rvFindFriends;
//    private FindFriendAdapter findFriendAdapter;
//    private List<FindFriendModel> findFriendModelList;
//    private TextView tvEmptyFriendsList;
//
//    private DatabaseReference databaseReference, databaseReferenceFriendRequests;
//    private FirebaseUser currentUser;
//    private  View progressBar;
//
//
//    public FindFriendsFragment() {
//        // Required empty public constructor
//    }
//
//
//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container,
//                             Bundle savedInstanceState) {
//        // Inflate the layout for this fragment
//        return inflater.inflate(R.layout.fragment_find_friends, container, false);
//    }
//
//    @Override
//    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
//        super.onViewCreated(view, savedInstanceState);
//
//        rvFindFriends = view.findViewById(R.id.rvFindFriends);
//        progressBar = view.findViewById(R.id.progressBar);
//        tvEmptyFriendsList = view.findViewById(R.id.tvEmptyFriendsList);
//
//        rvFindFriends.setLayoutManager(new LinearLayoutManager(getActivity()));
//
//        findFriendModelList =new ArrayList<>();
//        findFriendAdapter = new FindFriendAdapter(getActivity(), findFriendModelList);
//        rvFindFriends.setAdapter(findFriendAdapter);
//
//        databaseReference = FirebaseDatabase.getInstance().getReference().child(NodeNames.USERS);
//        currentUser  = FirebaseAuth.getInstance().getCurrentUser();
//
//        databaseReferenceFriendRequests = FirebaseDatabase.getInstance().getReference().child(NodeNames.FRIEND_REQUESTS).child(currentUser.getUid());
//
//        tvEmptyFriendsList.setVisibility(View.VISIBLE);
//        progressBar.setVisibility(View.VISIBLE);
//
//        Query query = databaseReference.orderByChild(NodeNames.NAME);
//        query.addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                findFriendModelList.clear();
//                for (final DataSnapshot ds : dataSnapshot.getChildren())
//                {
//                    final String userId= ds.getKey();
//
//                    if(userId.equals(currentUser.getUid()))
//                        continue;
//
//                    if(ds.child(NodeNames.NAME).getValue()!=null)
//                    {
//                        final String fullName = ds.child(NodeNames.NAME).getValue().toString();
//                        final String photoName = ds.child(NodeNames.PHOTO).getValue()!=null? ds.child(NodeNames.PHOTO).getValue().toString():"";
//
//                        databaseReferenceFriendRequests.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
//                            @Override
//                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                                if(dataSnapshot.exists())
//                                {
//                                    String requestType =  dataSnapshot.child(NodeNames.REQUEST_TYPE).getValue().toString();
//                                    if(requestType.equals(Constants.REQUEST_STATUS_SENT))
//                                    {
//                                        findFriendModelList.add(new FindFriendModel(fullName, photoName, userId, true));
//                                        findFriendAdapter.notifyDataSetChanged();
//
//                                    }
//                                }
//                                else{
//                                    findFriendModelList.add(new FindFriendModel(fullName, photoName, userId, false));
//                                    findFriendAdapter.notifyDataSetChanged();
//
//                                }
//                            }
//
//                            @Override
//                            public void onCancelled(@NonNull DatabaseError databaseError) {
//                                progressBar.setVisibility(View.GONE);
//                            }
//                        });
//
//
//                       tvEmptyFriendsList.setVisibility(View.GONE);
//                       progressBar.setVisibility(View.GONE);
//                    }
//                }
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError databaseError) {
//                    progressBar.setVisibility(View.GONE);
//                Toast.makeText(getContext(), getContext().getString( R.string.failed_to_fetch_friends, databaseError.getMessage())
//                        , Toast.LENGTH_SHORT).show();
//            }
//        });
//
//    }
//}