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
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.pm.appdev.duta.Common.Extras;
import com.pm.appdev.duta.Common.Util;
import com.pm.appdev.duta.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ChatListViewHolder> {

    private static final String TAG = "ChatListAdapter";

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

        boolean hasUnread = !Objects.equals(chatListModel.getUnreadCount(), "0");
        holder.tvUnreadCount.setVisibility(hasUnread ? View.VISIBLE : View.GONE);
        if (hasUnread) {
            holder.tvUnreadCount.setText(chatListModel.getUnreadCount());
        }

        bindProfilePhoto(holder.ivProfile, chatListModel.getPhotoName());

        holder.llChatList.setOnClickListener(view -> {
            if (chatListModel.getUserId() == null) {
                Log.e(TAG, "Cannot open chat: User ID is null");
                return;
            }

            Intent intent = new Intent(view.getContext(), ChatActivity.class);
            intent.putExtra(Extras.USER_KEY, chatListModel.getUserId());
            view.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return chatListModelList.size();
    }

    public void submitList(@NonNull List<ChatListModel> newItems) {
        List<ChatListModel> oldItems = new ArrayList<>(chatListModelList);
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffCallback(oldItems, newItems));
        chatListModelList.clear();
        chatListModelList.addAll(newItems);
        diffResult.dispatchUpdatesTo(this);
    }

    private void bindProfilePhoto(ImageView imageView, String photoBase64) {
        if (photoBase64 == null || photoBase64.isEmpty()) {
            imageView.setImageResource(R.drawable.default_profile);
            return;
        }

        try {
            byte[] decodedString = Base64.decode(photoBase64, Base64.DEFAULT);
            Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            imageView.setImageBitmap(decodedBitmap);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Failed to decode Base64 profile photo", e);
            imageView.setImageResource(R.drawable.default_profile);
        }
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

    private static class DiffCallback extends DiffUtil.Callback {
        private final List<ChatListModel> oldItems;
        private final List<ChatListModel> newItems;

        DiffCallback(List<ChatListModel> oldItems, List<ChatListModel> newItems) {
            this.oldItems = oldItems;
            this.newItems = newItems;
        }

        @Override
        public int getOldListSize() {
            return oldItems.size();
        }

        @Override
        public int getNewListSize() {
            return newItems.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return Objects.equals(oldItems.get(oldItemPosition).getUserId(),
                    newItems.get(newItemPosition).getUserId());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            ChatListModel oldItem = oldItems.get(oldItemPosition);
            ChatListModel newItem = newItems.get(newItemPosition);
            return Objects.equals(oldItem.getUserName(), newItem.getUserName())
                    && Objects.equals(oldItem.getPhotoName(), newItem.getPhotoName())
                    && Objects.equals(oldItem.getUnreadCount(), newItem.getUnreadCount())
                    && Objects.equals(oldItem.getLastMessage(), newItem.getLastMessage())
                    && oldItem.getLastMessageTime() == newItem.getLastMessageTime();
        }
    }
}
