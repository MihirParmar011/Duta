package com.pm.appdev.duta.requests;

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
import com.pm.appdev.duta.R;

import java.util.List;

public class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.RequestViewHolder> {

    private final Context context;
    private final List<RequestModel> requestModelList;
    private final DatabaseReference friendRequestsDatabase, friendsDatabase;
    private final FirebaseUser currentUser;

    public RequestAdapter(Context context, List<RequestModel> requestModelList) {
        this.context = context;
        this.requestModelList = requestModelList;
        this.currentUser = FirebaseAuth.getInstance().getCurrentUser();
        this.friendRequestsDatabase = FirebaseDatabase.getInstance().getReference("FriendRequests");
        this.friendsDatabase = FirebaseDatabase.getInstance().getReference("Friends");
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

        holder.tvFullName.setText(requestModel.getUserName());

        Glide.with(context)
                .load(requestModel.getPhotoName())
                .placeholder(R.drawable.default_profile)
                .into(holder.ivProfile);

        holder.btnAcceptRequest.setOnClickListener(view -> acceptRequest(requestModel, holder, position));
        holder.btnDenyRequest.setOnClickListener(view -> denyRequest(requestModel, holder, position));
    }

    private void acceptRequest(RequestModel requestModel, RequestViewHolder holder, int position) {
        String senderUsername = requestModel.getUserId();
        String currentUsername = currentUser.getDisplayName();

        Log.d("RequestAdapter", "Accepting request from: " + senderUsername);

        holder.pbDecision.setVisibility(View.VISIBLE);
        holder.btnDenyRequest.setVisibility(View.GONE);
        holder.btnAcceptRequest.setVisibility(View.GONE);

        friendsDatabase.child(currentUsername).child(senderUsername).setValue(true);
        friendsDatabase.child(senderUsername).child(currentUsername).setValue(true);

        friendRequestsDatabase.child(currentUsername).child(senderUsername).removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        friendRequestsDatabase.child(senderUsername).child(currentUsername).removeValue()
                                .addOnCompleteListener(task1 -> {
                                    if (task1.isSuccessful()) {
                                        requestModelList.remove(position);
                                        notifyItemRemoved(position);
                                        notifyItemRangeChanged(position, requestModelList.size());
                                        Toast.makeText(context, "Friend request accepted!", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                    holder.pbDecision.setVisibility(View.GONE);
                });
    }


    private void denyRequest(RequestModel requestModel, RequestViewHolder holder, int position) {
        String senderUsername = requestModel.getUserId();  // Using usernames
        String currentUsername = currentUser.getDisplayName();  // Using display name

        Log.d("RequestAdapter", "Denying request from: " + senderUsername);

        holder.pbDecision.setVisibility(View.VISIBLE);
        holder.btnDenyRequest.setVisibility(View.GONE);
        holder.btnAcceptRequest.setVisibility(View.GONE);

        // Remove request from FriendRequests node
        friendRequestsDatabase.child(currentUsername).child(senderUsername).removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        friendRequestsDatabase.child(senderUsername).child(currentUsername).removeValue()
                                .addOnCompleteListener(task1 -> {
                                    if (task1.isSuccessful()) {
                                        Log.d("RequestAdapter", "Request successfully denied and removed from database.");
                                        requestModelList.remove(position);
                                        notifyItemRemoved(position);
                                        notifyItemRangeChanged(position, requestModelList.size());
                                        Toast.makeText(context, "Friend request denied!", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Log.e("RequestAdapter", "Error removing request from sender's list: " + task1.getException());
                                    }
                                });
                    } else {
                        Log.e("RequestAdapter", "Error removing request: " + task.getException());
                    }
                    holder.pbDecision.setVisibility(View.GONE);
                });
    }

    @Override
    public int getItemCount() {
        return requestModelList.size();
    }

    public static class RequestViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivProfile;
        private final TextView tvFullName;
        private final Button btnAcceptRequest, btnDenyRequest;
        private final ProgressBar pbDecision;

        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProfile = itemView.findViewById(R.id.ivProfile);
            tvFullName = itemView.findViewById(R.id.tvFullName);
            btnAcceptRequest = itemView.findViewById(R.id.btnAcceptRequest);
            btnDenyRequest = itemView.findViewById(R.id.btnDenyRequest);
            pbDecision = itemView.findViewById(R.id.pbDecision);
        }
    }
}
