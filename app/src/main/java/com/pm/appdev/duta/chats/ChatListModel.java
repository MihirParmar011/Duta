package com.pm.appdev.duta.chats;

public class ChatListModel {
    private String userId;
    private String userName;
    private String photoName;
    private String unreadCount;
    private String lastMessage;
    private long lastMessageTime;  // Change from String to long

    public ChatListModel(String userId, String userName, String photoName, String unreadCount, String lastMessage, long lastMessageTime) {
        this.userId = userId;
        this.userName = userName;
        this.photoName = photoName;
        this.unreadCount = unreadCount;
        this.lastMessage = lastMessage;
        this.lastMessageTime = lastMessageTime;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getPhotoName() { return photoName; }
    public void setPhotoName(String photoName) { this.photoName = photoName; }

    public String getUnreadCount() { return unreadCount; }
    public void setUnreadCount(String unreadCount) { this.unreadCount = unreadCount; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public long getLastMessageTime() { return lastMessageTime; } // Updated
    public void setLastMessageTime(long lastMessageTime) { this.lastMessageTime = lastMessageTime; } // Updated
}