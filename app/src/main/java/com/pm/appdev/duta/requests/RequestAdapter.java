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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.pm.appdev.duta.R;

import java.util.List;
public class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.RequestViewHolder> {

    private final Context context;
    private final List<RequestModel> requestModelList;
    private final DatabaseReference friendRequestsDatabase;
    private final DatabaseReference chatsDatabase;
    private final FirebaseUser currentUser;

    public RequestAdapter(Context context, List<RequestModel> requestModelList) {
        this.context = context;
        this.requestModelList = requestModelList;
        this.friendRequestsDatabase = FirebaseDatabase.getInstance().getReference("FriendRequests");
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

        // Set user name
        if (requestModel.getUserName() != null && !requestModel.getUserName().isEmpty()) {
            holder.tvFullName.setText(requestModel.getUserName()); // Bind the name to the TextView
        } else {
            holder.tvFullName.setText("Unknown User"); // Fallback if name is not available
        }

        // Load profile picture using Glide (if available)
        if (requestModel.getPhotoName() != null && !requestModel.getPhotoName().isEmpty()) {
            StorageReference fileRef = FirebaseStorage.getInstance().getReference()
                    .child("images/" + requestModel.getPhotoName());

            fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                Glide.with(context)
                        .load(uri)
                        .placeholder(R.drawable.default_profile)
                        .error(R.drawable.default_profile)
                        .into(holder.ivProfile);
            }).addOnFailureListener(e -> {
                Log.e("RequestAdapter", "Failed to load profile picture: " + e.getMessage());
                holder.ivProfile.setImageResource(R.drawable.default_profile);
            });
        } else {
            holder.ivProfile.setImageResource(R.drawable.default_profile);
        }

        // Handle accept and deny buttons
        holder.btnAcceptRequest.setOnClickListener(v -> handleRequest(requestModel, holder, true));
        holder.btnDenyRequest.setOnClickListener(v -> handleRequest(requestModel, holder, false));
    }

    private void handleRequest(RequestModel requestModel, RequestViewHolder holder, boolean isAccepted) {
        holder.pbDecision.setVisibility(View.VISIBLE);
        holder.btnAcceptRequest.setVisibility(View.GONE);
        holder.btnDenyRequest.setVisibility(View.GONE);

        String requestId = requestModel.getRequestId(); // Use the requestId from RequestModel
        String requestType = isAccepted ? "accepted" : "denied"; // Update status to "accepted" or "denied"

        // Reference to the specific request node in FriendRequests
        DatabaseReference requestRef = friendRequestsDatabase.child(requestId);

        // Update the status in the existing request node
        requestRef.child("status").setValue(requestType)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (isAccepted) {
                            // Add the user to the Chats node after accepting the request
                            addUserToChats(requestModel.getUserId(), holder);
                        } else {
                            // If denied, just show a toast and update the UI
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

    // Add the user to the Chats node after accepting the request
    private void addUserToChats(String senderUserId, RequestViewHolder holder) {
        // Add the sender to the current user's chats
        chatsDatabase.child(currentUser.getUid()).child(senderUserId)
                .child("timestamp").setValue(System.currentTimeMillis())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Add the current user to the sender's chats
                        chatsDatabase.child(senderUserId).child(currentUser.getUid())
                                .child("timestamp").setValue(System.currentTimeMillis())
                                .addOnCompleteListener(task1 -> {
                                    if (task1.isSuccessful()) {
                                        Toast.makeText(context, "Request accepted", Toast.LENGTH_SHORT).show();
                                        updateUIAfterRequest(holder);
                                    } else {
                                        Toast.makeText(context, "Failed to add to chats", Toast.LENGTH_SHORT).show();
                                        updateUIAfterRequest(holder);
                                    }
                                });
                    } else {
                        Toast.makeText(context, "Failed to add to chats", Toast.LENGTH_SHORT).show();
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