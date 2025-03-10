package com.pm.appdev.duta.findfriends;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.pm.appdev.duta.Common.Constants;
import com.pm.appdev.duta.Common.NodeNames;
import com.pm.appdev.duta.R;

import java.util.List;

public class FindFriendAdapter extends RecyclerView.Adapter<FindFriendAdapter.FindFriendViewHolder> {

    private Context context;
    private List<FindFriendModel> findFriendModelList;
    private DatabaseReference friendRequestDatabase, usersDatabase;
    private FirebaseUser currentUser;

    public FindFriendAdapter(Context context, List<FindFriendModel> findFriendModelList) {
        this.context = context;
        this.findFriendModelList = findFriendModelList;
        friendRequestDatabase = FirebaseDatabase.getInstance().getReference().child(NodeNames.FRIEND_REQUESTS);
        usersDatabase = FirebaseDatabase.getInstance().getReference().child(NodeNames.USERS);
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
    }

    @NonNull
    @Override
    public FindFriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.find_friends_layout, parent, false);
        return new FindFriendViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final FindFriendViewHolder holder, int position) {
        final FindFriendModel friendModel = findFriendModelList.get(position);

        holder.tvFullName.setText(friendModel.getUserName());

        // Load profile picture
        Glide.with(context)
                .load(friendModel.getPhotoUrl())
                .placeholder(R.drawable.default_profile)
                .error(R.drawable.default_profile)
                .into(holder.ivProfile);

        if (friendModel.isRequestSent()) {
            holder.btnSendRequest.setVisibility(View.GONE);
            holder.btnCancelRequest.setVisibility(View.VISIBLE);
        } else {
            holder.btnSendRequest.setVisibility(View.VISIBLE);
            holder.btnCancelRequest.setVisibility(View.GONE);
        }

        // **Send Friend Request**
        holder.btnSendRequest.setOnClickListener(view -> fetchUserIDsAndSendRequest(holder, friendModel));

        // **Cancel Friend Request**
        holder.btnCancelRequest.setOnClickListener(view -> cancelFriendRequest(holder, friendModel));
    }

    /**
     * ✅ **Fetch `userID` from UID before sending request**
     */
    private void fetchUserIDsAndSendRequest(FindFriendViewHolder holder, FindFriendModel friendModel) {
        String senderUID = currentUser.getUid();
        String receiverUID = friendModel.getUserId();

        usersDatabase.child(senderUID).child(NodeNames.USER_ID).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot senderSnapshot) {
                if (!senderSnapshot.exists()) {
                    showError(holder, "Error fetching sender ID.");
                    return;
                }

                String senderUserID = senderSnapshot.getValue(String.class);

                usersDatabase.child(receiverUID).child(NodeNames.USER_ID).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot receiverSnapshot) {
                        if (!receiverSnapshot.exists()) {
                            showError(holder, "Error fetching receiver ID.");
                            return;
                        }

                        String receiverUserID = receiverSnapshot.getValue(String.class);
                        sendFriendRequest(holder, senderUserID, receiverUserID);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        showError(holder, "Database error: " + error.getMessage());
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showError(holder, "Database error: " + error.getMessage());
            }
        });
    }

    /**
     * 📌 **Store Friend Requests using `userID` instead of UID**
     */
    private void sendFriendRequest(FindFriendViewHolder holder, String senderUserID, String receiverUserID) {
        holder.btnSendRequest.setVisibility(View.GONE);
        holder.pbRequest.setVisibility(View.VISIBLE);

        DatabaseReference senderRef = friendRequestDatabase.child(senderUserID).child(receiverUserID);
        DatabaseReference receiverRef = friendRequestDatabase.child(receiverUserID).child(senderUserID);

        senderRef.child(NodeNames.REQUEST_TYPE).setValue(Constants.REQUEST_STATUS_SENT)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        receiverRef.child(NodeNames.REQUEST_TYPE).setValue(Constants.REQUEST_STATUS_RECEIVED)
                                .addOnCompleteListener(task1 -> {
                                    if (task1.isSuccessful()) {
                                        holder.btnSendRequest.setVisibility(View.GONE);
                                        holder.pbRequest.setVisibility(View.GONE);
                                        holder.btnCancelRequest.setVisibility(View.VISIBLE);
                                        Toast.makeText(context, "Friend request sent!", Toast.LENGTH_SHORT).show();
                                    } else {
                                        showError(holder, "Failed to send request.");
                                    }
                                });
                    } else {
                        showError(holder, "Failed to send request.");
                    }
                });
    }

    /**
     * 📌 **Fix: Cancel Friend Request using `userID`**
     */
    private void cancelFriendRequest(FindFriendViewHolder holder, FindFriendModel friendModel) {
        holder.btnCancelRequest.setVisibility(View.GONE);
        holder.pbRequest.setVisibility(View.VISIBLE);

        String senderUID = currentUser.getUid();
        String receiverUID = friendModel.getUserId();

        usersDatabase.child(senderUID).child(NodeNames.USER_ID).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot senderSnapshot) {
                if (!senderSnapshot.exists()) {
                    showError(holder, "Error fetching sender ID.");
                    return;
                }

                String senderUserID = senderSnapshot.getValue(String.class);

                usersDatabase.child(receiverUID).child(NodeNames.USER_ID).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot receiverSnapshot) {
                        if (!receiverSnapshot.exists()) {
                            showError(holder, "Error fetching receiver ID.");
                            return;
                        }

                        String receiverUserID = receiverSnapshot.getValue(String.class);
                        deleteFriendRequest(holder, senderUserID, receiverUserID);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        showError(holder, "Database error: " + error.getMessage());
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showError(holder, "Database error: " + error.getMessage());
            }
        });
    }

    private void deleteFriendRequest(FindFriendViewHolder holder, String senderUserID, String receiverUserID) {
        DatabaseReference senderRef = friendRequestDatabase.child(senderUserID).child(receiverUserID);
        DatabaseReference receiverRef = friendRequestDatabase.child(receiverUserID).child(senderUserID);

        senderRef.removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                receiverRef.removeValue().addOnCompleteListener(task1 -> {
                    holder.btnSendRequest.setVisibility(View.VISIBLE);
                    holder.pbRequest.setVisibility(View.GONE);
                    holder.btnCancelRequest.setVisibility(View.GONE);
                    Toast.makeText(context, "Friend request cancelled.", Toast.LENGTH_SHORT).show();
                });
            } else {
                showError(holder, "Failed to cancel request.");
            }
        });
    }

    private void showError(FindFriendViewHolder holder, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        holder.btnSendRequest.setVisibility(View.VISIBLE);
        holder.pbRequest.setVisibility(View.GONE);
    }

    @Override
    public int getItemCount() {
        return findFriendModelList.size();
    }

    /**
     * ✅ **FindFriendViewHolder Class**
     */
    public static class FindFriendViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivProfile;
        private TextView tvFullName;
        private Button btnSendRequest, btnCancelRequest;
        private ProgressBar pbRequest;

        public FindFriendViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProfile = itemView.findViewById(R.id.ivProfile);
            tvFullName = itemView.findViewById(R.id.tvFullName);
            btnSendRequest = itemView.findViewById(R.id.btnSendRequest);
            btnCancelRequest = itemView.findViewById(R.id.btnCancelRequest);
            pbRequest = itemView.findViewById(R.id.pbRequest);
        }
    }
}






//package com.pm.appdev.duta.findfriends;
//
//import android.content.Context;
//import android.net.Uri;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.Button;
//import android.widget.ImageView;
//import android.widget.ProgressBar;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.bumptech.glide.Glide;
//import com.google.android.gms.tasks.OnCompleteListener;
//import com.google.android.gms.tasks.OnSuccessListener;
//import com.google.android.gms.tasks.Task;
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.auth.FirebaseUser;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//import com.google.firebase.storage.FirebaseStorage;
//import com.google.firebase.storage.StorageReference;
//import com.pm.appdev.duta.R;
//import com.pm.appdev.duta.Common.NodeNames;
//import com.pm.appdev.duta.Common.Constants;
//import com.pm.appdev.duta.findfriends.FindFriendModel;
//
//import java.util.HashMap;
//import java.util.List;
//
//public class FindFriendAdapter extends RecyclerView.Adapter<FindFriendAdapter.FindFriendViewHolder> {
//
//    private Context context;
//    private List<FindFriendModel> findFriendModelList;
//    private DatabaseReference friendRequestDatabase;
//    private FirebaseUser currentUser;
//
//    public FindFriendAdapter(Context context, List<FindFriendModel> findFriendModelList) {
//        this.context = context;
//        this.findFriendModelList = findFriendModelList;
//        friendRequestDatabase = FirebaseDatabase.getInstance().getReference().child(NodeNames.FRIEND_REQUESTS);
//        currentUser = FirebaseAuth.getInstance().getCurrentUser();
//    }
//
//    @NonNull
//    @Override
//    public FindFriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//        View view = LayoutInflater.from(context).inflate(R.layout.find_friends_layout, parent, false);
//        return new FindFriendViewHolder(view);
//    }
//
//    @Override
//    public void onBindViewHolder(@NonNull final FindFriendViewHolder holder, int position) {
//        final FindFriendModel friendModel = findFriendModelList.get(position);
//
//        // Set User Name
//        holder.tvFullName.setText(friendModel.getUserName());
//
//        // Load Profile Image using Glide
//        Glide.with(context)
//                .load(friendModel.getPhotoUrl())
//                .placeholder(R.drawable.ic_profile) // Fallback image
//                .error(R.drawable.ic_profile) // If loading fails
//                .into(holder.ivProfile);
//
//        // Show correct button states based on request status
//        if (friendModel.isRequestSent()) {
//            holder.btnSendRequest.setVisibility(View.GONE);
//            holder.btnCancelRequest.setVisibility(View.VISIBLE);
//        } else {
//            holder.btnSendRequest.setVisibility(View.VISIBLE);
//            holder.btnCancelRequest.setVisibility(View.GONE);
//        }
//
//        // Send Friend Request Button Click
//        holder.btnSendRequest.setOnClickListener(v -> sendFriendRequest(holder, friendModel));
//
//        // Cancel Friend Request Button Click
//        holder.btnCancelRequest.setOnClickListener(v -> cancelFriendRequest(holder, friendModel));
//    }
//
//    private void sendFriendRequest(FindFriendViewHolder holder, FindFriendModel friendModel) {
//        holder.btnSendRequest.setVisibility(View.GONE);
//        holder.pbRequest.setVisibility(View.VISIBLE);
//
//        // Store request properly under both sender and receiver
//        friendRequestDatabase.child(currentUser.getUid()).child(friendModel.getUserId())
//                .child(NodeNames.REQUEST_TYPE).setValue(Constants.REQUEST_STATUS_SENT)
//                .addOnCompleteListener(task -> {
//                    if (task.isSuccessful()) {
//                        friendRequestDatabase.child(friendModel.getUserId()).child(currentUser.getUid())
//                                .child(NodeNames.REQUEST_TYPE).setValue(Constants.REQUEST_STATUS_RECEIVED)
//                                .addOnCompleteListener(task1 -> {
//                                    if (task1.isSuccessful()) {
//                                        holder.btnSendRequest.setVisibility(View.GONE);
//                                        holder.pbRequest.setVisibility(View.GONE);
//                                        holder.btnCancelRequest.setVisibility(View.VISIBLE);
//                                        Toast.makeText(context, "Friend request sent!", Toast.LENGTH_SHORT).show();
//                                    } else {
//                                        holder.btnSendRequest.setVisibility(View.VISIBLE);
//                                        holder.pbRequest.setVisibility(View.GONE);
//                                    }
//                                });
//                    } else {
//                        holder.btnSendRequest.setVisibility(View.VISIBLE);
//                        holder.pbRequest.setVisibility(View.GONE);
//                    }
//                });
//    }
//
//
//    private void cancelFriendRequest(FindFriendViewHolder holder, FindFriendModel friendModel) {
//        holder.btnCancelRequest.setVisibility(View.GONE);
//        holder.pbRequest.setVisibility(View.VISIBLE);
//
//        friendRequestDatabase.child(currentUser.getUid()).child(friendModel.getUserId())
//                .removeValue().addOnCompleteListener(task -> {
//                    if (task.isSuccessful()) {
//                        friendRequestDatabase.child(friendModel.getUserId()).child(currentUser.getUid())
//                                .removeValue().addOnCompleteListener(task1 -> {
//                                    holder.btnSendRequest.setVisibility(View.VISIBLE);
//                                    holder.pbRequest.setVisibility(View.GONE);
//                                    holder.btnCancelRequest.setVisibility(View.GONE);
//                                    Toast.makeText(context, "Friend request cancelled.", Toast.LENGTH_SHORT).show();
//                                });
//                    } else {
//                        holder.btnCancelRequest.setVisibility(View.VISIBLE);
//                        holder.pbRequest.setVisibility(View.GONE);
//                        Toast.makeText(context, "Failed to cancel request.", Toast.LENGTH_SHORT).show();
//                    }
//                });
//    }
//
//    @Override
//    public int getItemCount() {
//        return findFriendModelList.size();
//    }
//
//    public static class FindFriendViewHolder extends RecyclerView.ViewHolder {
//        private ImageView ivProfile;
//        private TextView tvFullName;
//        private Button btnSendRequest, btnCancelRequest;
//        private ProgressBar pbRequest;
//
//        public FindFriendViewHolder(@NonNull View itemView) {
//            super(itemView);
//            ivProfile = itemView.findViewById(R.id.ivProfile);
//            tvFullName = itemView.findViewById(R.id.tvFullName);
//            btnSendRequest = itemView.findViewById(R.id.btnSendRequest);
//            btnCancelRequest = itemView.findViewById(R.id.btnCancelRequest);
//            pbRequest = itemView.findViewById(R.id.pbRequest);
//        }
//    }
//}