package com.pm.appdev.duta.requests;
public class RequestModel {
    private String requestId; // Add this field
    private String userId;
    private String userName;
    private String photoName;
    private String status;
    private long timestamp;

    // Constructor
    public RequestModel(String requestId, String userId, String userName, String photoName, String status, long timestamp) {
        this.requestId = requestId;
        this.userId = userId;
        this.userName = userName;
        this.photoName = photoName;
        this.status = status;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    @Override
    public String toString() {
        return "RequestModel{" +
                "requestId='" + requestId + '\'' +
                ", userId='" + userId + '\'' +
                ", userName='" + userName + '\'' +
                ", photoName='" + photoName + '\'' +
                ", status='" + status + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }

}
