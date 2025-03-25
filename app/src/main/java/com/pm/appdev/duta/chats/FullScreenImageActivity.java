package com.pm.appdev.duta.chats;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.appbar.MaterialToolbar;
import com.pm.appdev.duta.R;
import com.pm.appdev.duta.Common.ImageRepository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FullScreenImageActivity extends AppCompatActivity {

    private ImageView ivFullScreen;
    private ProgressBar pbLoading;
    private TextView tvError;
    private TextView tvImageInfo;
    private boolean isFavorite = false;
    private Bitmap currentBitmap; // Keep strong reference

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_screen_image);

        // Initialize views
        ivFullScreen = findViewById(R.id.ivFullScreen);
        pbLoading = findViewById(R.id.pbLoading);
        tvError = findViewById(R.id.tvError);
        tvImageInfo = findViewById(R.id.tvImageInfo);

        // Set up toolbar and buttons
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

        // Initial state
        ivFullScreen.setImageResource(android.R.drawable.ic_menu_gallery);
        pbLoading.setVisibility(View.VISIBLE);
        tvError.setVisibility(View.GONE);

        // Load image
        loadImage();
    }

    private void loadImage() {
        // First try to get from repository
        Bitmap repositoryBitmap = ImageRepository.getCurrentImage();
        if (repositoryBitmap != null && !repositoryBitmap.isRecycled()) {
            currentBitmap = repositoryBitmap;
            displayImage(currentBitmap);
            return;
        }

        // Then try from intent extras
        Uri imageUri = getIntent().getParcelableExtra("image_uri");
        if (imageUri != null) {
            loadImageFromUri(imageUri);
        } else {
            String imageBase64 = getIntent().getStringExtra("image");
            if (imageBase64 == null || imageBase64.isEmpty()) {
                showError("No image data found");
                return;
            }
            loadImageFromBase64(imageBase64);
        }
    }

    private void loadImageFromUri(Uri uri) {
        new Thread(() -> {
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                Bitmap bitmap = BitmapFactory.decodeStream(in);
                if (bitmap != null) {
                    currentBitmap = bitmap;
                    ImageRepository.setCurrentImage(bitmap); // Store in repository
                    displayImage(bitmap);
                } else {
                    showError("Failed to decode image");
                }
            } catch (Exception e) {
                showError("Failed to load image");
            }
        }).start();
    }

    private void loadImageFromBase64(String imageBase64) {
        new Thread(() -> {
            try {
                byte[] decodedString = Base64.decode(imageBase64, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                if (bitmap != null) {
                    currentBitmap = bitmap;
                    ImageRepository.setCurrentImage(bitmap); // Store in repository
                    displayImage(bitmap);
                } else {
                    showError("Failed to decode image");
                }
            } catch (Exception e) {
                showError("Failed to decode image");
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
        if (currentBitmap == null || currentBitmap.isRecycled()) {
            Toast.makeText(this, "No image to share", Toast.LENGTH_SHORT).show();
            return;
        }

        String path = MediaStore.Images.Media.insertImage(
                getContentResolver(),
                currentBitmap,
                "Shared Image",
                null
        );
        Uri imageUri = Uri.parse(path);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("image/*");
        shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
        startActivity(Intent.createChooser(shareIntent, "Share Image"));
    }

    private void downloadImage() {
        if (currentBitmap == null || currentBitmap.isRecycled()) {
            Toast.makeText(this, "No image to download", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String fileName = "image_" + System.currentTimeMillis() + ".jpg";
            MediaStore.Images.Media.insertImage(
                    getContentResolver(),
                    currentBitmap,
                    fileName,
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
        // Clear current reference but let ImageRepository handle its own recycling
        currentBitmap = null;
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