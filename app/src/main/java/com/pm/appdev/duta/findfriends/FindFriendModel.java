// FindFriendModel.java
package com.pm.appdev.duta.findfriends;
public class FindFriendModel {
    private String userName;
    private String photoUrl;
    private String userId;
    private boolean requestSent;

    public FindFriendModel() {

    }

    public FindFriendModel(String userName, String photoUrl, String userId, boolean requestSent) {
        this.userName = userName;
        this.photoUrl = photoUrl;
        this.userId = userId;
        this.requestSent = requestSent;
    }

    public String getUserName() {
        return userName;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public String getUserId() {
        return userId;
    }

    public boolean isRequestSent() {
        return requestSent;
    }
}
