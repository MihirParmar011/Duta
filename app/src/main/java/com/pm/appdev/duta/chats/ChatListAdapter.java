package com.pm.appdev.duta.chats;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pm.appdev.duta.R;
import com.pm.appdev.duta.Common.Extras;
import com.pm.appdev.duta.Common.Util;
import java.util.List;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ChatListViewHolder> {

    private final Context context;
    private final List<ChatListModel> chatListModelList;

    public ChatListAdapter(Context context, List<ChatListModel> chatListModelList) {
        this.context = context;
        this.chatListModelList = chatListModelList;
    }

    @NonNull
    @Override
    public ChatListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.chat_list_layout, parent, false);
        return new ChatListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ChatListViewHolder holder, int position) {
        final ChatListModel chatListModel = chatListModelList.get(position);

        holder.tvFullName.setText(chatListModel.getUserName());
        holder.tvLastMessage.setText(chatListModel.getLastMessage());
        holder.tvLastMessageTime.setText(Util.getTimeAgo(chatListModel.getLastMessageTime()));

        if (!chatListModel.getUnreadCount().equals("0")) {
            holder.tvUnreadCount.setVisibility(View.VISIBLE);
            holder.tvUnreadCount.setText(chatListModel.getUnreadCount());
        } else {
            holder.tvUnreadCount.setVisibility(View.GONE);
        }

        // Decode Base64 photo and set to ImageView
        if (chatListModel.getPhotoName() != null && !chatListModel.getPhotoName().isEmpty()) {
            try {
                byte[] decodedString = Base64.decode(chatListModel.getPhotoName(), Base64.DEFAULT);
                Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                holder.ivProfile.setImageBitmap(decodedBitmap);
            } catch (IllegalArgumentException e) {
                Log.e("ChatListAdapter", "Failed to decode Base64 photo: " + e.getMessage());
                holder.ivProfile.setImageResource(R.drawable.default_profile);
            }
        } else {
            holder.ivProfile.setImageResource(R.drawable.default_profile);
        }

        holder.llChatList.setOnClickListener(view -> {
            if (chatListModel.getUserId() == null) {
                Log.e("ChatListAdapter", "Error: User ID is null!");
                return;  // Stop execution
            }

            // Pass only the userId to ChatActivity
            Intent intent = new Intent(view.getContext(), ChatActivity.class);
            intent.putExtra(Extras.USER_KEY, chatListModel.getUserId());
            view.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return chatListModelList.size();
    }

    public static class ChatListViewHolder extends RecyclerView.ViewHolder {

        private final LinearLayout llChatList;
        private final TextView tvFullName;
        private final TextView tvLastMessage;
        private final TextView tvLastMessageTime;
        private final TextView tvUnreadCount;
        private final ImageView ivProfile;

        public ChatListViewHolder(@NonNull View itemView) {
            super(itemView);
            llChatList = itemView.findViewById(R.id.llChatList);
            tvFullName = itemView.findViewById(R.id.tvFullName);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvLastMessageTime = itemView.findViewById(R.id.tvLastMessageTime);
            tvUnreadCount = itemView.findViewById(R.id.tvUnreadCount);
            ivProfile = itemView.findViewById(R.id.ivProfile);
        }
    }
}
