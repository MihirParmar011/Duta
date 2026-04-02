package com.pm.appdev.duta.notifications;

import androidx.annotation.NonNull;

public class NotificationSnapshot {
    private final int unreadChatCount;
    private final int pendingRequestCount;
    private final boolean stale;

    public NotificationSnapshot(int unreadChatCount, int pendingRequestCount, boolean stale) {
        this.unreadChatCount = Math.max(0, unreadChatCount);
        this.pendingRequestCount = Math.max(0, pendingRequestCount);
        this.stale = stale;
    }

    public int getUnreadChatCount() {
        return unreadChatCount;
    }

    public int getPendingRequestCount() {
        return pendingRequestCount;
    }

    public boolean isStale() {
        return stale;
    }

    public int getTotalCount() {
        return unreadChatCount + pendingRequestCount;
    }

    @NonNull
    @Override
    public String toString() {
        return "NotificationSnapshot{" +
                "unreadChatCount=" + unreadChatCount +
                ", pendingRequestCount=" + pendingRequestCount +
                ", stale=" + stale +
                '}';
    }
}
