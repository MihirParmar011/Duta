package com.pm.appdev.duta.signup;

import android.Manifest;
import android.app.Activity;
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

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.FirebaseApp;
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
import com.google.firebase.storage.UploadTask;
import com.pm.appdev.duta.Common.NodeNames;
import com.pm.appdev.duta.Login.LoginActivity;
import com.pm.appdev.duta.R;

import java.util.HashMap;

public class Signup extends AppCompatActivity {
    private TextInputEditText etEmail, etName, etUserID, etPassword, etConfirmPassword;
    private String email, name, confirmPasword;
    private String password;
    private String userId;

    private ImageView ivProfile;
    private FirebaseUser firebaseUser;
    private DatabaseReference databaseReference;

    private StorageReference fileStorage;
    private Uri localFileUri, serverFileUri;
    private View progressBar;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup);
        FirebaseApp.initializeApp(this);

        try {
            FirebaseApp.initializeApp(this); // Initialize Firebase
            Log.d("FirebaseInit", "Firebase initialized successfully");
            fileStorage = FirebaseStorage.getInstance().getReference();
        } catch (Exception e) {
            Log.e("FirebaseInit", "Firebase initialization failed: " + e.getMessage());
            Toast.makeText(this, "Firebase initialization failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return; // Exit if Firebase initialization fails
        }

        etEmail = findViewById(R.id.etEmail);
        etName = findViewById(R.id.etName);
        etUserID = findViewById(R.id.etUserID);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        ivProfile = findViewById(R.id.ivProfile);
        progressBar = findViewById(R.id.progressBar);

        // Initialize the Activity Result Launcher
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri selectedImageUri = result.getData().getData();
                        ivProfile.setImageURI(selectedImageUri);
                        localFileUri = selectedImageUri;
                    }
                }
        );

        // Initialize the permission request launcher
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        pickImage();
                    } else {
                        Toast.makeText(this, R.string.permission_required, Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }
//
private void pickImage() {
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    } else {
        requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
    }
}

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101) {
            if (resultCode == RESULT_OK) {
                localFileUri = data.getData();
                ivProfile.setImageURI(localFileUri);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 102) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
//                startActivityForResult(intent, 101);
            } else {
                Toast.makeText(this, R.string.permission_required, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateNameAndPhoto() {
        String strFileName = firebaseUser.getUid() + ".jpg";
        final StorageReference fileRef = fileStorage.child("images/" + strFileName);

        progressBar.setVisibility(View.VISIBLE);
        fileRef.putFile(localFileUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                progressBar.setVisibility(View.GONE);
                if (task.isSuccessful()) {
                    fileRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            serverFileUri = uri;

                            UserProfileChangeRequest request = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(etName.getText().toString().trim())
                                    .setPhotoUri(serverFileUri)
                                    .build();

                            firebaseUser.updateProfile(request).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        databaseReference = FirebaseDatabase.getInstance().getReference().child(NodeNames.USERS);

                                        HashMap<String, String> hashMap = new HashMap<>();
                                        hashMap.put(NodeNames.USER_ID, userId); // Store user ID
                                        hashMap.put(NodeNames.NAME, etName.getText().toString().trim());
                                        hashMap.put(NodeNames.EMAIL, etEmail.getText().toString().trim());
                                        hashMap.put(NodeNames.ONLINE, "true");
                                        hashMap.put(NodeNames.PHOTO, serverFileUri.getPath());

                                        databaseReference.child(userId).setValue(hashMap) // Use userId as the key
                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        Toast.makeText(Signup.this, R.string.user_created_successfully, Toast.LENGTH_SHORT).show();
                                                        startActivity(new Intent(Signup.this, LoginActivity.class));
                                                    }
                                                });
                                    } else {
                                        Toast.makeText(Signup.this,
                                                getString(R.string.failed_to_update_profile, task.getException()), Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                        }
                    });
                }
            }
        });
    }

    public void btnSignupClick(View v) {
        email = etEmail.getText().toString().trim();
        String name = etName.getText().toString().trim();
        password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        userId = etUserID.getText().toString().trim(); // Get user ID input

        // Validation
        if (email.isEmpty()) {
            etEmail.setError(getString(R.string.enter_email));
        } else if (name.isEmpty()) {
            etName.setError(getString(R.string.enter_name));
        } else if (userId.isEmpty()) {
            etUserID.setError(getString(R.string.enter_user_id));
        } else if (etPassword.equals("")) {
            etPassword.setError(getString(R.string.enter_password));
        } else if (confirmPassword.isEmpty()) {
            etConfirmPassword.setError(getString(R.string.confirm_password));
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError(getString(R.string.enter_correct_email));
        } else if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError(getString(R.string.password_mismatch));
        } else {
            // Check if user ID already exists
            progressBar.setVisibility(View.VISIBLE);
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child(NodeNames.USERS).child(userId);

            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    progressBar.setVisibility(View.GONE);
                    if (snapshot.exists()) {
                        // User ID already exists
                        etUserID.setError(getString(R.string.user_id_already_exists));
                        Toast.makeText(Signup.this, R.string.user_id_already_exists, Toast.LENGTH_SHORT).show();
                    } else {
                        // User ID is unique, proceed with signup
                        createUserWithEmailAndPassword();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(Signup.this, R.string.database_error, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
    private void createUserWithEmailAndPassword() {
        progressBar.setVisibility(View.VISIBLE);
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();


        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        firebaseUser = firebaseAuth.getCurrentUser();

                        if (localFileUri != null) {
                            updateNameAndPhoto();
                        } else {
                            updateNameAndPhoto();
                        }
                    } else {
                        Toast.makeText(Signup.this,
                                getString(R.string.signup_failed, task.getException()), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}