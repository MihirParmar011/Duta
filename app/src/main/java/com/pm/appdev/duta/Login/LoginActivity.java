package com.pm.appdev.duta.Login;

import com.pm.appdev.duta.Common.NodeNames;
import com.pm.appdev.duta.Common.Util;
import com.pm.appdev.duta.MainActivity;
import com.pm.appdev.duta.MessageActivity;
import com.pm.appdev.duta.R;
import com.pm.appdev.duta.password.ResetPasswordActivity;
import com.pm.appdev.duta.signup.Signup;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.Objects;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmailOrUserId, etPassword;
    private String emailOrUserId, password;
    private View progressBar;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmailOrUserId = findViewById(R.id.etEmailOrUserId);
        etPassword = findViewById(R.id.etPassword);
        progressBar = findViewById(R.id.progressBar);

        // Apply window insets to avoid overlap with status and navigation bars
        View rootView = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            int navigationBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            v.setPadding(0, statusBarHeight, 0, navigationBarHeight);
            return insets;
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();

        if (firebaseUser != null) {
            FirebaseMessaging.getInstance().getToken()
                    .addOnCompleteListener(task -> {
                        if (!task.isSuccessful()) {
                            // Could not get FirebaseMessagingToken
                            return;
                        }
                        if (null != task.getResult()) {
                            // Got FirebaseMessagingToken
                            String firebaseMessagingToken = Objects.requireNonNull(task.getResult());
                            // Use firebaseMessagingToken further
                            Util.updateDeviceToken(LoginActivity.this, firebaseMessagingToken);
                        }
                    });

            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        }
    }

    public void tvSignupClick(View v) {
        startActivity(new Intent(this, Signup.class));
    }

    public void btnLoginClick(View v) {
        emailOrUserId = Objects.requireNonNull(etEmailOrUserId.getText()).toString().trim();
        password = Objects.requireNonNull(etPassword.getText()).toString().trim();

        if (emailOrUserId.isEmpty()) {
            etEmailOrUserId.setError(getString(R.string.enter_email_or_user_id));
        } else if (password.isEmpty()) {
            etPassword.setError(getString(R.string.enter_password));
        } else {
            if (Util.connectionAvailable(this)) {
                progressBar.setVisibility(View.VISIBLE);
                if (Patterns.EMAIL_ADDRESS.matcher(emailOrUserId).matches()) {
                    // Login with email
                    loginWithEmail(emailOrUserId, password);
                } else {
                    // Login with user ID
                    loginWithUserId(emailOrUserId, password);
                }
            } else {
                startActivity(new Intent(LoginActivity.this, MessageActivity.class));
            }
        }
    }

    private void loginWithEmail(String email, String password) {
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            progressBar.setVisibility(View.GONE);
            if (task.isSuccessful()) {
                onStart();
            } else {
                Toast.makeText(LoginActivity.this, "Login Failed: " + task.getException(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loginWithUserId(String userId, String password) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference()
                .child(NodeNames.USERS)
                .child(userId);

        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String email = snapshot.child(NodeNames.EMAIL).getValue(String.class);
                    if (email != null) {
                        loginWithEmail(email, password);
                    } else {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(LoginActivity.this, "Email not found for this User ID", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(LoginActivity.this, "User ID not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(LoginActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void tvResetPasswordClick(View view) {
        startActivity(new Intent(LoginActivity.this, ResetPasswordActivity.class));
    }

    // Double back pressed in this LoginActivity will close the application
    private boolean isBackPressedOnce = false;

    @SuppressWarnings(value = "deprecation")
    @Override
    public void onBackPressed() {
        if (isBackPressedOnce) {
            super.onBackPressed();
            return;
        }

        Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show();
        isBackPressedOnce = true;

        new Handler().postDelayed(() -> isBackPressedOnce = false, 2000);
    }
}