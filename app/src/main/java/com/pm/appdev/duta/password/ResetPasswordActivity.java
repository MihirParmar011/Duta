package com.pm.appdev.duta.password;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.pm.appdev.duta.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class ResetPasswordActivity extends AppCompatActivity {

    private TextInputEditText etEmail;
    private TextView tvMessage;
    private LinearLayout llResetPassword, llMessage;
    private Button btnRetry;
    private View progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        // Initialize views
        etEmail = findViewById(R.id.etEmail);
        tvMessage = findViewById(R.id.tvMessage);
        llMessage = findViewById(R.id.llMessage);
        llResetPassword = findViewById(R.id.llResetPassword);
        btnRetry = findViewById(R.id.btnRetry);
        progressBar = findViewById(R.id.progressBar);
    }

    public void btnResetPasswordClick(View view) {
        String email = etEmail.getText().toString().trim();

        if (email.isEmpty()) {
            etEmail.setError(getString(R.string.enter_email));
            return;
        }

        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        progressBar.setVisibility(View.VISIBLE);

        firebaseAuth.sendPasswordResetEmail(email).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                progressBar.setVisibility(View.GONE);
                llResetPassword.setVisibility(View.GONE);
                llMessage.setVisibility(View.VISIBLE);

                if (task.isSuccessful()) {
                    tvMessage.setText(getString(R.string.reset_password_instructions, email));

                    // Start a countdown timer for 60 seconds
                    new CountDownTimer(60000, 1000) {
                        @Override
                        public void onTick(long millisUntilFinished) {
                            btnRetry.setText(getString(R.string.resend_timer, String.valueOf(millisUntilFinished / 1000)));
                            btnRetry.setOnClickListener(null); // Disable button during countdown
                        }

                        @Override
                        public void onFinish() {
                            btnRetry.setText(R.string.retry);
                            btnRetry.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    llResetPassword.setVisibility(View.VISIBLE);
                                    llMessage.setVisibility(View.GONE);
                                }
                            });
                        }
                    }.start();
                } else {
                    // Handle failure
                    String errorMessage = task.getException() != null ? task.getException().getMessage() : getString(R.string.email_sent_failed);
                    tvMessage.setText(getString(R.string.email_sent_failed, errorMessage));
                    btnRetry.setText(R.string.retry);

                    btnRetry.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            llResetPassword.setVisibility(View.VISIBLE);
                            llMessage.setVisibility(View.GONE);
                        }
                    });
                }
            }
        });
    }

    public void btnCloseClick(View view) {
        finish(); // Close the activity
    }
}