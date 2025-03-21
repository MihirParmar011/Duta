package com.pm.appdev.duta.requests;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
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

import java.util.List;

public class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.RequestViewHolder> {

    private final Context context;
    private final List<RequestModel> requestModelList;
    private final DatabaseReference friendRequestsDatabase;
    private final DatabaseReference usersDatabase;
    private final DatabaseReference chatsDatabase;
    private final FirebaseUser currentUser;

    public RequestAdapter(Context context, List<RequestModel> requestModelList) {
        this.context = context;
        this.requestModelList = requestModelList;
        this.friendRequestsDatabase = FirebaseDatabase.getInstance().getReference("FriendRequests");
        this.usersDatabase = FirebaseDatabase.getInstance().getReference("Users");
        this.chatsDatabase = FirebaseDatabase.getInstance().getReference("Chats");
        this.currentUser = FirebaseAuth.getInstance().getCurrentUser();
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.friend_request_layout, parent, false);
        return new RequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final RequestViewHolder holder, int position) {
        final RequestModel requestModel = requestModelList.get(position);
        String requestId = requestModel.getRequestId();

        // Fetch sender details using senderUserId
        friendRequestsDatabase.child(requestId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot requestSnapshot) {
                if (requestSnapshot.exists()) {
                    String senderUserId = requestSnapshot.child("senderUserId").getValue(String.class);

                    if (senderUserId != null && !senderUserId.isEmpty()) {
                        fetchUserDetails(senderUserId, holder);
                    } else {
                        setUnknownUser(holder);
                    }
                } else {
                    setUnknownUser(holder);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                setUnknownUser(holder);
            }
        });

        // Handle accept and deny buttons
        holder.btnAcceptRequest.setOnClickListener(v -> handleRequest(requestModel, holder, true));
        holder.btnDenyRequest.setOnClickListener(v -> handleRequest(requestModel, holder, false));
    }

    private void fetchUserDetails(String userId, RequestViewHolder holder) {
        usersDatabase.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                if (userSnapshot.exists()) {
                    String userName = userSnapshot.child("name").getValue(String.class);
                    String photoUrl = userSnapshot.child("photo").getValue(String.class);

                    if (userName != null && !userName.isEmpty()) {
                        holder.tvFullName.setText(userName);
                    } else {
                        holder.tvFullName.setText("Unknown User");
                    }

                    if (photoUrl != null && !photoUrl.isEmpty()) {
                        Glide.with(context)
                                .load(photoUrl)
                                .placeholder(R.drawable.default_profile)
                                .error(R.drawable.default_profile)
                                .into(holder.ivProfile);
                    } else {
                        holder.ivProfile.setImageResource(R.drawable.default_profile);
                    }
                } else {
                    setUnknownUser(holder);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                setUnknownUser(holder);
            }
        });
    }

    private void setUnknownUser(RequestViewHolder holder) {
        holder.tvFullName.setText("Unknown User");
        holder.ivProfile.setImageResource(R.drawable.default_profile);
    }

    private void handleRequest(RequestModel requestModel, RequestViewHolder holder, boolean isAccepted) {
        holder.pbDecision.setVisibility(View.VISIBLE);
        holder.btnAcceptRequest.setVisibility(View.GONE);
        holder.btnDenyRequest.setVisibility(View.GONE);

        String requestId = requestModel.getRequestId();
        String requestType = isAccepted ? "accepted" : "denied";

        friendRequestsDatabase.child(requestId).child("status").setValue(requestType)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (isAccepted) {
                            addUserToChats(requestModel.getSenderUid(), requestModel.getSenderUserId(), holder);
                        } else {
                            Toast.makeText(context, "Request denied", Toast.LENGTH_SHORT).show();
                            updateUIAfterRequest(holder);
                        }
                    } else {
                        Toast.makeText(context, "Failed to update request", Toast.LENGTH_SHORT).show();
                        updateUIAfterRequest(holder);
                    }
                });
    }

    @SuppressLint("NotifyDataSetChanged")
    private void updateUIAfterRequest(RequestViewHolder holder) {
        holder.pbDecision.setVisibility(View.GONE);
        holder.btnAcceptRequest.setVisibility(View.VISIBLE);
        holder.btnDenyRequest.setVisibility(View.VISIBLE);

        int position = holder.getAdapterPosition();
        if (position != RecyclerView.NO_POSITION && position < requestModelList.size()) {
            synchronized (requestModelList) {
                requestModelList.remove(position);
                notifyItemRemoved(position);
            }
        } else {
            Log.e("RequestAdapter", "Invalid adapter position: " + position);
        }

        if (requestModelList.isEmpty()) {
            notifyDataSetChanged();
        }
    }

    private void addUserToChats(String senderUid, String senderUserId, RequestViewHolder holder) {
        if (currentUser == null) {
            Toast.makeText(context, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUid = currentUser.getUid();

        // Find the currentUserId (Receiver's userId) by searching in the Users node
        usersDatabase.orderByChild("uid").equalTo(currentUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String currentUserId = null;

                    // Find the correct userId key from the database
                    for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                        currentUserId = userSnapshot.getKey();  // Gets "Kano1101" or "Mihir1101"
                        break;
                    }

                    if (currentUserId == null || senderUserId == null) {
                        Toast.makeText(context, "User data is missing", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    DatabaseReference chatRefCurrentUser = chatsDatabase.child(currentUid).child(senderUid);
                    DatabaseReference chatRefSender = chatsDatabase.child(senderUid).child(currentUid);

                    long timestamp = System.currentTimeMillis();

                    // Store chat details for both users
                    chatRefCurrentUser.child("timestamp").setValue(timestamp);
                    chatRefCurrentUser.child("userId").setValue(senderUserId); // Sender's userId
                    chatRefCurrentUser.child("receiverUserId").setValue(currentUserId); // Receiver's userId

                    chatRefSender.child("timestamp").setValue(timestamp);
                    chatRefSender.child("userId").setValue(currentUserId); // Receiver's userId
                    chatRefSender.child("receiverUserId").setValue(senderUserId); // Sender's userId

                    chatRefSender.child("timestamp").setValue(timestamp).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(context, "Request accepted", Toast.LENGTH_SHORT).show();
                            updateUIAfterRequest(holder);
                        } else {
                            Toast.makeText(context, "Failed to add to chats", Toast.LENGTH_SHORT).show();
                            updateUIAfterRequest(holder);
                        }
                    });

                } else {
                    Toast.makeText(context, "Failed to fetch user details", Toast.LENGTH_SHORT).show();
                    updateUIAfterRequest(holder);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(context, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                updateUIAfterRequest(holder);
            }
        });
    }

    @Override
    public int getItemCount() {
        return requestModelList.size();
    }

    public static class RequestViewHolder extends RecyclerView.ViewHolder {
        TextView tvFullName;
        ImageView ivProfile;
        Button btnAcceptRequest, btnDenyRequest;
        ProgressBar pbDecision;

        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFullName = itemView.findViewById(R.id.tvFullName);
            ivProfile = itemView.findViewById(R.id.ivProfile);
            btnAcceptRequest = itemView.findViewById(R.id.btnAcceptRequest);
            btnDenyRequest = itemView.findViewById(R.id.btnDenyRequest);
            pbDecision = itemView.findViewById(R.id.pbDecision);
        }
    }
}