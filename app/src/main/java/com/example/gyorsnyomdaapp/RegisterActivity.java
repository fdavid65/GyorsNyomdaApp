package com.example.gyorsnyomdaapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class RegisterActivity extends AppCompatActivity {

    private EditText emailEditText, passwordEditText, usernameEditText;
    private Button registerButton;
    private TextView errorTextView;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();

        emailEditText = findViewById(R.id.editTextEmail);
        passwordEditText = findViewById(R.id.editTextPassword);
        usernameEditText = findViewById(R.id.editTextUsername);
        registerButton = findViewById(R.id.buttonRegister);
        errorTextView = findViewById(R.id.errorTextView);
        errorTextView.setVisibility(View.GONE); // alapból rejtve

        registerButton.setOnClickListener(v -> {
            errorTextView.setVisibility(View.GONE); // reset hibaüzenet

            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();
            String username = usernameEditText.getText().toString().trim();

            // --- Validáció ---
            if (username.isEmpty()) {
                showError("Add meg a felhasználónevet!");
                return;
            }

            if (email.isEmpty()) {
                showError("Add meg az email címet!");
                return;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                showError("Érvénytelen email formátum.");
                return;
            }

            if (password.isEmpty()) {
                showError("Add meg a jelszót!");
                return;
            }

            if (password.length() < 6) {
                showError("A jelszónak legalább 6 karakter hosszúnak kell lennie.");
                return;
            }

            // --- Firebase regisztráció ---
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();

                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(username)
                                    .build();

                            if (user != null) {
                                user.updateProfile(profileUpdates)
                                        .addOnCompleteListener(updateTask -> {
                                            if (updateTask.isSuccessful()) {
                                                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                                                startActivity(intent);
                                                finish();
                                            } else {
                                                showError("Hiba a felhasználónév beállításakor.");
                                            }
                                        });
                            }
                        } else {
                            Exception e = task.getException();
                            if (e instanceof FirebaseAuthUserCollisionException) {
                                showError("Ez az email cím már regisztrálva van.");
                            } else {
                                showError("Hiba: " + e.getMessage());
                            }
                        }
                    });
        });

        findViewById(R.id.textViewLoginRedirect).setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void showError(String message) {
        errorTextView.setText(message);
        errorTextView.setVisibility(View.VISIBLE);
    }
}
