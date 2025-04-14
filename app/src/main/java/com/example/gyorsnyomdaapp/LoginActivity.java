package com.example.gyorsnyomdaapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private EditText emailEditText, passwordEditText;
    private Button loginButton;
    private TextView errorTextView;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        errorTextView = findViewById(R.id.errorTextView);
        errorTextView.setVisibility(View.GONE);

        mAuth = FirebaseAuth.getInstance();

        loginButton.setOnClickListener(v -> {
            Animation scaleAnimation = AnimationUtils.loadAnimation(LoginActivity.this, R.anim.button_scale);
            loginButton.startAnimation(scaleAnimation);

            errorTextView.setVisibility(View.GONE);

            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (email.isEmpty()) {
                showError("Az email cím megadása kötelező!");
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                showError("Érvénytelen email cím formátum!");
            } else if (password.isEmpty()) {
                showError("A jelszó megadása kötelező!");
            } else {
                loginUser(email, password);
            }
        });

        findViewById(R.id.textViewRegisterRedirect).setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            finish();
        });
    }

    private void loginUser(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        showError("Hibás email cím vagy jelszó!");
                    }
                });
    }

    private void showError(String message) {
        errorTextView.setText(message);
        errorTextView.setVisibility(View.VISIBLE);
    }
}
