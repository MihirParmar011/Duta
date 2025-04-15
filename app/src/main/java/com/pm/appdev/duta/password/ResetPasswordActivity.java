package com.pm.appdev.duta.password;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.view.WindowManager;
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

        View decorView = getWindow().getDecorView();

        // ✅ Allow layout in display cutout (notch area)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(lp);
        }

        // ✅ Extend layout fullscreen and under notch/nav bar
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

        // ✅ Make status and nav bar transparent
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);

        // ✅ Optional: hide system bars, reveal on swipe
        WindowInsetsControllerCompat controller = ViewCompat.getWindowInsetsController(decorView);
        if (controller != null) {
            controller.hide(WindowInsetsCompat.Type.systemBars());
            controller.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            );
        }

        // ✅ Handle padding if needed
        ViewCompat.setOnApplyWindowInsetsListener(decorView, (view, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            // Optional padding logic (only if content goes under system bars)
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

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