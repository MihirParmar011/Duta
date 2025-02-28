package com.pm.appdev.duta.requests;

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
import com.google.firebase.database.ValueEventListener;
import com.pm.appdev.duta.BlankFragment;
import com.pm.appdev.duta.R;

import java.util.ArrayList;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 */
public class RequestsFragment extends Fragment {


    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public RequestsFragment() {
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
//    private RecyclerView rvRequests;
//    private RequestAdapter adapter;
//    private List<RequestModel> requestModelList;
//    private TextView tvEmptyRequestsList;
//
//    private DatabaseReference databaseReferenceRequests, databaseReferenceUsers;
//    private FirebaseUser currentUser;
//    private  View progressBar;
//
//
//    public RequestsFragment() {
//        // Required empty public constructor
//    }
//
//
//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container,
//                             Bundle savedInstanceState) {
//        // Inflate the layout for this fragment
//        return inflater.inflate(R.layout.fragment_requests, container, false);
//    }
//
//    @Override
//    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
//        super.onViewCreated(view, savedInstanceState);
//
//        rvRequests = view.findViewById(R.id.rvRequests);
//        tvEmptyRequestsList = view.findViewById(R.id.tvEmptyRequestsList);
//
//        progressBar = view.findViewById(R.id.progressBar);
//
//        rvRequests.setLayoutManager( new LinearLayoutManager(getActivity()));
//        requestModelList = new ArrayList<>();
//        adapter = new RequestAdapter(getActivity(), requestModelList);
//        rvRequests.setAdapter(adapter);
//
//        currentUser = FirebaseAuth.getInstance().getCurrentUser();
//        databaseReferenceUsers = FirebaseDatabase.getInstance().getReference().child(NodeNames.USERS);
//
//        databaseReferenceRequests = FirebaseDatabase.getInstance().getReference().child(NodeNames.FRIEND_REQUESTS).child(currentUser.getUid());
//
//        tvEmptyRequestsList.setVisibility(View.VISIBLE);
//        progressBar.setVisibility(View.VISIBLE);
//
//
//        databaseReferenceRequests.addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                progressBar.setVisibility(View.GONE);
//                requestModelList.clear();
//
//                for(DataSnapshot ds : dataSnapshot.getChildren()){
//                    if(ds.exists()){
//                        String requestType = ds.child(NodeNames.REQUEST_TYPE).getValue().toString();
//                        if(requestType.equals(Constants.REQUEST_STATUS_RECEIVED))
//                        {
//                            final String userId= ds.getKey();
//                            databaseReferenceUsers.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
//                                @Override
//                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                                    String userName = dataSnapshot.child(NodeNames.NAME).getValue().toString();
//
//                                    String photoName="";
//                                    if(dataSnapshot.child(NodeNames.PHOTO).getValue()!=null)
//                                    {
//                                        photoName = dataSnapshot.child(NodeNames.PHOTO).getValue().toString();
//                                    }
//
//                                    RequestModel requestModel = new RequestModel(userId, userName, photoName);
//                                    requestModelList.add(requestModel);
//                                    adapter.notifyDataSetChanged();
//                                    tvEmptyRequestsList.setVisibility(View.GONE);
//                                }
//
//                                @Override
//                                public void onCancelled(@NonNull DatabaseError databaseError) {
//                                    Toast.makeText(getActivity(),  getActivity().getString( R.string.failed_to_fetch_friend_requests, databaseError.getMessage())
//                                            , Toast.LENGTH_SHORT).show();
//                                    progressBar.setVisibility(View.GONE);
//                                }
//                            });
//                        }
//                    }
//                }
//
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError databaseError) {
//                Toast.makeText(getActivity(),  getActivity().getString( R.string.failed_to_fetch_friend_requests, databaseError.getMessage())
//                        , Toast.LENGTH_SHORT).show();
//                progressBar.setVisibility(View.GONE);
//            }
//        });
//
//
//    }
//
//}















