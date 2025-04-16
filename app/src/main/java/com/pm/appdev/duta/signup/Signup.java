package com.pm.appdev.duta.signup;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;
import com.pm.appdev.duta.MainActivity;
import com.pm.appdev.duta.R;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Objects;

public class Signup extends AppCompatActivity {

    private TextInputEditText etEmail, etName, etUserID, etPassword, etConfirmPassword;
    private ImageView ivProfile;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser firebaseUser;
    private DatabaseReference userDatabase;
    private Uri localFileUri;
    private View progressBar;

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
                    Log.d("PermissionCheck", "Permission granted! Opening image picker...");
                    pickImage();
                } else {
                    Log.e("PermissionCheck", "Permission denied!");
                    Toast.makeText(this, "Permission required to access media.", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        FirebaseApp.initializeApp(this);

        View decorView = getWindow().getDecorView();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(lp);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
        } else {
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        }

        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);

        WindowInsetsControllerCompat controller = ViewCompat.getWindowInsetsController(decorView);
        if (controller != null) {
            controller.hide(WindowInsetsCompat.Type.systemBars());
            controller.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            );
        }

        ViewCompat.setOnApplyWindowInsetsListener(decorView, (view, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        firebaseAuth = FirebaseAuth.getInstance();
        userDatabase = FirebaseDatabase.getInstance().getReference("Users");

        etEmail = findViewById(R.id.etEmail);
        etName = findViewById(R.id.etName);
        etUserID = findViewById(R.id.etUserID);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        ivProfile = findViewById(R.id.ivProfile);
        progressBar = findViewById(R.id.progressBar);

        ivProfile.setOnClickListener(this::pickImage);
    }

    public void pickImage(View v) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) {
                pickImage();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                pickImage();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
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
            String base64Image = null;
            if (localFileUri != null) {
                base64Image = convertImageToBase64(localFileUri);
            }
            createUserWithEmailAndPassword(email, name, userId, password, base64Image);
        }
    }

    private boolean validateInputs(String email, String name, String userId, String password, String confirmPassword) {
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid email");
            return false;
        } else if (name.isEmpty()) {
            etName.setError("Enter name");
            return false;
        } else if (userId.isEmpty()) {
            etUserID.setError("Enter user ID");
            return false;
        } else if (password.isEmpty() || !password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            return false;
        }
        return true;
    }

    private void createUserWithEmailAndPassword(String email, String name, String userId, String password, String base64Image) {
        progressBar.setVisibility(View.VISIBLE);

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        firebaseUser = firebaseAuth.getCurrentUser();
                        saveUserData(name, email, userId, base64Image);
                    } else {
                        Toast.makeText(Signup.this, "Signup failed: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private String convertImageToBase64(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            return android.util.Base64.encodeToString(byteArray, android.util.Base64.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void saveUserData(String name, String email, String userId, String base64Image) {
        String uid = firebaseUser.getUid();

        // First attempt to get FCM token
        fetchFcmToken(uid, email, name, userId, base64Image, 0);
    }

    private void fetchFcmToken(String uid, String email, String name,
                               String userId, String base64Image, int retryCount) {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.e("FCM_TOKEN", "Fetch token failed", task.getException());

                        // Retry up to 3 times with 1-second delay
                        if (retryCount < 3) {
                            new android.os.Handler().postDelayed(() -> {
                                fetchFcmToken(uid, email, name, userId, base64Image, retryCount + 1);
                            }, 1000);
                            return;
                        }

                        // Final fallback - save with empty token
                        Log.w("FCM_TOKEN", "Using empty token after retries");
                        saveUserToDatabase(uid, email, name, userId, base64Image, "");
                        return;
                    }

                    String token = task.getResult();
                    Log.d("FCM_TOKEN", "Token retrieved: " + token);
                    saveUserToDatabase(uid, email, name, userId, base64Image, token);
                });
    }

    private void saveUserToDatabase(String uid, String email, String name,
                                    String userId, String base64Image, String token) {
        HashMap<String, Object> userMap = new HashMap<>();
        userMap.put("uid", uid);
        userMap.put("email", email);
        userMap.put("name", name);
        userMap.put("photo", (base64Image != null) ? base64Image : "");
        userMap.put("token", token);
        userMap.put("timestamp", System.currentTimeMillis()); // Added timestamp

        userDatabase.child(userId).setValue(userMap)
                .addOnCompleteListener(dbTask -> {
                    if (dbTask.isSuccessful()) {
                        Log.d("DATABASE", "User data saved successfully");
                        autoLoginAndRedirect();
                    } else {
                        Log.e("DATABASE", "Error saving user data", dbTask.getException());
                        Toast.makeText(Signup.this,
                                "Error saving user data: " + dbTask.getException(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void autoLoginAndRedirect() {
        if (firebaseUser != null) {
            startActivity(new Intent(Signup.this, MainActivity.class));
            finish();
        }
    }
}

//    package com.pm.appdev.duta.signup;
//
//    import android.Manifest;
//    import android.content.Intent;
//    import android.content.pm.PackageManager;
//    import android.graphics.Bitmap;
//    import android.graphics.BitmapFactory;
//    import android.net.Uri;
//    import android.os.Build;
//    import android.os.Bundle;
//    import android.provider.MediaStore;
//    import android.util.Log;
//    import android.util.Patterns;
//    import android.view.View;
//    import android.view.WindowManager;
//    import android.widget.ImageView;
//    import android.widget.Toast;
//
//    import androidx.activity.result.ActivityResultLauncher;
//    import androidx.activity.result.contract.ActivityResultContracts;
//    import androidx.appcompat.app.AppCompatActivity;
//    import androidx.core.content.ContextCompat;
//    import androidx.core.graphics.Insets;
//    import androidx.core.view.ViewCompat;
//    import androidx.core.view.WindowInsetsCompat;
//    import androidx.core.view.WindowInsetsControllerCompat;
//
//    import com.google.android.material.textfield.TextInputEditText;
//    import com.google.firebase.FirebaseApp;
//    import com.google.firebase.auth.FirebaseAuth;
//    import com.google.firebase.auth.FirebaseUser;
//    import com.google.firebase.database.DatabaseReference;
//    import com.google.firebase.database.FirebaseDatabase;
//    import com.google.firebase.messaging.FirebaseMessaging;
//    import com.pm.appdev.duta.MainActivity;
//    import com.pm.appdev.duta.R;
//
//    import java.io.ByteArrayOutputStream;
//    import java.io.IOException;
//    import java.io.InputStream;
//    import java.util.HashMap;
//    import java.util.Objects;
//
//    public class Signup extends AppCompatActivity {
//
//        private TextInputEditText etEmail, etName, etUserID, etPassword, etConfirmPassword;
//        private ImageView ivProfile;
//        private FirebaseAuth firebaseAuth;
//        private FirebaseUser firebaseUser;
//        private DatabaseReference userDatabase;
//        private Uri localFileUri;
//        private View progressBar;
//
//        private final ActivityResultLauncher<Intent> imagePickerLauncher =
//                registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
//                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
//                        localFileUri = result.getData().getData();
//                        ivProfile.setImageURI(localFileUri);
//                    }
//                });
//
//        private final ActivityResultLauncher<String> requestPermissionLauncher =
//                registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
//                    if (isGranted) {
//                        Log.d("PermissionCheck", "Permission granted! Opening image picker...");
//                        pickImage();
//                    } else {
//                        Log.e("PermissionCheck", "Permission denied!");
//                        Toast.makeText(this, "Permission required to access media.", Toast.LENGTH_SHORT).show();
//                    }
//                });
//
//        @Override
//        protected void onCreate(Bundle savedInstanceState) {
//            super.onCreate(savedInstanceState);
//            setContentView(R.layout.activity_signup);
//            FirebaseApp.initializeApp(this);
//
//            View decorView = getWindow().getDecorView();
//
//            // ✅ Allow layout in display cutout (notch area)
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
//                WindowManager.LayoutParams lp = getWindow().getAttributes();
//                lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
//                getWindow().setAttributes(lp);
//            }
//
//            // ✅ Extend layout fullscreen and under notch/nav bar
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//                getWindow().setDecorFitsSystemWindows(false);
//            } else {
//                decorView.setSystemUiVisibility(
//                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
//                                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
//                                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
//                                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
//                );
//            }
//
//            // ✅ Make status and nav bar transparent
//            getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
//            getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);
//
//            // ✅ Optional: hide system bars, reveal on swipe
//            WindowInsetsControllerCompat controller = ViewCompat.getWindowInsetsController(decorView);
//            if (controller != null) {
//                controller.hide(WindowInsetsCompat.Type.systemBars());
//                controller.setSystemBarsBehavior(
//                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
//                );
//            }
//
//            // ✅ Handle padding if needed
//            ViewCompat.setOnApplyWindowInsetsListener(decorView, (view, windowInsets) -> {
//                Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
//                // Optional padding logic (only if content goes under system bars)
//                view.setPadding(insets.left, insets.top, insets.right, insets.bottom);
//                return WindowInsetsCompat.CONSUMED;
//            });
//
//            firebaseAuth = FirebaseAuth.getInstance();
//            userDatabase = FirebaseDatabase.getInstance().getReference("Users");
//
//            etEmail = findViewById(R.id.etEmail);
//            etName = findViewById(R.id.etName);
//            etUserID = findViewById(R.id.etUserID);
//            etPassword = findViewById(R.id.etPassword);
//            etConfirmPassword = findViewById(R.id.etConfirmPassword);
//            ivProfile = findViewById(R.id.ivProfile);
//            progressBar = findViewById(R.id.progressBar);
//
//            ivProfile.setOnClickListener(this::pickImage);
//        }
//
//        public void pickImage(View v) {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//                // Android 13+ (API 33+)
//                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) {
//                    pickImage();
//                } else {
//                    requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
//                }
//            } else {
//                // Android 12 and below
//                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
//                    pickImage();
//                } else {
//                    requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
//                }
//            }
//        }
//
//        private void pickImage() {
//            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
//            imagePickerLauncher.launch(intent);
//        }
//
//        public void btnSignupClick(View v) {
//            String email = Objects.requireNonNull(etEmail.getText()).toString().trim();
//            String name = Objects.requireNonNull(etName.getText()).toString().trim();
//            String userId = Objects.requireNonNull(etUserID.getText()).toString().trim();
//            String password = Objects.requireNonNull(etPassword.getText()).toString().trim();
//            String confirmPassword = Objects.requireNonNull(etConfirmPassword.getText()).toString().trim();
//
//            if (validateInputs(email, name, userId, password, confirmPassword)) {
//                String base64Image = null;
//                if (localFileUri != null) {
//                    base64Image = convertImageToBase64(localFileUri); // Convert image to Base64
//                }
//                createUserWithEmailAndPassword(email, name, userId, password, base64Image);
//            }
//        }
//
//        private boolean validateInputs(String email, String name, String userId, String password, String confirmPassword) {
//            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
//                etEmail.setError("Enter a valid email");
//                return false;
//            } else if (name.isEmpty()) {
//                etName.setError("Enter name");
//                return false;
//            } else if (userId.isEmpty()) {
//                etUserID.setError("Enter user ID");
//                return false;
//            } else if (password.isEmpty() || !password.equals(confirmPassword)) {
//                etConfirmPassword.setError("Passwords do not match");
//                return false;
//            }
//            return true;
//        }
//
//        private void createUserWithEmailAndPassword(String email, String name, String userId, String password, String base64Image) {
//            progressBar.setVisibility(View.VISIBLE);
//
//            firebaseAuth.createUserWithEmailAndPassword(email, password)
//                    .addOnCompleteListener(task -> {
//                        progressBar.setVisibility(View.GONE);
//                        if (task.isSuccessful()) {
//                            firebaseUser = firebaseAuth.getCurrentUser();
//                            saveUserData(name, email, userId, base64Image); // Save user data with Base64 image
//                        } else {
//                            Toast.makeText(Signup.this, "Signup failed: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_LONG).show();
//                        }
//                    });
//        }
//
//        private String convertImageToBase64(Uri imageUri) {
//            try {
//                InputStream inputStream = getContentResolver().openInputStream(imageUri);
//                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
//                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream); // Compress image
//                byte[] byteArray = byteArrayOutputStream.toByteArray();
//                return android.util.Base64.encodeToString(byteArray, android.util.Base64.DEFAULT); // Convert to Base64
//            } catch (IOException e) {
//                e.printStackTrace();
//                return null;
//            }
//        }
//
//        private void saveUserData(String name, String email, String userId, String base64Image) {
//            String uid = firebaseUser.getUid();
//
//            FirebaseMessaging.getInstance().getToken()
//                    .addOnCompleteListener(task -> {
//                        if (!task.isSuccessful()) {
//                            Log.w("FCM", "Fetching FCM registration token failed", task.getException());
//                            return;
//                        }
//
//                        String token = task.getResult(); // ✅ Get device token
//
//                        HashMap<String, Object> userMap = new HashMap<>();
//                        userMap.put("uid", uid);
//                        userMap.put("email", email);
//                        userMap.put("name", name);
//                        userMap.put("photo", (base64Image != null) ? base64Image : "");
//                        userMap.put("token", token); // ✅ Save token
//
//                        userDatabase.child(userId).setValue(userMap)
//                                .addOnCompleteListener(dbTask -> {
//                                    if (dbTask.isSuccessful()) {
//                                        autoLoginAndRedirect();
//                                    } else {
//                                        Toast.makeText(Signup.this, "Error saving user data.", Toast.LENGTH_SHORT).show();
//                                    }
//                                });
//                    });
//        }
//
//
//        private void autoLoginAndRedirect() {
//            if (firebaseUser != null) {
//                startActivity(new Intent(Signup.this, MainActivity.class));
//                finish();
//            }
//        }
//    }
//