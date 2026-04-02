package com.pm.appdev.duta.chats;

import androidx.annotation.NonNull;

import java.util.Objects;

public class MessageModel {
    private String message;
    private String messageFrom;
    private String messageId;
    private long messageTime;
    private String messageType;
    private String messageStatus;

    public MessageModel() {
    }

    public MessageModel(String message, String messageFrom, String messageId, long messageTime,
                        String messageType, String messageStatus) {
        this.message = message;
        this.messageFrom = messageFrom;
        this.messageId = messageId;
        this.messageTime = messageTime;
        this.messageType = messageType;
        this.messageStatus = messageStatus;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessageFrom() {
        return messageFrom;
    }

    public void setMessageFrom(String messageFrom) {
        this.messageFrom = messageFrom;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public long getMessageTime() {
        return messageTime;
    }

    public void setMessageTime(long messageTime) {
        this.messageTime = messageTime;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getMessageStatus() {
        return messageStatus;
    }

    public void setMessageStatus(String messageStatus) {
        this.messageStatus = messageStatus;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MessageModel)) return false;
        MessageModel that = (MessageModel) o;
        return messageTime == that.messageTime
                && Objects.equals(message, that.message)
                && Objects.equals(messageFrom, that.messageFrom)
                && Objects.equals(messageId, that.messageId)
                && Objects.equals(messageType, that.messageType)
                && Objects.equals(messageStatus, that.messageStatus);
    }

    @Override
    public int hashCode() {
        return Objects.hash(message, messageFrom, messageId, messageTime, messageType, messageStatus);
    }

    @NonNull
    @Override
    public String toString() {
        return "MessageModel{" +
                "messageId='" + messageId + '\'' +
                ", messageType='" + messageType + '\'' +
                ", messageStatus='" + messageStatus + '\'' +
                '}';
    }
}
