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
import com.pm.appdev.duta.R;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FindFriendAdapter extends RecyclerView.Adapter<FindFriendAdapter.FindFriendViewHolder> {

    private Context context;
    private List<FindFriendModel> findFriendModelList;
    private DatabaseReference friendRequestDatabase;
    private DatabaseReference usersDatabase; // Add this line
    private FirebaseUser currentUser;

    // Update constructor to accept usersDatabase
    public FindFriendAdapter(Context context, List<FindFriendModel> findFriendModelList, DatabaseReference usersDatabase) {
        this.context = context;
        this.findFriendModelList = findFriendModelList;
        this.usersDatabase = usersDatabase; // Initialize usersDatabase
        this.currentUser = FirebaseAuth.getInstance().getCurrentUser();
        this.friendRequestDatabase = FirebaseDatabase.getInstance().getReference("FriendRequests");
    }

    @NonNull
    @Override
    public FindFriendAdapter.FindFriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.find_friends_layout, parent, false);
        return new FindFriendViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final FindFriendAdapter.FindFriendViewHolder holder, int position) {
        final FindFriendModel friendModel = findFriendModelList.get(position);

        holder.tvFullName.setText(friendModel.getUserName());

        Glide.with(context)
                .load(friendModel.getPhotoName())
                .placeholder(R.drawable.default_profile)
                .into(holder.ivProfile);

        if (friendModel.isRequestSent()) {
            holder.btnSendRequest.setVisibility(View.GONE);
            holder.btnCancelRequest.setVisibility(View.VISIBLE);
        } else {
            holder.btnSendRequest.setVisibility(View.VISIBLE);
            holder.btnCancelRequest.setVisibility(View.GONE);
        }

        holder.btnSendRequest.setOnClickListener(view -> sendFriendRequest(friendModel, holder));
        holder.btnCancelRequest.setOnClickListener(view -> cancelFriendRequest(friendModel, holder));
    }


    private void sendFriendRequest(FindFriendModel friendModel, FindFriendViewHolder holder) {
        String receiverUserId = friendModel.getUserId();
        holder.btnSendRequest.setVisibility(View.GONE);
        holder.pbRequest.setVisibility(View.VISIBLE);

        // Retrieve the receiver's Firebase UID
        usersDatabase.child(receiverUserId).child("uid").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String receiverUid = dataSnapshot.getValue(String.class);

                    // Check if sender and receiver IDs are the same
                    if (currentUser.getUid().equals(receiverUid)) {
                        holder.pbRequest.setVisibility(View.GONE);
                        holder.btnSendRequest.setVisibility(View.VISIBLE);
                        Toast.makeText(context, "You cannot send a friend request to yourself!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Generate a unique request ID
                    String requestId = friendRequestDatabase.push().getKey();

                    // Create a new friend request entry
                    Map<String, Object> requestMap = new HashMap<>();
                    requestMap.put("senderId", currentUser.getUid());
                    requestMap.put("receiverId", receiverUid);
                    requestMap.put("status", "pending"); // Initial status is "pending"

                    // Save the request to Firebase
                    friendRequestDatabase.child(requestId).setValue(requestMap).addOnCompleteListener(task -> {
                        holder.pbRequest.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            Toast.makeText(context, "Friend request sent!", Toast.LENGTH_SHORT).show();
                            friendModel.setRequestSent(true);
                            notifyDataSetChanged();
                        } else {
                            holder.btnSendRequest.setVisibility(View.VISIBLE);
                            Toast.makeText(context, "Failed to send request!", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    holder.pbRequest.setVisibility(View.GONE);
                    holder.btnSendRequest.setVisibility(View.VISIBLE);
                    Toast.makeText(context, "User not found!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                holder.pbRequest.setVisibility(View.GONE);
                holder.btnSendRequest.setVisibility(View.VISIBLE);
                Toast.makeText(context, "Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void cancelFriendRequest(FindFriendModel friendModel, FindFriendViewHolder holder) {
        String receiverUserId = friendModel.getUserId();
        holder.btnCancelRequest.setVisibility(View.GONE);
        holder.pbRequest.setVisibility(View.VISIBLE);

        // Retrieve the receiver's Firebase UID
        usersDatabase.child(receiverUserId).child("uid").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String receiverUid = dataSnapshot.getValue(String.class);

                    // Find and remove the friend request
                    friendRequestDatabase.orderByChild("receiverId").equalTo(receiverUid)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot requestSnapshot) {
                                    for (DataSnapshot snapshot : requestSnapshot.getChildren()) {
                                        String senderId = snapshot.child("senderId").getValue(String.class);
                                        if (senderId != null && senderId.equals(currentUser.getUid())) {
                                            snapshot.getRef().removeValue().addOnCompleteListener(task -> {
                                                holder.pbRequest.setVisibility(View.GONE);
                                                if (task.isSuccessful()) {
                                                    Toast.makeText(context, "Friend request canceled!", Toast.LENGTH_SHORT).show();
                                                    friendModel.setRequestSent(false);
                                                    notifyDataSetChanged();
                                                } else {
                                                    holder.btnCancelRequest.setVisibility(View.VISIBLE);
                                                    Toast.makeText(context, "Failed to cancel request!", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                            break;
                                        }
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                    holder.btnCancelRequest.setVisibility(View.VISIBLE);
                                    holder.pbRequest.setVisibility(View.GONE);
                                    Toast.makeText(context, "Failed to cancel request!", Toast.LENGTH_SHORT).show();
                                }
                            });
                } else {
                    holder.pbRequest.setVisibility(View.GONE);
                    holder.btnCancelRequest.setVisibility(View.VISIBLE);
                    Toast.makeText(context, "User not found!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                holder.btnCancelRequest.setVisibility(View.VISIBLE);
                holder.pbRequest.setVisibility(View.GONE);
                Toast.makeText(context, "Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return findFriendModelList.size();
    }

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