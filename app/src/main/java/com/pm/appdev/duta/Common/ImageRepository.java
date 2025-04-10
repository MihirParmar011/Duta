package com.pm.appdev.duta.Common;

import android.graphics.Bitmap;

public class ImageRepository {
    private static Bitmap currentImage;
    private static String currentImagePath;

    public static void setCurrentImage(Bitmap image) {
        // Clear previous image if exists
        if (currentImage != null && !currentImage.isRecycled()) {
            currentImage.recycle();
        }
        currentImage = image;
    }

    public static void setCurrentImagePath(String path) {
        currentImagePath = path;
    }

    public static Bitmap getCurrentImage() {
        return currentImage;
    }

    public static String getCurrentImagePath() {
        return currentImagePath;
    }

    public static void clear() {
        if (currentImage != null && !currentImage.isRecycled()) {
            currentImage.recycle();
        }
        currentImage = null;
        currentImagePath = null;
    }
}

//package com.pm.appdev.duta.Common;
//
//import android.graphics.Bitmap;
//
//public class ImageRepository {
//    private static Bitmap currentImage;
//
//    public static void setCurrentImage(Bitmap image) {
//        // Clear previous image if exists
//        if (currentImage != null && !currentImage.isRecycled()) {
//            currentImage.recycle();
//        }
//        currentImage = image;
//    }
//
//    public static Bitmap getCurrentImage() {
//        return currentImage;
//    }
//
//    public static void clear() {
//        if (currentImage != null && !currentImage.isRecycled()) {
//            currentImage.recycle();
//        }
//        currentImage = null;
//    }
//}