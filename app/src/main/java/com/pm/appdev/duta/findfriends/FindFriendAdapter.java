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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.pm.appdev.duta.R;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FindFriendAdapter extends RecyclerView.Adapter<FindFriendAdapter.FindFriendViewHolder> {

    private Context context;
    private List<FindFriendModel> findFriendModelList;
    private DatabaseReference friendRequestDatabase;
    private FirebaseUser currentUser;

    public FindFriendAdapter(Context context, List<FindFriendModel> findFriendModelList) {
        this.context = context;
        this.findFriendModelList = findFriendModelList;
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

        // Update Firebase FriendRequests node
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put(currentUser.getUid() + "/" + receiverUserId + "/requestType", "sent");
        requestMap.put(receiverUserId + "/" + currentUser.getUid() + "/requestType", "received");

        friendRequestDatabase.updateChildren(requestMap).addOnCompleteListener(task -> {
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
    }

    private void cancelFriendRequest(FindFriendModel friendModel, FindFriendViewHolder holder) {
        String receiverUserId = friendModel.getUserId();
        holder.btnCancelRequest.setVisibility(View.GONE);
        holder.pbRequest.setVisibility(View.VISIBLE);

        // Remove friend request from Firebase
        friendRequestDatabase.child(currentUser.getUid()).child(receiverUserId).removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        friendRequestDatabase.child(receiverUserId).child(currentUser.getUid()).removeValue()
                                .addOnCompleteListener(task1 -> {
                                    holder.pbRequest.setVisibility(View.GONE);
                                    if (task1.isSuccessful()) {
                                        Toast.makeText(context, "Friend request canceled!", Toast.LENGTH_SHORT).show();
                                        friendModel.setRequestSent(false);
                                        notifyDataSetChanged();
                                    } else {
                                        holder.btnCancelRequest.setVisibility(View.VISIBLE);
                                        Toast.makeText(context, "Failed to cancel request!", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        holder.btnCancelRequest.setVisibility(View.VISIBLE);
                        holder.pbRequest.setVisibility(View.GONE);
                        Toast.makeText(context, "Failed to cancel request!", Toast.LENGTH_SHORT).show();
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
