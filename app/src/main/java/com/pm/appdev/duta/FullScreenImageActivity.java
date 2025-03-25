package com.pm.appdev.duta;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class FullScreenImageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_screen_image);

        ImageView ivFullScreen = findViewById(R.id.ivFullScreen);
        ProgressBar pbLoading = findViewById(R.id.pbLoading);

        // Set initial placeholder
        ivFullScreen.setImageResource(R.drawable.ic_image_placeholder);

        String imageBase64 = getIntent().getStringExtra("image");
        if (imageBase64 == null || imageBase64.isEmpty()) {
            ivFullScreen.setImageResource(R.drawable.ic_broken_image);
            pbLoading.setVisibility(View.GONE);
            return;
        }

        new Thread(() -> {
            try {
                byte[] decodedString = Base64.decode(imageBase64, Base64.DEFAULT);
                Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                runOnUiThread(() -> {
                    ivFullScreen.setImageBitmap(decodedBitmap);
                    pbLoading.setVisibility(View.GONE);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    pbLoading.setVisibility(View.GONE);
                    ivFullScreen.setImageResource(R.drawable.ic_broken_image);
                    Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
}