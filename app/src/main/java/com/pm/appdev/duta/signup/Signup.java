package com.pm.appdev.duta.signup;

import android.Manifest;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.FirebaseApp;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.pm.appdev.duta.Login.LoginActivity;
import com.pm.appdev.duta.R;

import java.util.HashMap;
import java.util.Objects;


public class Signup extends AppCompatActivity {

    private TextInputEditText etEmail, etName, etUserID, etPassword, etConfirmPassword;
    private ImageView ivProfile;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser firebaseUser;
    private DatabaseReference userDatabase, tokenDatabase;
    private StorageReference fileStorage;
    private Uri localFileUri;
    private View progressBar;
    private String deviceToken;

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    localFileUri = result.getData().getData();
                    ivProfile.setImageURI(localFileUri);
                }
            });

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    pickImage();
                } else {
                    Toast.makeText(this, "Permission required to access media.", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        FirebaseApp.initializeApp(this);

        firebaseAuth = FirebaseAuth.getInstance();
        fileStorage = FirebaseStorage.getInstance().getReference();
        userDatabase = FirebaseDatabase.getInstance().getReference("Users");
        tokenDatabase = FirebaseDatabase.getInstance().getReference("Tokens");

        etEmail = findViewById(R.id.etEmail);
        etName = findViewById(R.id.etName);
        etUserID = findViewById(R.id.etUserID);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        ivProfile = findViewById(R.id.ivProfile);
        progressBar = findViewById(R.id.progressBar);

        // Get FCM Device Token
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token -> deviceToken = token);
    }

    public void pickImage(View v) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            pickImage();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    public void btnSignupClick(View v) {
        String email = Objects.requireNonNull(etEmail.getText()).toString().trim();
        String name = Objects.requireNonNull(etName.getText()).toString().trim();
        String userId = Objects.requireNonNull(etUserID.getText()).toString().trim();
        String password = Objects.requireNonNull(etPassword.getText()).toString().trim();
        String confirmPassword = Objects.requireNonNull(etConfirmPassword.getText()).toString().trim();

        if (validateInputs(email, name, userId, password, confirmPassword)) {
            createUserWithEmailAndPassword(email, name, userId, password);
        }
    }

    private boolean validateInputs(String email, String name, String userId, String password, String confirmPassword) {
        if (email.isEmpty()) {
            etEmail.setError("Enter email");
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid email");
            return false;
        } else if (name.isEmpty()) {
            etName.setError("Enter name");
            return false;
        } else if (userId.isEmpty()) {
            etUserID.setError("Enter user ID");
            return false;
        } else if (password.isEmpty()) {
            etPassword.setError("Enter password");
            return false;
        } else if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            return false;
        }
        return true;
    }

    private void createUserWithEmailAndPassword(String email, String name, String userId, String password) {
        progressBar.setVisibility(View.VISIBLE);

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        firebaseUser = firebaseAuth.getCurrentUser();
                        if (localFileUri != null) {
                            uploadProfilePicture(name, email, userId);
                        } else {
                            saveUserData(name, email, userId, null);
                        }
                    } else {
                        Toast.makeText(Signup.this, "Signup failed: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void uploadProfilePicture(String name, String email, String userId) {
        if (localFileUri == null) {
            saveUserData(name, email, userId, null);
            return;
        }

        String fileName = userId + ".jpg";
        StorageReference fileRef = fileStorage.child("profile_images/" + fileName);

        progressBar.setVisibility(View.VISIBLE);
        fileRef.putFile(localFileUri)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        fileRef.getDownloadUrl().addOnSuccessListener(uri -> saveUserData(name, email, userId, uri.toString()));
                    } else {
                        Log.e("UploadError", "Profile picture upload failed", task.getException());
                        saveUserData(name, email, userId, null);
                    }
                });
    }

    private void saveUserData(String name, String email, String userId, String photoUrl) {
        HashMap<String, Object> userMap = new HashMap<>();
        userMap.put("userId", userId);
        userMap.put("name", name);
        userMap.put("email", email);
        userMap.put("online", "true");
        userMap.put("photo", (photoUrl != null) ? photoUrl : "");

        userDatabase.child(userId).setValue(userMap)
                .addOnCompleteListener(dbTask -> {
                    if (dbTask.isSuccessful()) {
                        saveUserToken(userId);
                    } else {
                        Toast.makeText(Signup.this, "Error saving user data.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveUserToken(String userId) {
        HashMap<String, String> tokenMap = new HashMap<>();
        tokenMap.put("token", deviceToken);

        tokenDatabase.child(userId).setValue(tokenMap)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        Toast.makeText(Signup.this, "User created successfully!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(Signup.this, LoginActivity.class));
                        finish();
                    } else {
                        Toast.makeText(Signup.this, "Token saving failed!", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}