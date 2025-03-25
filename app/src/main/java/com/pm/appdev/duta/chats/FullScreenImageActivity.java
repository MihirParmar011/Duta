package com.pm.appdev.duta.chats;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.pm.appdev.duta.R;
import com.pm.appdev.duta.Common.ImageRepository;

import java.io.InputStream;

public class FullScreenImageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_screen_image);

        ImageView ivFullScreen = findViewById(R.id.ivFullScreen);
        ProgressBar pbLoading = findViewById(R.id.pbLoading);

        // Set initial placeholder and show loading
        ivFullScreen.setImageResource(R.drawable.ic_image_placeholder);
        pbLoading.setVisibility(View.VISIBLE);

        // First check if image is available in repository
        Bitmap repositoryBitmap = ImageRepository.getCurrentImage();
        if (repositoryBitmap != null && !repositoryBitmap.isRecycled()) {
            ivFullScreen.setImageBitmap(repositoryBitmap);
            pbLoading.setVisibility(View.GONE);
            return;
        }

        // If not in repository, check for URI (FileProvider approach)
        Uri imageUri = getIntent().getParcelableExtra("image_uri");
        if (imageUri != null) {
            loadImageFromUri(ivFullScreen, pbLoading, imageUri);
        }
        // Fall back to Base64 string (legacy approach)
        else {
            String imageBase64 = getIntent().getStringExtra("image");
            if (imageBase64 == null || imageBase64.isEmpty()) {
                showError(ivFullScreen, pbLoading, "No image data found");
                return;
            }
            loadImageFromBase64(ivFullScreen, pbLoading, imageBase64);
        }
    }

    private void loadImageFromUri(ImageView imageView, ProgressBar progressBar, Uri uri) {
        new Thread(() -> {
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                Bitmap bitmap = BitmapFactory.decodeStream(in);

                if (bitmap == null) {
                    throw new Exception("Failed to decode image");
                }

                runOnUiThread(() -> {
                    imageView.setImageBitmap(bitmap);
                    progressBar.setVisibility(View.GONE);
                    // Store in repository for any subsequent needs
                    ImageRepository.setCurrentImage(bitmap);
                });
            } catch (Exception e) {
                runOnUiThread(() -> showError(imageView, progressBar, "Failed to load image"));
            }
        }).start();
    }

    private void loadImageFromBase64(ImageView imageView, ProgressBar progressBar, String imageBase64) {
        new Thread(() -> {
            try {
                byte[] decodedString = Base64.decode(imageBase64, Base64.DEFAULT);
                Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                if (decodedBitmap == null) {
                    throw new Exception("Failed to decode Base64 image");
                }

                runOnUiThread(() -> {
                    imageView.setImageBitmap(decodedBitmap);
                    progressBar.setVisibility(View.GONE);
                    // Store in repository for any subsequent needs
                    ImageRepository.setCurrentImage(decodedBitmap);
                });
            } catch (Exception e) {
                runOnUiThread(() -> showError(imageView, progressBar, "Failed to decode image"));
            }
        }).start();
    }

    private void showError(ImageView imageView, ProgressBar progressBar, String errorMessage) {
        runOnUiThread(() -> {
            imageView.setImageResource(R.drawable.ic_broken_image);
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();

            // Close activity after short delay
            imageView.postDelayed(this::finish, 1500);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clear the repository when activity is destroyed
        ImageRepository.clear();
    }
}