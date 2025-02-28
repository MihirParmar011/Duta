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
import com.pm.appdev.duta.BlankFragment;
import com.pm.appdev.duta.R;

import java.util.ArrayList;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 */
public class FindFriendsFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public FindFriendsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment BlankFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static BlankFragment newInstance(String param1, String param2) {
        BlankFragment fragment = new BlankFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_blank, container, false);
    }
}
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













