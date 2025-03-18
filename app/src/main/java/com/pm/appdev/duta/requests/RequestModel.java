
package com.pm.appdev.duta.requests;

public class RequestModel {
    private String userId;
    private String userName;
    private String photoName;

    public RequestModel() {
        // Default constructor required for Firebase
    }

    public RequestModel(String userId, String userName, String photoName) {
        this.userId = userId;
        this.userName = userName;
        this.photoName = photoName;
    }

    public String getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public String getPhotoName() {
        return photoName;
    }
}
//package com.pm.appdev.duta.requests;
//
//public class RequestModel {
//    private String userId;
//    private String userName;
//    private String photoName;
//
//    // Default constructor required for Firebase
//    public RequestModel() {
//    }
//
//    public RequestModel(String userId, String userName, String photoName) {
//        this.userId = userId;
//        this.userName = userName;
//        this.photoName = photoName;
//    }
//
//    // Getters
//    public String getUserId() {
//        return userId;
//    }
//
//    public String getUserName() {
//        return userName;
//    }
//
//    public String getPhotoName() {
//        return photoName;
//    }
//
//    // Setters (Optional: Only if you need to modify values after object creation)
//    public void setUserId(String userId) {
//        this.userId = userId;
//    }
//
//    public void setUserName(String userName) {
//        this.userName = userName;
//    }
//
//    public void setPhotoName(String photoName) {
//        this.photoName = photoName;
//    }
//}


//package com.pm.appdev.duta.requests;
//
//
//public class RequestModel {
//    private String userName;
//    private String photoUrl;
//    private String userId;
//    private boolean requestReceived;
//
//    public RequestModel(String userId, String userName, String photoName) {
//        // Default constructor required for Firebase
//    }
//
//    public RequestModel(String userName, String photoUrl, String userId, boolean requestReceived) {
//        this.userName = userName;
//        this.photoUrl = photoUrl;
//        this.userId = userId;
//        this.requestReceived = requestReceived;
//    }
//
//    public String getUserName() {
//        return userName;
//    }
//
//    public String getPhotoUrl() {
//        return photoUrl;
//    }
//
//    public String getUserId() {
//        return userId;
//    }
//
//    public boolean isRequestReceived() {
//        return requestReceived;
//    }
//
//    public void setRequestReceived(boolean requestReceived) {
//        this.requestReceived = requestReceived;
//    }
//    public String getPhotoName() {  // ✅ FIX: Add this method
//        String photoName = null;
//        return photoName;
//    }
//}

//package com.pm.appdev.duta.requests;
//
//public class RequestModel {
//    private String userName;
//    private String photoUrl;
//    private String userId;
//
//    public RequestModel() {
//        // Default constructor required for Firebase
//    }
//
//    public RequestModel(String userName, String photoUrl, String userId) {
//        this.userName = userName;
//        this.photoUrl = photoUrl;
//        this.userId = userId;
//    }
//
//    public String getUserName() {
//        return userName;
//    }
//
//    public String getPhotoUrl() {
//        return photoUrl;
//    }
//
//    public String getUserId() {
//        return userId;
//    }
//}