package com.pm.appdev.duta.requests;

public class RequestModel {
    private String requestId; // Unique ID for the request
    private String senderUid; // Firebase UID of the sender
    private String senderUserId; // Custom user ID of the sender (e.g., "Mihir1101")
    private String receiverUid; // Firebase UID of the receiver
    private String receiverUserId; // Custom user ID of the receiver (e.g., "Kano1101")
    private String status; // Status of the request (e.g., "pending", "accepted", "denied")
    private long timestamp; // Timestamp of the request

    // Constructor
    public RequestModel(String requestId, String senderUid, String senderUserId, String receiverUid, String receiverUserId, String status, long timestamp) {
        this.requestId = requestId;
        this.senderUid = senderUid;
        this.senderUserId = senderUserId;
        this.receiverUid = receiverUid;
        this.receiverUserId = receiverUserId;
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

    public String getSenderUid() {
        return senderUid;
    }

    public void setSenderUid(String senderUid) {
        this.senderUid = senderUid;
    }

    public String getSenderUserId() {
        return senderUserId;
    }

    public void setSenderUserId(String senderUserId) {
        this.senderUserId = senderUserId;
    }

    public String getReceiverUid() {
        return receiverUid;
    }

    public void setReceiverUid(String receiverUid) {
        this.receiverUid = receiverUid;
    }

    public String getReceiverUserId() {
        return receiverUserId;
    }

    public void setReceiverUserId(String receiverUserId) {
        this.receiverUserId = receiverUserId;
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
                ", senderUid='" + senderUid + '\'' +
                ", senderUserId='" + senderUserId + '\'' +
                ", receiverUid='" + receiverUid + '\'' +
                ", receiverUserId='" + receiverUserId + '\'' +
                ", status='" + status + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}