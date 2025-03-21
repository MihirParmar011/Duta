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
import java.util.concurrent.atomic.AtomicReference;

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
    public void onBindViewHolder(@NonNull final FindFriendViewHolder holder, int position) {
        final FindFriendModel friendModel = findFriendModelList.get(position);

        holder.tvFullName.setText(friendModel.getUserName());

        Glide.with(context)
                .load(friendModel.getPhotoName())
                .placeholder(R.drawable.default_profile)
                .into(holder.ivProfile);

        friendRequestDatabase.orderByChild("senderUid").equalTo(currentUser.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        boolean alreadyRequested = false;
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            if (snapshot.child("receiverUid").getValue(String.class).equals(friendModel.getUid())) {
                                alreadyRequested = true;
                                break;
                            }
                        }
                        friendModel.setRequestSent(alreadyRequested);
                        notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });

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
        String receiverUid = friendModel.getUid();
        holder.btnSendRequest.setVisibility(View.GONE);
        holder.pbRequest.setVisibility(View.VISIBLE);

        if (currentUser.getUid().equals(receiverUid)) {
            holder.pbRequest.setVisibility(View.GONE);
            holder.btnSendRequest.setVisibility(View.VISIBLE);
            Toast.makeText(context, "You cannot send a friend request to yourself!", Toast.LENGTH_SHORT).show();
            return;
        }

        String requestId = friendRequestDatabase.push().getKey();

        usersDatabase.orderByChild("uid").equalTo(currentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String senderUserId = null;
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        senderUserId = snapshot.getKey();
                        break;
                    }
                    if (senderUserId != null) {
                        Map<String, Object> requestMap = new HashMap<>();
                        requestMap.put("senderUid", currentUser.getUid());
                        requestMap.put("senderUserId", senderUserId);
                        requestMap.put("receiverUid", receiverUid);
                        requestMap.put("receiverUserId", receiverUserId);
                        requestMap.put("status", "pending");
                        requestMap.put("timestamp", System.currentTimeMillis());

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
                        Toast.makeText(context, "Sender user ID not found!", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    holder.pbRequest.setVisibility(View.GONE);
                    holder.btnSendRequest.setVisibility(View.VISIBLE);
                    Toast.makeText(context, "Sender user ID not found!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                holder.pbRequest.setVisibility(View.GONE);
                holder.btnSendRequest.setVisibility(View.VISIBLE);
                Toast.makeText(context, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cancelFriendRequest(FindFriendModel friendModel, FindFriendViewHolder holder) {
        holder.btnCancelRequest.setVisibility(View.GONE);
        holder.pbRequest.setVisibility(View.VISIBLE);

        String receiverUid = friendModel.getUid();

        if (receiverUid == null || receiverUid.isEmpty()) {
            holder.pbRequest.setVisibility(View.GONE);
            holder.btnCancelRequest.setVisibility(View.VISIBLE);
            Toast.makeText(context, "Receiver UID not found!", Toast.LENGTH_SHORT).show();
            return;
        }

        friendRequestDatabase.orderByChild("senderUid").equalTo(currentUser.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot requestSnapshot) {
                        boolean requestFound = false;
                        for (DataSnapshot request : requestSnapshot.getChildren()) {
                            if (receiverUid.equals(request.child("receiverUid").getValue(String.class))) {
                                requestFound = true;
                                request.getRef().removeValue().addOnCompleteListener(task -> {
                                    holder.pbRequest.setVisibility(View.GONE);
                                    holder.btnCancelRequest.setVisibility(task.isSuccessful() ? View.GONE : View.VISIBLE);
                                    if (task.isSuccessful()) {
                                        friendModel.setRequestSent(false);
                                        notifyDataSetChanged();
                                        Toast.makeText(context, "Friend request canceled!", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(context, "Failed to cancel request!", Toast.LENGTH_SHORT).show();
                                    }
                                });
                                break;
                            }
                        }
                        if (!requestFound) {
                            holder.pbRequest.setVisibility(View.GONE);
                            holder.btnCancelRequest.setVisibility(View.VISIBLE);
                            Toast.makeText(context, "Request not found!", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        holder.pbRequest.setVisibility(View.GONE);
                        holder.btnCancelRequest.setVisibility(View.VISIBLE);
                        Toast.makeText(context, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
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