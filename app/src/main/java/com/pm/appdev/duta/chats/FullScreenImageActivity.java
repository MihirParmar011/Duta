package com.pm.appdev.duta.chats;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.pm.appdev.duta.Common.ImageRepository;
import com.pm.appdev.duta.R;

import java.io.File;
import java.io.IOException;

public class FullScreenImageActivity extends AppCompatActivity {

    private ImageView ivFullScreen;
    private ProgressBar pbLoading;
    private TextView tvError;
    private TextView tvImageInfo;
    private boolean isFavorite = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_screen_image);

        initializeViews();
        setupToolbar();
        loadImage();
    }

    private void initializeViews() {
        ivFullScreen = findViewById(R.id.ivFullScreen);
        pbLoading = findViewById(R.id.pbLoading);
        tvError = findViewById(R.id.tvError);
        tvImageInfo = findViewById(R.id.tvImageInfo);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        ImageButton btnShare = findViewById(R.id.btnShare);
        btnShare.setOnClickListener(v -> shareImage());

        ImageButton btnFavorite = findViewById(R.id.btnFavorite);
        btnFavorite.setOnClickListener(v -> toggleFavorite());

        ImageButton btnDownload = findViewById(R.id.btnDownload);
        btnDownload.setOnClickListener(v -> downloadImage());
    }

    private void loadImage() {
        // First try to get from path
        String imagePath = getIntent().getStringExtra("image_path");
        if (imagePath != null) {
            loadImageFromPath(imagePath);
            return;
        }

        // Then try from repository
        Bitmap repositoryBitmap = ImageRepository.getCurrentImage();
        if (repositoryBitmap != null && !repositoryBitmap.isRecycled()) {
            displayImage(repositoryBitmap);
            return;
        }

        // Fallback to other methods if needed
        showError("No image data found");
    }

    private void loadImageFromPath(String imagePath) {
        new Thread(() -> {
            try {
                File imageFile = new File(imagePath);
                if (imageFile.exists()) {
                    Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
                    if (bitmap != null) {
                        ImageRepository.setCurrentImage(bitmap);
                        displayImage(bitmap);
                        return;
                    }
                }
                showError("Failed to load image");
            } catch (Exception e) {
                showError("Failed to load image");
            }
        }).start();
    }

    private void displayImage(Bitmap bitmap) {
        runOnUiThread(() -> {
            if (bitmap != null && !bitmap.isRecycled()) {
                ivFullScreen.setImageBitmap(bitmap);
                pbLoading.setVisibility(View.GONE);
                tvImageInfo.setText("Image loaded");
            } else {
                showError("Invalid image data");
            }
        });
    }

    private void showError(String message) {
        runOnUiThread(() -> {
            ivFullScreen.setImageResource(android.R.drawable.ic_menu_report_image);
            pbLoading.setVisibility(View.GONE);
            tvError.setVisibility(View.VISIBLE);
            tvError.setText(message);
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        });
    }

    private void toggleFavorite() {
        isFavorite = !isFavorite;
        ImageButton btnFavorite = findViewById(R.id.btnFavorite);
        btnFavorite.setImageResource(isFavorite ?
                android.R.drawable.btn_star_big_on :
                android.R.drawable.btn_star_big_off);
        Toast.makeText(this,
                isFavorite ? "Added to favorites" : "Removed from favorites",
                Toast.LENGTH_SHORT).show();
    }

    private void shareImage() {
        Bitmap bitmap = ImageRepository.getCurrentImage();
        if (bitmap == null || bitmap.isRecycled()) {
            Toast.makeText(this, "No image to share", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String path = MediaStore.Images.Media.insertImage(
                    getContentResolver(),
                    bitmap,
                    "Shared Image",
                    null
            );
            Uri imageUri = Uri.parse(path);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/*");
            shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
            startActivity(Intent.createChooser(shareIntent, "Share Image"));
        } catch (Exception e) {
            Toast.makeText(this, "Failed to share image", Toast.LENGTH_SHORT).show();
        }
    }

    private void downloadImage() {
        String imagePath = ImageRepository.getCurrentImagePath();
        if (imagePath != null) {
            File imageFile = new File(imagePath);
            if (imageFile.exists()) {
                try {
                    MediaStore.Images.Media.insertImage(
                            getContentResolver(),
                            imagePath,
                            imageFile.getName(),
                            "Downloaded image"
                    );
                    Toast.makeText(this, "Image saved to gallery", Toast.LENGTH_SHORT).show();
                    return;
                } catch (Exception e) {
                    // Continue to try with bitmap
                }
            }
        }

        Bitmap bitmap = ImageRepository.getCurrentImage();
        if (bitmap == null || bitmap.isRecycled()) {
            Toast.makeText(this, "No image to download", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            MediaStore.Images.Media.insertImage(
                    getContentResolver(),
                    bitmap,
                    "image_" + System.currentTimeMillis(),
                    "Downloaded image"
            );
            Toast.makeText(this, "Image saved to gallery", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ImageRepository.clear();
    }
}

//package com.pm.appdev.duta.chats;
//
//import android.graphics.Bitmap;
//import android.graphics.BitmapFactory;
//import android.net.Uri;
//import android.os.Bundle;
//import android.util.Base64;
//import android.view.View;
//import android.widget.ImageView;
//import android.widget.ProgressBar;
//import android.widget.Toast;
//
//import androidx.appcompat.app.AppCompatActivity;
//
//import com.pm.appdev.duta.R;
//import com.pm.appdev.duta.Common.ImageRepository;
//
//import java.io.InputStream;
//
//public class FullScreenImageActivity extends AppCompatActivity {
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_full_screen_image);
//
//        ImageView ivFullScreen = findViewById(R.id.ivFullScreen);
//        ProgressBar pbLoading = findViewById(R.id.pbLoading);
//
//        // Set initial placeholder and show loading
//        ivFullScreen.setImageResource(R.drawable.ic_image_placeholder);
//        pbLoading.setVisibility(View.VISIBLE);
//
//        // First check if image is available in repository
//        Bitmap repositoryBitmap = ImageRepository.getCurrentImage();
//        if (repositoryBitmap != null && !repositoryBitmap.isRecycled()) {
//            ivFullScreen.setImageBitmap(repositoryBitmap);
//            pbLoading.setVisibility(View.GONE);
//            return;
//        }
//
//        // If not in repository, check for URI (FileProvider approach)
//        Uri imageUri = getIntent().getParcelableExtra("image_uri");
//        if (imageUri != null) {
//            loadImageFromUri(ivFullScreen, pbLoading, imageUri);
//        }
//        // Fall back to Base64 string (legacy approach)
//        else {
//            String imageBase64 = getIntent().getStringExtra("image");
//            if (imageBase64 == null || imageBase64.isEmpty()) {
//                showError(ivFullScreen, pbLoading, "No image data found");
//                return;
//            }
//            loadImageFromBase64(ivFullScreen, pbLoading, imageBase64);
//        }
//    }
//
//    private void loadImageFromUri(ImageView imageView, ProgressBar progressBar, Uri uri) {
//        new Thread(() -> {
//            try (InputStream in = getContentResolver().openInputStream(uri)) {
//                Bitmap bitmap = BitmapFactory.decodeStream(in);
//
//                if (bitmap == null) {
//                    throw new Exception("Failed to decode image");
//                }
//
//                runOnUiThread(() -> {
//                    imageView.setImageBitmap(bitmap);
//                    progressBar.setVisibility(View.GONE);
//                    // Store in repository for any subsequent needs
//                    ImageRepository.setCurrentImage(bitmap);
//                });
//            } catch (Exception e) {
//                runOnUiThread(() -> showError(imageView, progressBar, "Failed to load image"));
//            }
//        }).start();
//    }
//
//    private void loadImageFromBase64(ImageView imageView, ProgressBar progressBar, String imageBase64) {
//        new Thread(() -> {
//            try {
//                byte[] decodedString = Base64.decode(imageBase64, Base64.DEFAULT);
//                Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
//
//                if (decodedBitmap == null) {
//                    throw new Exception("Failed to decode Base64 image");
//                }
//
//                runOnUiThread(() -> {
//                    imageView.setImageBitmap(decodedBitmap);
//                    progressBar.setVisibility(View.GONE);
//                    // Store in repository for any subsequent needs
//                    ImageRepository.setCurrentImage(decodedBitmap);
//                });
//            } catch (Exception e) {
//                runOnUiThread(() -> showError(imageView, progressBar, "Failed to decode image"));
//            }
//        }).start();
//    }
//
//    private void showError(ImageView imageView, ProgressBar progressBar, String errorMessage) {
//        runOnUiThread(() -> {
//            imageView.setImageResource(R.drawable.ic_broken_image);
//            progressBar.setVisibility(View.GONE);
//            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
//
//            // Close activity after short delay
//            imageView.postDelayed(this::finish, 1500);
//        });
//    }
//
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        // Clear the repository when activity is destroyed
//        ImageRepository.clear();
//    }
//}