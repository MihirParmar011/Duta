package com.pm.appdev.duta.findfriends;

public class FindFriendModel {
    private String userName;
    private String photoName;
    private String userId; // Custom userId (e.g., "Mihir1101")
    private String uid; // Firebase UID
    private boolean requestSent;

    public FindFriendModel(String userName, String photoName, String userId, String uid, boolean requestSent) {
        this.userName = userName;
        this.photoName = photoName;
        this.userId = userId;
        this.uid = uid;
        this.requestSent = requestSent;
    }

    // Getters and setters
    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPhotoName() {
        return photoName;
    }

    public void setPhotoName(String photoName) {
        this.photoName = photoName;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public boolean isRequestSent() {
        return requestSent;
    }

    public void setRequestSent(boolean requestSent) {
        this.requestSent = requestSent;
    }
}