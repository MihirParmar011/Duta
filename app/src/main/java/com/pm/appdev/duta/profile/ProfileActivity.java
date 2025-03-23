package com.pm.appdev.duta.profile;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.pm.appdev.duta.R;
import com.pm.appdev.duta.Common.NodeNames;
import com.pm.appdev.duta.Login.LoginActivity;
import com.pm.appdev.duta.password.ChangePasswordActivity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Objects;

public class ProfileActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etName;
    private ImageView ivProfile;
    private FirebaseUser firebaseUser;
    private DatabaseReference databaseReference;
    private StorageReference fileStorage;
    private Uri localFileUri, serverFileUri;
    private FirebaseAuth firebaseAuth;
    private View progressBar;
    private String userID; // Store the current user's ID (e.g., Mihir1101, Kano1101)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        etEmail = findViewById(R.id.etEmail);
        etName = findViewById(R.id.etName);
        ivProfile = findViewById(R.id.ivProfile);
        progressBar = findViewById(R.id.progressBar);

        fileStorage = FirebaseStorage.getInstance().getReference();
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        databaseReference = FirebaseDatabase.getInstance().getReference().child(NodeNames.USERS);

        if (firebaseUser != null) {
            // Get the current user's UID from Firebase Authentication
            String uid = firebaseUser.getUid();

            // Find the UserID in the Users node using the UID
            findUserIDByUID(uid);
        }
    }

    // Find UserID in the Users node using UID
    private void findUserIDByUID(String uid) {
        progressBar.setVisibility(View.VISIBLE);

        databaseReference.orderByChild("uid").equalTo(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                progressBar.setVisibility(View.GONE);
                if (snapshot.exists()) {
                    for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                        // Get the UserID (e.g., Mihir1101, Kano1101)
                        userID = userSnapshot.getKey();

                        // Fetch user data using the UserID
                        fetchUserData(userID);
                        break; // Exit loop after finding the first match
                    }
                } else {
                    Toast.makeText(ProfileActivity.this, "User data not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ProfileActivity.this, "Failed to fetch user data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Fetch user data from Firebase Realtime Database
    private void fetchUserData(String userID) {
        progressBar.setVisibility(View.VISIBLE);

        databaseReference.child(userID).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                progressBar.setVisibility(View.GONE);
                if (snapshot.exists()) {
                    // Fetch name
                    String name = snapshot.child(NodeNames.NAME).getValue(String.class);
                    if (name != null) {
                        etName.setText(name);
                    }

                    // Fetch email
                    String email = snapshot.child(NodeNames.EMAIL).getValue(String.class);
                    if (email != null) {
                        etEmail.setText(email);
                    }

                    // Fetch photo (Base64 or URL)
                    String photo = snapshot.child(NodeNames.PHOTO).getValue(String.class);
                    if (photo != null && !photo.isEmpty()) {
                        if (photo.startsWith("http")) {
                            // Load image from URL
                            Glide.with(ProfileActivity.this)
                                    .load(photo)
                                    .placeholder(R.drawable.default_profile)
                                    .error(R.drawable.default_profile)
                                    .into(ivProfile);
                        } else {
                            // Decode Base64 image
                            Bitmap bitmap = decodeBase64ToBitmap(photo);
                            ivProfile.setImageBitmap(bitmap);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ProfileActivity.this, "Failed to fetch user data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Method to decode Base64 string to Bitmap
    private Bitmap decodeBase64ToBitmap(String base64Image) {
        byte[] decodedBytes = Base64.decode(base64Image, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
    }

    public void btnSaveClick(View view) {
        if (Objects.requireNonNull(etName.getText()).toString().trim().isEmpty()) {
            etName.setError(getString(R.string.enter_name));
        } else {
            if (localFileUri != null) {
                updateNameAndPhoto(userID);
            } else {
                updateOnlyName(userID);
            }
        }
    }

    // Update user data with photo
    private void updateNameAndPhoto(String userID) {
        progressBar.setVisibility(View.VISIBLE);

        // Convert the selected image to Base64
        String base64Image = convertImageToBase64(localFileUri);
        if (base64Image == null) {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show();
            return;
        }

        // Update Firebase Realtime Database with Base64 image
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put(NodeNames.NAME, Objects.requireNonNull(etName.getText()).toString().trim());
        hashMap.put(NodeNames.PHOTO, base64Image); // Store Base64 string

        databaseReference.child(userID).updateChildren(hashMap)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        Toast.makeText(ProfileActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(ProfileActivity.this, "Failed to update profile: " + task.getException(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Update user data without photo
    private void updateOnlyName(String userID) {
        progressBar.setVisibility(View.VISIBLE);
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put(NodeNames.NAME, Objects.requireNonNull(etName.getText()).toString().trim());

        databaseReference.child(userID).updateChildren(hashMap)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        Toast.makeText(ProfileActivity.this, "Name updated successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(ProfileActivity.this, "Failed to update name: " + task.getException(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Method to convert image URI to Base64 string
    private String convertImageToBase64(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream); // Compress image
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            return Base64.encodeToString(byteArray, Base64.DEFAULT); // Convert to Base64
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Rest of your existing methods (unchanged)
    public void btnLogoutClick(View view) {
        final FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        DatabaseReference databaseReference = rootRef.child(NodeNames.TOKENS).child(Objects.requireNonNull(currentUser).getUid());

        // Clear the device_token while signing out using setValue()
        databaseReference.setValue(null).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                firebaseAuth.signOut();
                startActivity(new Intent(ProfileActivity.this, LoginActivity.class));
                finish();
            } else {
                Toast.makeText(ProfileActivity.this, getString(R.string.something_went_wrong, task.getException()), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Handle image change or removal
    public void changeImage(View view) {
        if (serverFileUri == null) {
            // No photo exists, allow the user to pick a new photo
            pickImage();
        } else {
            // Photo exists, show a popup menu to change or remove the photo
            PopupMenu popupMenu = new PopupMenu(this, view);
            popupMenu.getMenuInflater().inflate(R.menu.menu_picture, popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(menuItem -> {
                int id = menuItem.getItemId();

                if (id == R.id.mnuChangePic) {
                    // Change photo
                    pickImage();
                } else if (id == R.id.mnuRemovePic) {
                    // Remove photo
                    removePhoto();
                }
                return false;
            });
            popupMenu.show();
        }
    }

    // Method to pick an image from the gallery
    private void pickImage() {
        // Check if the permission is granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            // Permission is granted, launch the image picker
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, 101);
        } else {
            // Permission is not granted, request it
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 102);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101) {
            if (resultCode == RESULT_OK) {
                localFileUri = Objects.requireNonNull(data).getData();
                ivProfile.setImageURI(localFileUri);
            }
        }
    }

    // Handle the result of the permission request
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 102) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, launch the image picker
                pickImage();
            } else {
                // Permission denied, show a Toast message
                Toast.makeText(this, "Access permission is required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Remove the photo
    private void removePhoto() {
        progressBar.setVisibility(View.VISIBLE);

        // Update Firebase Realtime Database to remove the photo
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put(NodeNames.PHOTO, ""); // Set photo to empty string

        databaseReference.child(userID).updateChildren(hashMap)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        // Set default profile image
                        ivProfile.setImageResource(R.drawable.default_profile);
                        Toast.makeText(ProfileActivity.this, "Photo removed successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(ProfileActivity.this, "Failed to remove photo: " + task.getException(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public void btnChangePasswordClick(View view) {
        startActivity(new Intent(ProfileActivity.this, ChangePasswordActivity.class));
    }
}